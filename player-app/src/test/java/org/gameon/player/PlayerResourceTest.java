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
package org.gameon.player;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.gameon.player.control.PlayerAccountModificationException;
import org.gameon.player.entity.LocationChange;
import org.gameon.player.entity.Player;
import org.gameon.player.entity.PlayerFull;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class PlayerResourceTest {

    @Tested PlayerAccountResource tested;
    @Injectable CouchDbConnector dbi;
    @Injectable HttpServletRequest request;

    @Injectable(value="testId")
    String systemId = "testId";

    @Test
    public void checkGetMatchingId(@Mocked Player player) throws IOException {
        String playerId = "fish";
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.get(PlayerFull.class, playerId); returns(player);
        }};

        Player result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", player, result);
    }

    @Test
    public void checkGetMissingId(@Mocked Player player) throws IOException {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns(null);
            dbi.get(Player.class, playerId); returns(player);
        }};

        Player result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", player, result);
    }

    @Test
    public void checkMismatchedId(@Mocked Player player) throws IOException {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns("wibble");
            dbi.get(Player.class, playerId); returns(player);
        }};

        Player result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", player, result);
    }

    @Test(expected=DocumentNotFoundException.class)
    public void checkUnknownId(@Mocked Player player) throws IOException {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.get(PlayerFull.class, playerId); result = new DocumentNotFoundException("player.id");
        }};

        tested.getPlayerInformation(playerId);
    }

    @Test
    public void checkGetSystemId(@Mocked Player player) throws IOException {
        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            dbi.get(PlayerFull.class, playerId); returns(player);
        }};

        Player result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", player, result);
    }

    @Test
    public void checkDeleteMatchingId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            dbi.get(Player.class, playerId); returns(player);
        }};

        tested.removePlayer(playerId);

        new Verifications() {{
            Response.status(204); times = 1;
            dbi.delete(player); times = 1;
            builder.build(); times = 1;
        }};
    }

    @Test(expected = PlayerAccountModificationException.class)
    public void checkDeleteMissingId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(null);
        }};

        tested.removePlayer(playerId);
    }

    @Test
    public void checkDeleteSytemId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            dbi.get(Player.class, playerId); returns(player);
        }};

        tested.removePlayer(playerId);

        new Verifications() {{
            Response.status(204); times = 1;
            dbi.delete(player); times = 1;
            builder.build(); times = 1;
        }};
    }

    @Test(expected = PlayerAccountModificationException.class)
    public void checkDeleteMismatchedId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("Biscuit");
        }};

        tested.removePlayer(playerId);
    }

    @Test(expected=DocumentNotFoundException.class)
    public void checkDeleteUnknownId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            dbi.get(Player.class, playerId); result = new DocumentNotFoundException("player.id");
        }};

        tested.removePlayer(playerId);
    }

    @Test
    public void checkClientUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
        }};

        Player proposed = new Player();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);

        new Verifications() {{
            PlayerFull p;
            Response.ok(p = withCapture());

            assertEquals("Player Name should have been updated ", proposed.getName(), p.getName());
            assertEquals("Player Color should have been updated ", proposed.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should should have been updated ", proposed.getId(), p.getId());

            dbi.update(dbEntry); times = 1;
        }};
    }

    @Test
    public void checkServerIdUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
        }};

        Player proposed = new Player();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);

        new Verifications() {{
            PlayerFull p;
            Response.ok(p = withCapture());

            // Server is not allowed to change the player's name or favorite color
            assertEquals("Player Name should not have been updated", proposed.getName(), p.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), p.getId());
            assertEquals("Player ApiKey should not have changed ", "ShinyShoes", p.getApiKey());

            dbi.update(dbEntry); times = 1;
        }};
    }

    @Test(expected=DocumentNotFoundException.class)
    public void checkClientUpdateMissingDbEntry(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            dbi.get(PlayerFull.class, playerId); result = new DocumentNotFoundException("player.id");
        }};

        Player proposed = new Player();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);
    }

    @Test(expected = PlayerAccountModificationException.class)
    public void checkClientUpdateNoMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId+"FISH");
        }};

        Player proposed = new Player();

        tested.updatePlayer(playerId, proposed);
    }

    @Test
    public void checkClientUpdateMatchingIdKeepApiKey(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
        }};

        PlayerFull proposed = new PlayerFull();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setApiKey("FISH"); //doesn't matter what the value is, as long as its not null.
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);

        new Verifications() {{
            PlayerFull p;
            Response.ok(p = withCapture());

            assertEquals("Player Name was not updated ", proposed.getName(), p.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), p.getId());
            assertEquals("Player ApiKey should not have been updated ", "ShinyShoes", p.getApiKey());

            dbi.update(dbEntry); times = 1;
        }};
    }

    @Test
    public void checkServerUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        Claims claims = Jwts.claims();
        claims.setAudience("server");

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        PlayerFull proposed = new PlayerFull();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setApiKey("FISH"); //with aud server, fish will become the new key.
        proposed.setId(playerId);
        
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
        }};


        tested.updatePlayer(playerId, proposed);

        new Verifications() {{

            PlayerFull p;
            dbi.update(p = withCapture()); times = 1;
            Response.ok(p); times = 1;

            assertEquals("Player Name should not have changed ", dbEntry.getName(), p.getName());
            assertEquals("Player Color should not have changed ", dbEntry.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should not have changed ", dbEntry.getId(), p.getId());
            assertEquals("Player ApiKey should not have been updated ", "ShinyShoes", p.getApiKey());
        }};
    }

    @Test
    public void checkServerLocationUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");
        
        LocationChange locChange = new LocationChange();
        locChange.setOldLocation("Earth");
        locChange.setNewLocation("Mars");

        Claims claims = Jwts.claims();
        claims.setAudience("server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
        }};

        tested.updatePlayerLocation(playerId, locChange);

        new Verifications() {{
            dbi.update(any); times = 1;
            //check we got 200
            Response.status(200); times = 1;
        }};
    }

    @Test
    public void checkServerLocationUpdateMatchingIdNoMatchingLocation(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");

        LocationChange locChange = new LocationChange();
        locChange.setOldLocation("Mars");
        locChange.setNewLocation("Venus");

        Claims claims = Jwts.claims();
        claims.setAudience("server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
        }};

        tested.updatePlayerLocation(playerId, locChange);

        new Verifications() {{
            dbi.update(any); times = 0;
            //check we got 409
            Response.status(409); times = 1;
        }};
    }

    @Test(expected = UpdateConflictException.class)
    public void checkServerLocationUpdateMatchingIdConflict(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");

        LocationChange locChange = new LocationChange();
        locChange.setOldLocation("Earth");
        locChange.setNewLocation("Mars");

        Claims claims = Jwts.claims();
        claims.setAudience("server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.claims"); returns(claims);
            dbi.get(PlayerFull.class, playerId); returns(dbEntry);
            dbi.update(any); result = new UpdateConflictException();
        }};

        tested.updatePlayerLocation(playerId, locChange);
    }

}
