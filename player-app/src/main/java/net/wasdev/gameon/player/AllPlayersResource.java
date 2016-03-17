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
import java.util.List;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;

/**
 * All the players, and searching for players.
 *
 */
@Path("/")
public class AllPlayersResource {
    @Context
    HttpServletRequest httpRequest;
    
    @Inject
    protected CouchDbConnector db;
    
    @Resource(lookup = "systemId")
    String systemId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Player> getAllPlayers() throws IOException {
        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");
        
        ViewQuery q = new ViewQuery().allDocs().includeDocs(true);
        List<Player> results = db.queryView(q, Player.class);  
        
        //filter out the apikey info if unauthed, or for non matching ids
        //(unless id is system in which case allow it)
        for(Player p : results){
            if(authId == null || !(authId.equals(p.getId()) || authId.equals(systemId))){
                p.setApiKey("ACCESS_DENIED");
            }
        }
        
        return results;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPlayer(Player player) throws IOException {

        // set by the auth filter.
        String authId = (String) httpRequest.getAttribute("player.id");

        // only allow create for matching id.
        if (authId == null || !(authId.equals(player.getId())||authId.equals(systemId))) {
            return Response.status(403).entity("Bad authentication id").build();
        }
        
        if(db.contains(player.getId())){
            return Response.status(409).entity("Error player : " + player.getName() + " already exists").build();
        }
        
        if(player.getApiKey()==null){
            player.generateApiKey();
        }

        db.create(player);

        return Response.status(201).build();
    }
}
