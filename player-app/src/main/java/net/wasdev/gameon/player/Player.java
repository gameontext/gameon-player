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

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.UUID;

import org.ektorp.support.CouchDbDocument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_EMPTY)
public class Player extends CouchDbDocument{

    private static final long serialVersionUID = 1L;
   
    private String name;
    private String apiKey;
    private String authBy;
    private String location;
    private String favoriteColor;
    
    @JsonCreator
    public Player() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAuthBy() {
        return authBy;
    }

    public void setAuthBy(String authBy) {
        this.authBy = authBy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFavoriteColor() {
        return favoriteColor;
    }

    public void setFavoriteColor(String favoriteColor) {
        this.favoriteColor = favoriteColor;
    }

    @Override
    public String toString() {
        return "Player [id=" + getId() + ", revision=" + getRevision() +", name=" + name + ", authBy=" + authBy + ", location=" + location
                + ", favoriteColor=" + favoriteColor + "]";
    }
    
    //package visibility so can be invoked from places loading object
    @JsonIgnore
    void generateApiKey(){
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
