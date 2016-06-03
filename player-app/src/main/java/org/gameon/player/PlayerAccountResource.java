/*******************************************************************************
 * Copyright (c) 2015 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameon.player;

import java.io.IOException;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ektorp.CouchDbConnector;
import org.gameon.player.Kafka.PlayerEvent;
import org.gameon.player.control.PlayerAccountModificationException;
import org.gameon.player.entity.LocationChange;
import org.gameon.player.entity.Player;
import org.gameon.player.entity.PlayerFull;
import org.gameon.player.entity.PlayerLocation;

import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Player service, where players remember where they are, and what they have
 * in their pockets.
 *
 */
@Path("/accounts/{id}")
@Api( tags = {"players"})
public class PlayerAccountResource {
    private static final String ACCESS_DENIED = "ACCESS_DENIED";

    @Context
    HttpServletRequest httpRequest;

    @Inject
    protected CouchDbConnector db;
    
    @Inject
    Kafka kafka;

    @Resource(lookup = "systemId")
    String systemId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a specific player", 
        notes = "", 
        response = Player.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
    })
    public Player getPlayerInformation(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        Player p;
        
        if (unauthorizedId(authId, id)) {
            p = db.get(Player.class, id); // throws DocumentNotFoundException
        } else {
            p = db.get(PlayerFull.class, id); // throws DocumentNotFoundException
        }

        return p;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update a specific player",
            notes = "",
            response = Player.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "update specified player")
    })
    public Response updatePlayer(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id,
            @ApiParam(value = "Updated player attributes", required = true) Player newPlayer) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        //reject updates unless they come from matching player, or system id.
        if (unauthorizedId(authId, id)) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be updated",
                    authId + " is not allowed to update room " + id);
        }

        PlayerFull fullPlayer = db.get(PlayerFull.class, newPlayer.getId());
        
        Claims claims = (Claims) httpRequest.getAttribute("player.claims");        
        if ( !claims.getAudience().equals("server")) {
            // Check the "audience" to determine which fields can be updated
            // If it is not a server, but it is the matching player, 
            // we allow the user profile to be updated.
            fullPlayer.update(newPlayer);
        }
        
        db.update(fullPlayer);
        
        kafka.publishPlayerEvent(PlayerEvent.UPDATE, fullPlayer);
        
        return Response.ok(fullPlayer).build();
    }

    @DELETE
    @ApiOperation(
            value = "Delete a specific player",
            notes = "",
            code = HttpServletResponse.SC_NO_CONTENT )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_NO_CONTENT, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "delete specified player")
    })
    public Response removePlayer(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // players are allowed to delete themselves..
        // only allow delete for matching id, or system id.
        if (unauthorizedId(authId, id)) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be deleted",
                    authId + " is not allowed to delete player " + id);
        }

        Player p = db.get(Player.class, id); // throws DocumentNotFoundException
        db.delete(p);

        kafka.publishPlayerEvent(PlayerEvent.DELETE, p);
        
        return Response.status(HttpServletResponse.SC_NO_CONTENT).build();
    }

    @PUT
    @Path("/location")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update player location",
            notes = "",
            code = HttpServletResponse.SC_OK ,
            response = PlayerLocation.class )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "update player location")
    })
    public Response updatePlayerLocation(@PathParam("id") String id, LocationChange update) throws IOException {

        // we don't want to allow this method to be invoked by a user.
        Claims claims = (Claims) httpRequest.getAttribute("player.claims");       
        if ( !claims.getAudience().equals("server")) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Unable to update player location",
                    "Invalid token type " + claims.getAudience());
        }

        PlayerFull p = db.get(PlayerFull.class, id);  // throws DocumentNotFoundException

        String oldLocation = update.getOldLocation();
        String newLocation = update.getNewLocation();
        PlayerLocation finalLocation = new PlayerLocation();
        
        // try setting to the new location
        int rc;

        if (p.getLocation().equals(oldLocation)) {
            p.setLocation(newLocation);
            db.update(p);

            rc = HttpServletResponse.SC_OK;
            finalLocation.setLocation(newLocation);
        } else {
            rc = HttpServletResponse.SC_CONFLICT;
            finalLocation.setLocation(p.getLocation());
        }
        
        kafka.publishPlayerEvent(PlayerEvent.UPDATE_LOCATION, p);

        return Response.status(rc).entity(finalLocation).build();
    }
    
    @GET
    @Path("/location")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a specific player location", 
        notes = "", 
        response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
    })
    public String getPlayerLocation(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {
        PlayerFull p;
        
        p = db.get(PlayerFull.class, id); // throws DocumentNotFoundException
        
        return p.getLocation();
    }
    
    @GET
    @Path("/apikey")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a specific player apikey", 
        notes = "", 
        response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
    })
    public String getPlayerApiKey(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {
        
        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");
        
        if (unauthorizedId(authId, id)) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "APIKey For Player " + id + " could not be retrieved",
                    authId + " is not allowed to view information");
        }
        
        PlayerFull p = db.get(PlayerFull.class, id);  // throws DocumentNotFoundException
        
        return p.getApiKey();
    }
    

    @PUT
    @Path("/apikey")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update player API key",
            notes = "",
            code = HttpServletResponse.SC_OK ,
            response = Player.class )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "update player api key")
    })
    public Response updatePlayerApiKey(@PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // we don't want to allow this method to be invoked by the server (must be at user's request).
        Claims claims = (Claims) httpRequest.getAttribute("player.claims");
        if ( claims.getAudience().equals("server")) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Unable to update player location",
                    "Invalid token type " + claims.getAudience());
        }

        if (unauthorizedId(authId, id)) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be updated",
                    authId + " is not allowed to update player " + id);
        }
        
        PlayerFull p = db.get(PlayerFull.class, id);  // throws DocumentNotFoundException

        if( p.getApiKey() == null && !p.getApiKey().equals(ACCESS_DENIED)){
            p.generateApiKey();
        }           
        
        kafka.publishPlayerEvent(PlayerEvent.UPDATE_APIKEY, p);

        return Response.ok(p).build();
    }

    private boolean unauthorizedId(String user, String player) {
        return ( user == null || !(player.equals(user) || systemId.equals(user)) );
    }
}
