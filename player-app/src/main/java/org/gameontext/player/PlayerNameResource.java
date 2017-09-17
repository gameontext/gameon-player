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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * The Player Name Service, where we get to come up with names for players.. 
 *
 */
@Path("/name")
@Api( tags = {"players"})
public class PlayerNameResource {
    
    @Context
    HttpServletRequest httpRequest;

    //tables brought over from javascript.. 
    private final static String[] size = {"Tiny","Small","Large","Gigantic","Enormous"};
    private final static String[] composition = {"Chocolate","Fruit","GlutenFree","Sticky"};
    private final static String[] extra = {"Iced","Frosted","CreamFilled","JamFilled","MapleSyrupSoaked","SprinkleCovered"};
    private final static String[] form = {"FairyCake","CupCake","Muffin","PastrySlice","Doughnut"};

    private int getRandom(int max){
        return ThreadLocalRandom.current().nextInt(0, max);
    }
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get ten randomly generated player names", 
        notes = "", 
        responseContainer = "List",
        response = String.class)
        @ApiResponses(value = {
            @ApiResponse(code = HttpServletResponse.SC_OK, message = Messages.SUCCESSFUL, 
                    responseContainer = "List", response = String.class),
    })
    public List<String> getNames() {
        Set<String> tenNames = new HashSet<String>();
        while(tenNames.size()<10){
            tenNames.add(getName());
        }
        return new ArrayList<String>(tenNames);
    }
    
    private String getName(){
        int variant = getRandom(6);
        String name = null;
        switch(variant){
            case 0:
                name = size[getRandom(size.length)]+composition[getRandom(composition.length)]+form[getRandom(form.length)];
                break;
            case 1:
                name = composition[getRandom(composition.length)]+extra[getRandom(extra.length)]+form[getRandom(form.length)];
                break;
            case 2:
                name = size[getRandom(size.length)]+extra[getRandom(extra.length)]+form[getRandom(form.length)];
                break;
            case 3:
                name = extra[getRandom(extra.length)]+form[getRandom(form.length)];
                break;
            case 4:
                name = size[getRandom(size.length)]+form[getRandom(form.length)];
                break;
            case 5:
                name = composition[getRandom(composition.length)]+form[getRandom(form.length)];
                break;
            default:
                name = "fish";
        }
        return name;
    }

}
