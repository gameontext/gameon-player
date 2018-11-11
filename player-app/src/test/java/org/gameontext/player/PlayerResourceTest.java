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
package org.gameontext.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.UpdateConflictException;
import org.gameontext.player.Kafka;
import org.gameontext.player.PlayerAccountResource;
import org.gameontext.player.control.PlayerAccountModificationException;
import org.gameontext.player.entity.LocationChange;
import org.gameontext.player.entity.PlayerArgument;
import org.gameontext.player.entity.PlayerDbRecord;
import org.gameontext.player.entity.PlayerResponse;
import org.junit.Before;
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
    @Injectable Kafka kafka;

    @Injectable(value="testId")
    String systemId = "testId";

    PlayerDbRecord playerDb = new PlayerDbRecord();
    PlayerArgument playerArg = new PlayerArgument();
    @Before
    public void initPlayer(){
        playerDb.setId("123");
        playerDb.setName("Chunky");
        playerDb.setFavoriteColor("Fuschia");
        playerDb.setLocation("Home");
        playerDb.setRev("high");
        playerDb.setApiKey("FISH");

        playerArg.setId("123");
        playerArg.setName("Chunky");
        playerArg.setFavoriteColor("Fuschia");
        playerArg.setRev("high");
    }

    @Test
    public void checkGetMatchingId() throws IOException {
        String playerId = "fish";
        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            request.getAttribute("player.id"); result = playerId;
            dbi.get(PlayerDbRecord.class, playerId); result = playerDb;
        }};

        PlayerResponse result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", playerDb.getId(), result.getId());
        assertNotNull( "Method should return credentials for matching id",result.getCredentials());
    }

    @Test
    public void checkGetMissingId() throws IOException {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); result = null;
            dbi.get(PlayerDbRecord.class, playerId); result = playerDb;
        }};

        PlayerResponse result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", playerDb.getId(), result.getId());
        assertNull( "Method should not return credentials for missing id",result.getCredentials());
    }

    @Test
    public void checkMismatchedId() throws IOException {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); result = "wibble";
            dbi.get(PlayerDbRecord.class, playerId); result = playerDb;
        }};

        PlayerResponse result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", playerDb.getId(), result.getId());
        assertNull( "Method should not return credentials for mismatched id",result.getCredentials());
    }

    @Test(expected=DocumentNotFoundException.class)
    public void checkUnknownId(@Mocked PlayerDbRecord record, @Mocked PlayerResponse player) throws IOException {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); result = playerId;
            dbi.get(PlayerDbRecord.class, playerId); result = new DocumentNotFoundException("player.id");
        }};

        tested.getPlayerInformation(playerId);
    }

    @Test
    public void checkGetSystemId() throws IOException {
        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = "game-on.org";
            dbi.get(PlayerDbRecord.class, playerId); result = playerDb;
        }};

        PlayerResponse result = tested.getPlayerInformation(playerId);

        assertEquals( "Method should return the mocked player", playerDb.getId(), result.getId());
        assertNotNull( "Method should return credentials for system id",result.getCredentials());
    }

    @Test
    public void checkDeleteMatchingId( @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = playerId;
            dbi.get(PlayerDbRecord.class, playerId); result = playerDb;
        }};

        tested.removePlayer(playerId);

        new Verifications() {{
            Response.status(204); times = 1;
            dbi.delete(playerDb); times = 1;
            builder.build(); times = 1;
        }};
    }

    @Test(expected = PlayerAccountModificationException.class)
    public void checkDeleteMissingId(@Mocked PlayerArgument player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = null;
        }};

        tested.removePlayer(playerId);
    }

    @Test
    public void checkDeleteSytemId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = "game-on.org";
            dbi.get(PlayerDbRecord.class, playerId); result = playerDb;
        }};

        tested.removePlayer(playerId);

        new Verifications() {{
            Response.status(204); times = 1;
            dbi.delete(playerDb); times = 1;
            builder.build(); times = 1;
        }};
    }

    @Test(expected = PlayerAccountModificationException.class)
    public void checkDeleteMismatchedId(@Mocked PlayerArgument player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = "Biscuit";
        }};

        tested.removePlayer(playerId);
    }

    @Test(expected=DocumentNotFoundException.class)
    public void checkDeleteUnknownId(@Mocked PlayerArgument player, @Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = playerId;
            dbi.get(PlayerDbRecord.class, playerId); result = new DocumentNotFoundException("player.id");
        }};

        tested.removePlayer(playerId);
    }

    @Test
    public void checkClientUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerDbRecord dbEntry = new PlayerDbRecord();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = playerId;
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
        }};

        PlayerArgument proposed = new PlayerArgument();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);

        new Verifications() {{
            PlayerResponse p;
            Response.ok(p = withCapture());

            assertNotNull("Player response should not be null",p);
            assertEquals("Player Name should have been updated ", proposed.getName(), p.getName());
            assertEquals("Player Color should have been updated ", proposed.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should should have been updated ", proposed.getId(), p.getId());

            dbi.update(dbEntry); times = 1;
        }};
    }

    @Test
    public void checkServerIdUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerDbRecord dbEntry = new PlayerDbRecord();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");

        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = "game-on.org";
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
        }};

        PlayerArgument proposed = new PlayerArgument();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);

        new Verifications() {{
            PlayerResponse p;
            Response.ok(p = withCapture());

            // Server is not allowed to change the player's name or favorite color
            assertNotNull("Player response should not be null",p);
            assertEquals("Player Name should not have been updated", proposed.getName(), p.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), p.getId());
            assertNotNull("Player should have credentials block", p.getCredentials());
            assertEquals("Player ApiKey should not have changed ", "ShinyShoes", p.getCredentials().getSharedSecret());

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
            request.getAttribute("player.id"); result = "game-on.org";
            dbi.get(PlayerDbRecord.class, playerId); result = new DocumentNotFoundException("player.id");
        }};

        PlayerArgument proposed = new PlayerArgument();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);
    }

    @Test(expected = PlayerAccountModificationException.class)
    public void checkClientUpdateNoMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerDbRecord dbEntry = new PlayerDbRecord();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = playerId+"FISH";
        }};

        PlayerArgument proposed = new PlayerArgument();

        tested.updatePlayer(playerId, proposed);
    }

    @Test
    public void checkClientUpdateMatchingIdKeepApiKey(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerDbRecord dbEntry = new PlayerDbRecord();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Claims claims = Jwts.claims();
        claims.setAudience("client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = playerId;
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
        }};

        PlayerArgument proposed = new PlayerArgument();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        tested.updatePlayer(playerId, proposed);

        new Verifications() {{
            PlayerResponse p;
            Response.ok(p = withCapture());

            assertNotNull("Player response should not be null",p);
            assertEquals("Player Name was not updated ", proposed.getName(), p.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), p.getId());
            assertEquals("Player ApiKey should not have changed ", "ShinyShoes", p.getCredentials().getSharedSecret());

            dbi.update(dbEntry); times = 1;
        }};
    }

    @Test
    public void checkServerUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        Claims claims = Jwts.claims();
        claims.setAudience("server");

        PlayerDbRecord dbEntry = new PlayerDbRecord();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        PlayerArgument proposed = new PlayerArgument();
        proposed.setName("AnotherName");
        proposed.setFavoriteColor("AnotherColor");
        proposed.setId(playerId);

        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); result = playerId;
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
        }};


        tested.updatePlayer(playerId, proposed);

        new Verifications() {{

            PlayerDbRecord p;
            PlayerResponse pr;
            dbi.update(p = withCapture()); times = 1;
            Response.ok(pr = withCapture()); times = 1;

            assertNotNull("Player response should not be null",p);
            assertEquals("Player Name should not have changed ", dbEntry.getName(), p.getName());
            assertEquals("Player Color should not have changed ", dbEntry.getFavoriteColor(), p.getFavoriteColor());
            assertEquals("Player Id should not have changed ", dbEntry.getId(), p.getId());
            assertEquals("Player ApiKey should not have changed ", "ShinyShoes", p.getApiKey());
        }};
    }

    @Test
    public void checkServerLocationUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {
        String playerId = "fish";

        PlayerDbRecord dbEntry = new PlayerDbRecord();
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
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
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

        PlayerDbRecord dbEntry = new PlayerDbRecord();
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
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
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

        PlayerDbRecord dbEntry = new PlayerDbRecord();
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
            request.getAttribute("player.claims"); result = claims;
            dbi.get(PlayerDbRecord.class, playerId); result = dbEntry;
            dbi.update(any); result = new UpdateConflictException();
        }};

        tested.updatePlayerLocation(playerId, locChange);
    }

}
