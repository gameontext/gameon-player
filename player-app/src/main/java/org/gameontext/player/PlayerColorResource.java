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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Player Color Service, where we get to come up with the players favorite color, as long as they like pink.
 *
 */
@Path("/color")
@Api( tags = {"players"})
public class PlayerColorResource {
    
    private final static String[] prefix = {"Light","Dark","Faded","Vivid","Intense"};
    private final static String[] color = {"Pink","Purple","Violet","Orchid","Mauve","Rose","Fuschia","Cerise","Lavender","Magenta","Lilac"};

    private int getRandom(int max){
        return ThreadLocalRandom.current().nextInt(0, max);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get ten generated colors", 
        notes = "", 
        responseContainer = "List",
        response = String.class)
        @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL),
    })
    public List<String> getColors() {
        Set<String> tenColors = new HashSet<String>();
        while(tenColors.size()<10){
            tenColors.add(getColor());
        }
        return new ArrayList<String>(tenColors);
    }
    
    private String getColor() {
        int variant = getRandom(2);
        String name = null;
        switch(variant){
            case 0:
                name = color[getRandom(color.length)];
                break;
            case 1:
                name = prefix[getRandom(prefix.length)]+color[getRandom(color.length)];
                break;
            default:
                name = "Baby Pink";
        }
        return name;
    }

}
