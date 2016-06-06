package org.gameon.player.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Player credentials")
public class PlayerCredentials {

    @ApiModelProperty(value = "shared secret for player", example = "fjhre8h49hf438u9h45", required = true)
    protected String sharedSecret;

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
