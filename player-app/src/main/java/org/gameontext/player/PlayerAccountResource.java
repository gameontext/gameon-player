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
package org.gameontext.player;

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
import org.gameontext.player.Kafka.PlayerEvent;
import org.gameontext.player.control.PlayerAccountModificationException;
import org.gameontext.player.entity.ErrorResponse;
import org.gameontext.player.entity.LocationChange;
import org.gameontext.player.entity.PlayerArgument;
import org.gameontext.player.entity.PlayerCredentials;
import org.gameontext.player.entity.PlayerDbRecord;
import org.gameontext.player.entity.PlayerLocation;
import org.gameontext.player.entity.PlayerResponse;
import org.gameontext.player.utils.SharedSecretGenerator;

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
        response = PlayerResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response=PlayerResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
    })
    public PlayerResponse getPlayerInformation(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        PlayerDbRecord p = db.get(PlayerDbRecord.class, id); // throws DocumentNotFoundException

        // because we added collecting emails after release, existing accounts can be
        // missing email info, but if a player gets this far, they have seen & agreed
        // to the t&c's via the login, so we update their record with the email from the
        // jwt.
        // we know the UI does a get against the ID to test if the user exists or not yet,
        // during the initial login, so by adding this logic here, we use that call to
        // update the player record with the email.
        // Only do this for missing emails, where the jwt id matches the requested id.
        // (eg, not for players with emails already set, and not for the system id)
        if(p.getEmail()==null && authId!=null && authId.equals(id)) {
          Claims claims = (Claims) httpRequest.getAttribute("player.claims");
          //only do this bit for client based jwts.
          if ( claims!=null && !claims.getAudience().equals("server")) {
              //if the jwt has an email (older cached ones may not)
              if(claims.get("email")!=null){
                p.setEmail(claims.get("email").toString());
                db.update(p);
                kafka.publishPlayerEvent(PlayerEvent.UPDATE_EMAIL, p);
              }
          }
        }

        PlayerResponse pr = new PlayerResponse(p);

        if (unauthorizedId(authId, id)) {
            pr.setCredentials(null);
        }

        return pr;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update a specific player",
            notes = "",
            response = PlayerResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response=PlayerResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "update specified player", response=ErrorResponse.class)
    })
    public Response updatePlayer(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id,
            @ApiParam(value = "Updated player attributes", required = true) PlayerArgument newPlayer) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        //reject updates unless they come from matching player, or system id.
        if (unauthorizedId(authId, id)) {
            if(authId==null){
                authId="Unauthenticated User";
            }
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be updated",
                    authId + " is not allowed to update player " + id);
        }

        PlayerDbRecord fullPlayer = db.get(PlayerDbRecord.class, newPlayer.getId());

        Claims claims = (Claims) httpRequest.getAttribute("player.claims");
        if ( !claims.getAudience().equals("server")) {
            // Check the "audience" to determine which fields can be updated
            // If it is not a server, but it is the matching player,
            // we allow the user profile to be updated.
            fullPlayer.update(newPlayer);
        }

        db.update(fullPlayer);

        kafka.publishPlayerEvent(PlayerEvent.UPDATE, fullPlayer);

        PlayerResponse pr = new PlayerResponse(fullPlayer);

        return Response.ok(pr).build();
    }

    @DELETE
    @ApiOperation(
            value = "Delete a specific player",
            notes = "",
            code = HttpServletResponse.SC_NO_CONTENT )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_NO_CONTENT, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "delete specified player", response=ErrorResponse.class)
    })
    public Response removePlayer(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // players are allowed to delete themselves..
        // only allow delete for matching id, or system id.
        if (unauthorizedId(authId, id)) {
            if(authId==null){
                authId="Unauthenticated User";
            }
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be deleted",
                    authId + " is not allowed to delete player " + id);
        }

        PlayerDbRecord p = db.get(PlayerDbRecord.class, id); // throws DocumentNotFoundException
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
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response=PlayerLocation.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + "update player location", response=ErrorResponse.class)
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

        PlayerDbRecord p = db.get(PlayerDbRecord.class, id);  // throws DocumentNotFoundException

        String oldLocation = update.getOldLocation();
        String newLocation = update.getNewLocation();
        PlayerLocation finalLocation = new PlayerLocation();

        // try setting to the new location
        int rc;
        if (p.getLocation()==null || p.getLocation().equals(oldLocation)) {
            p.setLocation(newLocation);
            db.update(p);

            rc = HttpServletResponse.SC_OK;
            finalLocation.setLocation(newLocation);
            kafka.publishPlayerEvent(PlayerEvent.UPDATE_LOCATION, p);
        } else {
            rc = HttpServletResponse.SC_CONFLICT;
            finalLocation.setLocation(p.getLocation());
        }

        return Response.status(rc).entity(finalLocation).build();
    }

    @GET
    @Path("/location")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a specific player location",
        notes = "returns a map of player id to location, location can be null if historically the player has never had a location stored.",
        code = HttpServletResponse.SC_OK ,
        response = PlayerLocation.class)
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response=PlayerLocation.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
    })
    public PlayerLocation getPlayerLocation(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {
        PlayerDbRecord p;

        p = db.get(PlayerDbRecord.class, id); // throws DocumentNotFoundException

        PlayerLocation location = new PlayerLocation();
        location.setLocation(p.getLocation());
        return location;
    }

    @GET
    @Path("/credentials")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get credentials for a specific player",
        notes = "")
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response=PlayerCredentials.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
    })
    public PlayerCredentials getPlayerCredentials(
            @ApiParam(value = "target player id", required = true) @PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        if (unauthorizedId(authId, id)) {
            if(authId==null){
                authId="Unauthenticated User";
            }
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Credentials for Player " + id + " could not be retrieved",
                    authId + " is not allowed to view requested information");
        }

        PlayerDbRecord p = db.get(PlayerDbRecord.class, id);  // throws DocumentNotFoundException

        PlayerCredentials credentials = new PlayerCredentials();
        credentials.setSharedSecret(p.getApiKey());
        credentials.setEmail(p.getEmail());
        return credentials;
    }


    @PUT
    @Path("/credentials/sharedSecret")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update player shared secret",
            notes = "",
            code = HttpServletResponse.SC_OK ,
            response = PlayerArgument.class )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, response=PlayerArgument.class),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + " update player shared secret", response=ErrorResponse.class)
    })
    public Response updatePlayerApiKey(@PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // we don't want to allow this method to be invoked by the server (must be at user's request).
        Claims claims = (Claims) httpRequest.getAttribute("player.claims");
        if ( claims.getAudience().equals("server")) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Unable to update player apikey",
                    "Invalid token type " + claims.getAudience());
        }

        if (unauthorizedId(authId, id)) {
            if(authId==null){
                authId="Unauthenticated User";
            }
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be updated",
                    authId + " is not allowed to update player " + id);
        }

        PlayerDbRecord p = db.get(PlayerDbRecord.class, id);  // throws DocumentNotFoundException

        //if no existing apikey, or apikey exists, but has not been perma-banned..
        if( !ACCESS_DENIED.equals(p.getApiKey())){
            p.setApiKey(SharedSecretGenerator.generateApiKey());
            db.update(p);
            kafka.publishPlayerEvent(PlayerEvent.UPDATE_APIKEY, p);
            return Response.ok(p).build();
        }else{
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Unable to update player apikey",
                    "ApiKey use is banned for player id " + p.getId());
        }
    }

    @PUT
    @Path("/credentials/email")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Update player contact email",
            notes = "",
            code = HttpServletResponse.SC_OK ,
            response = PlayerArgument.class )
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_CONFLICT, message = Messages.CONFLICT, response=ErrorResponse.class),
            @ApiResponse(code = HttpServletResponse.SC_FORBIDDEN, message = Messages.FORBIDDEN + " update player contact email", response=ErrorResponse.class)
    })
    public Response updatePlayerEmail(@PathParam("id") String id) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // we don't want to allow this method to be invoked by the server (must be at user's request).
        Claims claims = (Claims) httpRequest.getAttribute("player.claims");
        if ( claims.getAudience().equals("server")) {
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Unable to update player email",
                    "Invalid token type " + claims.getAudience());
        }

        if (unauthorizedId(authId, id)) {
            if(authId==null){
                authId="Unauthenticated User";
            }
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Player " + id + " could not be updated",
                    authId + " is not allowed to update player " + id);
        }

        PlayerDbRecord p = db.get(PlayerDbRecord.class, id);  // throws DocumentNotFoundException

        //if no existing apikey, or apikey exists, but has not been perma-banned..
        if( !ACCESS_DENIED.equals(p.getApiKey())){
            if(claims.get("email")!=null){
              p.setEmail(claims.get("email").toString());
              db.update(p);
            }
            kafka.publishPlayerEvent(PlayerEvent.UPDATE_EMAIL, p);
            return Response.ok(p).build();
        }else{
            throw new PlayerAccountModificationException(
                    Response.Status.FORBIDDEN,
                    "Unable to update player email",
                    "Updates are banned for player id " + p.getId());
        }
    }

    private boolean unauthorizedId(String user, String player) {
        return ( user == null || !(player.equals(user) || systemId.equals(user)) );
    }
}
