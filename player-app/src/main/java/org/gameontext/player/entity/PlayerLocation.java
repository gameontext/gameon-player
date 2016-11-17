package org.gameontext.player.entity;

import org.gameontext.player.PlayerApplication;

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
        if(location==null){
            this.location = PlayerApplication.FIRST_ROOM;
        }else{
            this.location = location;
        }
    }    
}
