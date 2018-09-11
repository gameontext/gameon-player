/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.ektorp.CouchDbConnector;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/")
@Api( value = "players")
public class PlayersResource {


    @Inject
    protected CouchDbConnector db;

    @GET
    @ApiOperation(value="basic ping", hidden = true)
    public Response basicGet() {
        return Response.ok().build();
    }

    /**
     * GET /players/v1/health
     */
    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value="health check", hidden = true)
    public Response healthCheck() {
        if ( db != null && db.getDbInfo() != null ) {
            return Response.ok().entity("{\"status\":\"UP\"}").build();
        }  else {
            System.out.println("db = " + db);
            return Response.status(Status.SERVICE_UNAVAILABLE).entity("{\"status\":\"DOWN\"}").build();
        }
    }
}
