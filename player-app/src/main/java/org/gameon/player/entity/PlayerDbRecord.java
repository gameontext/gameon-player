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
package org.gameon.player.entity;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Player account information")
@JsonInclude(Include.NON_EMPTY)
public class PlayerDbRecord {

    private static final long serialVersionUID = 1L;

    /** Player account/record id */
    @JsonProperty("_id")
    @ApiModelProperty(
            value = "Unique player id",
            readOnly = true,
            name = "_id",
            example = "oauthProvider:userid",
            required = true)
    protected String id;

    /** Document revision */
    @JsonProperty("_rev")
    @ApiModelProperty(hidden = true)
    protected String rev;

    @ApiModelProperty(
            value = "Player name",
            example = "Harriet",
            required = true)
    protected String name;
        
    @ApiModelProperty(
            value = "Favorite color",
            example = "Tangerine",
            required = true)
    protected String favoriteColor;

    @JsonCreator
    public PlayerDbRecord() {}

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getRev() {
        return rev;
    }
    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFavoriteColor() {
        return favoriteColor;
    }

    public void setFavoriteColor(String favoriteColor) {
        this.favoriteColor = favoriteColor;
    }

    @ApiModelProperty(hidden = true)
    private String apiKey;

    @ApiModelProperty(hidden = true)
    private String location;
    
    @JsonIgnore
    public void update(PlayerArgument p) {
        this.id = p.id;
        this.rev = p.rev;
        this.name = p.name;
        this.favoriteColor = p.favoriteColor;
    }
    
    @JsonIgnore
    public void removeProtected() {
        this.apiKey = null;
        this.location = null;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


    @Override
    public String toString() {
        return "Player [id=" + id + ", revision=" + rev +", name=" + name
                + ", favoriteColor=" + favoriteColor
                + ", location=" + location + "]";
    }

    @JsonIgnore
    public void generateApiKey(){
        Encoder e = Base64.getEncoder();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16*2]);
        for(int i=0;i<2; i++){
            UUID u = UUID.randomUUID();
            bb.putLong(u.getMostSignificantBits());
            bb.putLong(u.getLeastSignificantBits());
        }
        setApiKey(e.encodeToString(bb.array()));
    }
}
