package org.gameontext.player.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Player location change")
public class LocationChange {

    @ApiModelProperty(
            value = "old player location",
            example = "room_id_1",
            required = true)
    protected String oldLocation;
        
    @ApiModelProperty(
            value = "new player location",
            example = "room_id_1",
            required = true)
    protected String newLocation;

    public String getOldLocation() {
        return oldLocation;
    }

    public void setOldLocation(String oldLocation) {
        this.oldLocation = oldLocation;
    }

    public String getNewLocation() {
        return newLocation;
    }

    public void setNewLocation(String newLocation) {
        this.newLocation = newLocation;
    }
    
    
}
