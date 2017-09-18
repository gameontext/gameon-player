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

import static org.junit.Assert.fail;

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
import org.gameontext.player.control.PlayerAccountModificationException;
import org.gameontext.player.entity.PlayerArgument;
import org.gameontext.player.entity.PlayerDbRecord;
import org.gameontext.player.entity.PlayerResponse;
import org.junit.Assert;
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
public class AllPlayersResourceTest {

    @Tested AllPlayersResource tested;
    @Injectable CouchDbConnector dbi;
    @Injectable HttpServletRequest request;
    @Injectable Kafka kafka;

    @Injectable(value = "testId")
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
    public void checkCreateMissingId() throws IOException{

        new Expectations() {{
            request.getAttribute("player.id"); returns(null);
        }};

        try {
            tested.createPlayer(playerArg);
            fail("Expected account modification exception");
        } catch ( PlayerAccountModificationException pme ) {
            Assert.assertEquals(Response.Status.FORBIDDEN, pme.getStatus());
        }
    }

    @Test
    public void checkMismatchedId() throws IOException{

        new Expectations() {{
            request.getAttribute("player.id"); returns(playerArg.getId()+"FISH");
        }};

        try {
            tested.createPlayer(playerArg);
            fail("Expected account modification exception");
        } catch ( PlayerAccountModificationException pme ) {
            Assert.assertEquals(Response.Status.FORBIDDEN, pme.getStatus());
        }
    }

    @Test(expected=UpdateConflictException.class)
    public void checkAlreadyKnownId() throws IOException{
        Claims claims = Jwts.claims();
        claims.put("email","example@test.test");
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerArg.getId());
            request.getAttribute("player.claims"); returns(claims);
            dbi.create(withAny(PlayerDbRecord.class)); result = new UpdateConflictException();
        }};

        tested.createPlayer(playerArg);
    }

    @Test
    public void checkCreateSystemId(@Mocked Response response) throws IOException{
        Claims claims = Jwts.claims();
        claims.put("email","example@test.test");
        new Expectations() {{
            request.getAttribute("player.id"); returns(systemId);
            request.getAttribute("player.claims"); returns(claims);            
        }};

        tested.createPlayer(playerArg);

        new Verifications() {{
            Response.created(URI.create("/players/v1/accounts/" + playerArg.getId())); times = 1;
            dbi.create(withAny(PlayerDbRecord.class)); times = 1;
        }};
    }

    @Test
    public void checkGetAllWithSystemId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{

        PlayerDbRecord another = new PlayerDbRecord();
        another.setName("Kitten");
        another.setFavoriteColor("Tangerine");
        another.setId("one");
        another.setLocation("Earth");
        another.setRev("343");

        List<PlayerDbRecord> players = new ArrayList<PlayerDbRecord>();
        players.add(playerDb);
        players.add(another);

        new Expectations() {{
            dbi.queryView((ViewQuery)any,PlayerDbRecord.class); returns(players);
        }};

        tested.getAllPlayers();

        new Verifications() {{
            GenericEntity<List<PlayerResponse>> entity;
            ResponseBuilder b = Response.ok(); times = 1;
            b.entity(entity = withCapture());

            List<PlayerResponse> resultList = (entity.getEntity());
            Assert.assertEquals("Players list is not the expected size", players.size(), resultList.size());
            //TODO: verify
        }};

    }
}
