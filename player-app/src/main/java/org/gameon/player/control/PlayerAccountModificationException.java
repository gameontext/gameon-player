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
package org.gameon.player.control;

import javax.ws.rs.core.Response;

public class PlayerAccountModificationException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final Response.Status status;
    private final String moreInfo;

    public PlayerAccountModificationException(String message) {
        this(message, null);
    }

    public PlayerAccountModificationException(String message, String moreInfo) {
        this(Response.Status.INTERNAL_SERVER_ERROR, message, moreInfo);
    }

    public PlayerAccountModificationException(Response.Status status, String message, String moreInfo) {
        super(message);
        this.status = status;
        this.moreInfo = moreInfo;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getMoreInfo() {
        return moreInfo;
    }
}
