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
package net.wasdev.gameon.player;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.ExceptionMapper;

import org.junit.Test;

import mockit.Expectations;
import mockit.Mocked;
import mockit.Verifications;

public class ExceptionMappingTest {
    @Test
    public void testPlayerNotFoundException(@Mocked Response response, @Mocked ResponseBuilder builder){
        new Expectations() {{
            builder.build(); returns(response);
        }};

        //exception is its own mapper.
        ExceptionMapper<PlayerNotFoundException> mapper = new PlayerNotFoundException();

        //use mapper to map request into response.
        Response r = mapper.toResponse(new PlayerNotFoundException());

        //check response is the one we are testing.
        assertEquals("Response returned is the one from build invocation ", response, r);

        new Verifications() {{
            //check we got 404
            Response.status(404); times = 1;
            //with some reason text
            builder.entity(anyString); times = 1;
            //with a text mimetype.
            builder.type("text/plain"); times = 1;
            //and it was built into our response..
            builder.build(); times = 1;
        }};
    }
    @Test
    public void testRequestNotAllowedForThisIDException(@Mocked Response response, @Mocked ResponseBuilder builder){
        new Expectations() {{
            builder.build(); returns(response);
        }};

        //exception is its own mapper.
        ExceptionMapper<RequestNotAllowedForThisIDException> mapper = new RequestNotAllowedForThisIDException();

        //use mapper to map request into response.
        Response r = mapper.toResponse(new RequestNotAllowedForThisIDException());

        //check response is the one we are testing.
        assertEquals("Response returned is the one from build invocation ", response, r);

        new Verifications() {{
            //check we got 403
            Response.status(403); times = 1;
            //with some reason text
            builder.entity(anyString); times = 1;
            //with a text mimetype.
            builder.type("text/plain"); times = 1;
            //and it was built into our response..
            builder.build(); times = 1;
        }};
    }
}
