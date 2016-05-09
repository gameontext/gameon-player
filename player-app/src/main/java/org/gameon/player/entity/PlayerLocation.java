package org.gameon.player.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Player location")
public class PlayerLocation {

    @ApiModelProperty(
            value = "current player location",
            example = "room_id_1",
            required = true)
    protected String location;
        
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }    
}
