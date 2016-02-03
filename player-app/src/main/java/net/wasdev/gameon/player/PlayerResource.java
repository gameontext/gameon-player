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
package net.wasdev.gameon.player;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.servlet.http.HttpServletRequest;
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
import org.ektorp.CouchDbInstance;
import org.ektorp.UpdateConflictException;
import org.ektorp.impl.StdCouchDbConnector;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The Player service, where players remember where they are, and what they have
 * in their pockets.
 *
 */
@Path("/{id}")
public class PlayerResource {
    @Context
    HttpServletRequest httpRequest;
    
    @Resource(name = "couchdb/connector")
    protected CouchDbInstance dbi;
       
    protected CouchDbConnector db;
    
    @PostConstruct
    protected void postConstruct() {
        db = new StdCouchDbConnector("playerdb", dbi); 
        db.createDatabaseIfNotExists();         
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Player getPlayerInformation(@PathParam("id") String id) throws IOException {
        
        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");
        
        if(authId == null){        
            if(db.contains(id)){
                Player p = db.get(Player.class, id);  
                p.setApiKey("NOT ALLOWED VIA UNAUTHENTICATED GET");
                return p ; 
            }else{
                throw new PlayerNotFoundException("Id not known");
            }            
        }else{
            // only allow get for matching id.
            if ( !authId.equals(id)) {
                throw new RequestNotAllowedForThisIDException("Bad authentication id");
            }        
            if(db.contains(id)){
                Player p = db.get(Player.class, id);             
                return p ; 
            }else{
                throw new PlayerNotFoundException("Id not known");
            }
        }
    }

    @PUT
    public Response updatePlayer(@PathParam("id") String id, Player newPlayer) throws IOException {
        // we don't want to allow this method to be invoked by a user.
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) httpRequest.getAttribute("player.claims");
        if (!"server".equals(claims.get("aud"))) {
            //if the audience isn't server.. we only allow update of selected Fields.
            Player requested = newPlayer;
            //lookup the matching record & clone the fields into it
            if(db.contains(requested.getId())){                
                newPlayer = db.get(Player.class, requested.getId());
                //we ONLY allow these fields to be updated with a non server audience jwt.
                newPlayer.setName(requested.getName());
                newPlayer.setFavoriteColor(requested.getFavoriteColor());
                
                //if the inbound profile has no apiKey set at all.. we regenerate the apikey for
                //this player..
                if(requested.getApiKey()==null){
                    newPlayer.generateApiKey();
                }               
            }else{
                throw new PlayerNotFoundException("Id not known");
            }
        }
              
        db.update(newPlayer);

        return Response.status(204).build();
    }

    @DELETE
    public Response removePlayer(@PathParam("id") String id) throws IOException{
        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // players are allowed to delete themselves..
        // only allow delete for matching id.
        if (authId == null || !authId.equals(id)) {
            return Response.status(403).entity("Bad authentication id").build();
        }
        
        Player p = db.get(Player.class, id);
        if(p!=null){
            db.delete(p);
        }
        
        return Response.status(200).build();
    }

    @PUT
    @Path("/location")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePlayerLocation(@PathParam("id") String id, JsonObject update) throws IOException {
        // we don't want to allow this method to be invoked by a user.
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>) httpRequest.getAttribute("player.claims");
        if (!"server".equals(claims.get("aud"))) {
            throw new RequestNotAllowedForThisIDException("Invalid token type " + claims.get("aud"));
        }
        
        Player p = db.get(Player.class, id);

        String oldLocation = update.getString("old");
        String newLocation = update.getString("new");
        String currentLocation = p.getLocation();

        // try setting to the new location
        int rc;
        JsonObjectBuilder result = Json.createObjectBuilder();

        if (currentLocation.equals(oldLocation)) {
            p.setLocation(newLocation);
            try {
                db.update(p);
                rc = 200;
                result.add("location", newLocation);
            } catch (UpdateConflictException e) {
                rc = 500;
                result.add("location", currentLocation);
            }
        } else {
            rc = 409;
            result.add("location", currentLocation);
        }

        return Response.status(rc).entity(result.build()).build();
    }

}
