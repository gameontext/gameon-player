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
package net.wasdev.gameon.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.ektorp.CouchDbConnector;
import org.ektorp.UpdateConflictException;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class PlayerResourceTest {

    @Tested PlayerResource tested;
    @Injectable CouchDbConnector dbi;
    @Injectable HttpServletRequest request;
    @Injectable Kafka kafka;

    @Injectable(value="testId")
    String systemId = "testId";

    @Test
    public void checkGetMatchingId(@Mocked Player player) {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(player);
        }};

        try{
            Player result = tested.getPlayerInformation(playerId);

            assertEquals( "Player was not the expected result", player, result);

            new Verifications() {{
                player.setApiKey(anyString); times = 0;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkGetMissingId(@Mocked Player player) {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns(null);
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(player);
            player.setApiKey("ACCESS_DENIED");
        }};

        try{
            Player result = tested.getPlayerInformation(playerId);

            assertEquals( "Player was not the expected result", player, result);

            new Verifications() {{
                player.setApiKey("ACCESS_DENIED"); times = 1;
            }};

        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkMismatchedId(@Mocked Player player) {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns("wibble");
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(player);
            player.setApiKey("ACCESS_DENIED");
        }};

        try{
            Player result = tested.getPlayerInformation(playerId);

            assertEquals( "Player was not the expected result", player, result);

            new Verifications() {{
                player.setApiKey("ACCESS_DENIED"); times = 1;
            }};

        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkUnknownId(@Mocked Player player) {
        String playerId = "fish";
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.contains(playerId); returns(false);
        }};

        try{
            @SuppressWarnings("unused")
            Player result = tested.getPlayerInformation(playerId);

            fail("unknown id should have thrown PlayerNotFoundException");
        }catch(PlayerNotFoundException e){
            //expected
        }catch(IOException io){
            fail("unknown id should have thrown PlayerNotFoundException");
        }
    }

    @Test
    public void checkGetSystemId(@Mocked Player player) {
        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(player);
        }};

        try{
            Player result = tested.getPlayerInformation(playerId);

            assertEquals( "Player was not the expected result", player, result);

            new Verifications() {{
                player.setApiKey(anyString); times = 0;
            }};

        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkDeleteMatchingId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder){

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            dbi.get(Player.class, playerId); returns(player);
        }};

        try{
            @SuppressWarnings("unused")
            Response result = tested.removePlayer(playerId);

            new Verifications() {{
                Response.status(200); times = 1;
                dbi.delete(player); times = 1;
                builder.build(); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkDeleteMissingId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder){

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(null);
        }};

        try{
            @SuppressWarnings("unused")
            Response result = tested.removePlayer(playerId);

            new Verifications() {{
                Response.status(403); times = 1;
                dbi.delete(any); times = 0;
                builder.entity(anyString); times = 1;
                builder.build(); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkDeleteSytemId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder){

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            dbi.get(Player.class, playerId); returns(player);
        }};

        try{
            @SuppressWarnings("unused")
            Response result = tested.removePlayer(playerId);

            new Verifications() {{
                Response.status(200); times = 1;
                dbi.delete(player); times = 1;
                builder.build(); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkDeleteMismatchedId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder){

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("Biscuit");
        }};

        try{
            @SuppressWarnings("unused")
            Response result = tested.removePlayer(playerId);

            new Verifications() {{
                Response.status(403); times = 1;
                dbi.delete(any); times = 0;
                builder.entity(anyString); times = 1;
                builder.build(); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkDeleteUnknownId(@Mocked Player player, @Mocked Response response, @Mocked ResponseBuilder builder){

        String playerId = "fish";
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            dbi.get(Player.class, playerId); returns(null);
        }};

        try{
            @SuppressWarnings("unused")
            Response result = tested.removePlayer(playerId);

            fail("unknown id should have thrown PlayerNotFoundException");
        }catch(PlayerNotFoundException e){
            //expected
        }catch(IOException io){
            fail("unknown id should have thrown PlayerNotFoundException");
        }
    }

    @Test
    public void checkClientUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            request.getAttribute("player.claims"); returns(claims);
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(dbEntry);
        }};

        try{
            Player proposed = new Player();
            proposed.setName("AnotherName");
            proposed.setFavoriteColor("AnotherColor");
            proposed.setId(playerId);

            Response result = tested.updatePlayer(playerId, proposed);

            assertEquals("Player Name was not updated ", proposed.getName(), dbEntry.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), dbEntry.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), dbEntry.getId());
            assertNotEquals("Player ApiKey should have been updated ", dbEntry.getApiKey(), "ShinyShoes");

            new Verifications() {{
                dbi.update(dbEntry); times = 1;
                //check we got 204
                Response.status(204); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkServerIdUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            request.getAttribute("player.claims"); returns(claims);
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(dbEntry);
        }};

        try{
            Player proposed = new Player();
            proposed.setName("AnotherName");
            proposed.setFavoriteColor("AnotherColor");
            proposed.setId(playerId);

            Response result = tested.updatePlayer(playerId, proposed);

            assertEquals("Player Name was not updated ", proposed.getName(), dbEntry.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), dbEntry.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), dbEntry.getId());
            assertNotEquals("Player ApiKey should have been updated ", dbEntry.getApiKey(), "ShinyShoes");

            new Verifications() {{
                dbi.update(dbEntry); times = 1;
                //check we got 204
                Response.status(204); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkClientUpdateMissingDbEntry(@Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns("game-on.org");
            request.getAttribute("player.claims"); returns(claims);
            dbi.contains(playerId); returns(false);
        }};

        try{
            Player proposed = new Player();
            proposed.setName("AnotherName");
            proposed.setFavoriteColor("AnotherColor");
            proposed.setId(playerId);

            try{
                Response result = tested.updatePlayer(playerId, proposed);
                fail("updatePlayer with no db entry did not throw playernotfound");
            }catch(PlayerNotFoundException e){
                //expected.
            }

            new Verifications() {{
                dbi.update(any); times = 0;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkClientUpdateNoMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId+"FISH");
        }};

        try{
            Player proposed = new Player();

            Response result = tested.updatePlayer(playerId, proposed);

            new Verifications() {{
                dbi.update(dbEntry); times = 0;
                //check we got 204
                Response.status(403); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkClientUpdateMatchingIdKeepApiKey(@Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","client");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            request.getAttribute("player.claims"); returns(claims);
            dbi.contains(playerId); returns(true);
            dbi.get(Player.class, playerId); returns(dbEntry);
        }};

        try{
            Player proposed = new Player();
            proposed.setName("AnotherName");
            proposed.setFavoriteColor("AnotherColor");
            proposed.setApiKey("FISH"); //doesn't matter what the value is, as long as its not null.
            proposed.setId(playerId);

            Response result = tested.updatePlayer(playerId, proposed);

            assertEquals("Player Name was not updated ", proposed.getName(), dbEntry.getName());
            assertEquals("Player Color was not updated ", proposed.getFavoriteColor(), dbEntry.getFavoriteColor());
            assertEquals("Player Id should not have changed ", proposed.getId(), dbEntry.getId());
            assertEquals("Player ApiKey should not have been updated ", dbEntry.getApiKey(), "ShinyShoes");

            new Verifications() {{
                dbi.update(dbEntry); times = 1;
                //check we got 204
                Response.status(204); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkServerUpdateMatchingId(@Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.id"); returns(playerId);
            request.getAttribute("player.claims"); returns(claims);
        }};

        try{
            Player proposed = new Player();
            proposed.setName("AnotherName");
            proposed.setFavoriteColor("AnotherColor");
            proposed.setApiKey("FISH"); //with aud server, fish will become the new key.
            proposed.setId(playerId);

            Response result = tested.updatePlayer(playerId, proposed);

            new Verifications() {{
                dbi.update(proposed); times = 1;
                //check we got 204
                Response.status(204); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkServerLocationUpdateMatchingId(@Mocked JsonObject update, @Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.claims"); returns(claims);
            update.getString("old"); returns("Earth");
            update.getString("new"); returns("Mars");
            dbi.get(Player.class, playerId); returns(dbEntry);
        }};

        try{
            Response result = tested.updatePlayerLocation(playerId, update);

            new Verifications() {{
                dbi.update(any); times = 1;
                //check we got 200
                Response.status(200); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkServerLocationUpdateMatchingIdNoMatchingLocation(@Mocked JsonObject update, @Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.claims"); returns(claims);
            update.getString("old"); returns("Mars");
            update.getString("new"); returns("Venus");
            dbi.get(Player.class, playerId); returns(dbEntry);
        }};

        try{
            Response result = tested.updatePlayerLocation(playerId, update);

            new Verifications() {{
                dbi.update(any); times = 0;
                //check we got 409
                Response.status(409); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }

    @Test
    public void checkServerLocationUpdateMatchingIdConflict(@Mocked JsonObject update, @Mocked Response response, @Mocked ResponseBuilder builder){
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");

        Map<String,Object> claims = new HashMap<String,Object>();
        claims.put("aud","server");
        new Expectations() {{
            tested.systemId = "game-on.org";
            request.getAttribute("player.claims"); returns(claims);
            update.getString("old"); returns("Earth");
            update.getString("new"); returns("Mars");
            dbi.get(Player.class, playerId); returns(dbEntry);
            dbi.update(any); result = new UpdateConflictException();
        }};

        try{
            Response result = tested.updatePlayerLocation(playerId, update);

            new Verifications() {{
                dbi.update(any); times = 1;
                //check we got 500
                Response.status(500); times = 1;
            }};
        }catch(IOException io){
            fail("Bad exception");
        }
    }



}
