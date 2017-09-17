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
package org.gameontext.player.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Error response")
@JsonInclude(Include.NON_EMPTY)
public class ErrorResponse {

    @ApiModelProperty(
            value = "Http Response code (for reference)",
            example = "400",
            required = true)
    private int status;

    @ApiModelProperty(
            value = "Error message",
            example = "Unauthenticated client",
            required = true)
    private String message;

    @ApiModelProperty(
            value = "Optional additional information",
            example = "Room owner could not be determined.",
            required = false)
    private String more_info;

    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public String getMore_info() {
        return more_info;
    }
    public void setMore_info(String more_info) {
        this.more_info = more_info;
    }


}
