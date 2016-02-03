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
package net.wasdev.gameon.player.boundary;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class PlayerNotFoundException extends RuntimeException implements ExceptionMapper<PlayerNotFoundException> {

    private static final long serialVersionUID = 1L;

    public PlayerNotFoundException() {
    }

    public PlayerNotFoundException(String message) {
        super(message);
    }

    @Override
    public Response toResponse(PlayerNotFoundException exception) {
        return Response.status(404).entity("Player not found").type("text/plain").build();
    }
}
