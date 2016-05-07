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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.ektorp.CouchDbConnector;
import org.ektorp.UpdateConflictException;
import org.ektorp.ViewQuery;
import org.gameon.player.control.PlayerAccountModificationException;
import org.gameon.player.entity.Player;
import org.gameon.player.entity.PlayerFull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class AllPlayersResourceTest {

    @Tested AllPlayersResource tested;
    @Injectable CouchDbConnector dbi;
    @Injectable HttpServletRequest request;

    @Injectable(value = "testId")
    String systemId = "testId";

    @Test
    public void checkCreateMissingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        new Expectations() {{
            request.getAttribute("player.id"); returns(null);
        }};

        try {
            tested.createPlayer(dbEntry);
        } catch ( PlayerAccountModificationException pme ) {
            Assert.assertEquals(Response.Status.FORBIDDEN, pme.getStatus());
        }
    }

    @Test
    public void checkMismatchedId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
        String playerId = "fish";

        PlayerFull dbEntry = new PlayerFull();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");

        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId+"FISH");
        }};

        try {
            tested.createPlayer(dbEntry);
        } catch ( PlayerAccountModificationException pme ) {
            Assert.assertEquals(Response.Status.FORBIDDEN, pme.getStatus());
        }
    }

    @Test(expected=UpdateConflictException.class)
    public void checkAlreadyKnownId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.create(withAny(PlayerFull.class)); result = new UpdateConflictException();
        }};

        tested.createPlayer(dbEntry);
    }

    @Test
    public void checkCreateSystemId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
        String playerId = "fish";

        Player dbEntry = new Player();
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);

        new Expectations() {{
            request.getAttribute("player.id"); returns(systemId);
        }};

        tested.createPlayer(dbEntry);

        new Verifications() {{
            Response.created(URI.create("/players/v1/accounts/" + playerId)); times = 1;
            dbi.create(withAny(PlayerFull.class)); times = 1;
        }};
    }

    @Test
    public void checkGetAllWithSystemId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{

        Player one = new Player();
        one.setName("Kitten");
        one.setFavoriteColor("Tangerine");
        one.setId("one");

        Player two = new Player();
        two.setName("Block");
        two.setFavoriteColor("Tangerine");
        two.setId("two");

        List<Player> players = new ArrayList<Player>();
        players.add(one);
        players.add(two);

        new Expectations() {{
            dbi.queryView((ViewQuery)any,Player.class); returns(players);
        }};
        
        tested.getAllPlayers();

        new Verifications() {{
            GenericEntity<List<Player>> entity;            
            ResponseBuilder b = Response.ok(); times = 1;
            b.entity(entity = withCapture());
            
            List<Player> resultList = (entity.getEntity());
            Assert.assertEquals("Players list is not the expected result", players, resultList);
        }};
        
    }
}
