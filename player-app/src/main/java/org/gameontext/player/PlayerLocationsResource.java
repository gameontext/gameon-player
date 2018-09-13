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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
import org.gameontext.player.entity.ErrorResponse;
import org.gameontext.player.entity.PlayerDbRecord;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.eclipse.microprofile.metrics.annotation.Timed;
import org.eclipse.microprofile.metrics.annotation.Metered;
import org.eclipse.microprofile.metrics.annotation.Counted;
/**
 * The Player location service, where we get to say where the players are.
 *
 */
@Path("/locations")
@Api( tags = {"players"})
public class PlayerLocationsResource {

    @Inject
    protected CouchDbConnector db;

    @Context
    HttpServletRequest httpRequest;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get player locations.",
        notes = "can pass optional params playerId to retrieve only location for player, "
               +"or siteId to retrieve players in a location, combining the two will return an empty"
               +" map if the player is not in that location",
        responseContainer = "Map")
    @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL,
                    responseContainer = "Map"),
            @ApiResponse(code = HttpServletResponse.SC_NOT_FOUND, message = Messages.NOT_FOUND, response=ErrorResponse.class),
    })
    @Timed(name = "getPlayerLocationInformation_timer",
        reusable = true,
        tags = "label=playerLocationsResource")
    @Counted(name = "getPlayerLocationInformation_count",
        monotonic = true,
        reusable = true,
        tags = "label=playerLocationsResource")
    @Metered(name = "getPlayerLocationInformation_meter",
        reusable = true,
        tags = "label=playerLocationsResource")
    public Map<String,String> getPlayerLocationInformation(
            @ApiParam(value = "target player id", required = false) @QueryParam("playerId") String playerId,
            @ApiParam(value = "target site id", required = false) @QueryParam("siteId") String siteId) throws IOException {
        
        Map<String,String> locations = new HashMap<String,String>();
        
        if(playerId!=null){
            PlayerDbRecord p = db.get(PlayerDbRecord.class, playerId);
            if(siteId==null || siteId.equals(p.getLocation()) ||
                (siteId.equals(PlayerApplication.FIRST_ROOM) && p.getLocation()==null)
              ){
                locations.put(p.getId(), p.getLocation()==null?PlayerApplication.FIRST_ROOM:p.getLocation());
            }
        }else{
            ViewQuery all = new ViewQuery().designDocId("_design/players").viewName("all").cacheOk(true).includeDocs(true);
            List<PlayerDbRecord> results = db.queryView(all, PlayerDbRecord.class);
            results
                .stream()
                .filter( player -> siteId==null ||
                                   siteId.equals(player.getLocation()) ||
                                  (siteId.equals(PlayerApplication.FIRST_ROOM) && player.getLocation()==null)
                       )
                .forEach( player -> locations.put(player.getId(), player.getLocation()==null?PlayerApplication.FIRST_ROOM:player.getLocation()));
        }
        
        return locations;
    }
    
}
