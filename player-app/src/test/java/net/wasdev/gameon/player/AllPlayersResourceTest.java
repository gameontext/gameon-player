package net.wasdev.gameon.player;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewQuery;
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
    @Injectable String systemId;
    
    @Test
    /** this is one we might chose to disable except for system id */
    public void checkCreateWithApiKey(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException {   
        String playerId = "fish";
        
        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.contains(playerId); returns(false);
        }}; 
        
        @SuppressWarnings("unused")
        Response result = tested.createPlayer(dbEntry);
    
        new Verifications() {{
            Response.status(201); times = 1;
            dbi.create(dbEntry); times = 1;
         }};
    }
    
    @Test
    public void checkCreateMissingId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{   
        String playerId = "fish";
        
        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(null);
        }}; 
        

        @SuppressWarnings("unused")
        Response result = tested.createPlayer(dbEntry);
        
        new Verifications() {{
            Response.status(403); times = 1;
            dbi.create(dbEntry); times = 0;
         }};
        
    }
    
    @Test
    public void checkMismatchedId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{   
        String playerId = "fish";
        
        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId+"FISH");
        }}; 
        

        @SuppressWarnings("unused")
        Response result = tested.createPlayer(dbEntry);
        
        new Verifications() {{
            Response.status(403); times = 1;
            dbi.create(dbEntry); times = 0;
         }};
    }
    
    @Test
    public void checkAlreadyKnownId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{   
        String playerId = "fish";
        
        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.contains(playerId); returns(true);
        }}; 
        
        @SuppressWarnings("unused")
        Response result = tested.createPlayer(dbEntry);
    
        new Verifications() {{
            Response.status(409); times = 1;
            dbi.create(dbEntry); times = 0;
         }};
    }
    
    @Test
    public void checkCreateSystemId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{    
        String playerId = "fish";
        
        Player dbEntry = new Player();
        dbEntry.setApiKey("ShinyShoes");
        dbEntry.setName("Kitten");
        dbEntry.setFavoriteColor("Tangerine");
        dbEntry.setId(playerId);
        dbEntry.setLocation("Earth");
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(systemId);
            dbi.contains(playerId); returns(false);
        }}; 
        
        @SuppressWarnings("unused")
        Response result = tested.createPlayer(dbEntry);
    
        new Verifications() {{
            Response.status(201); times = 1;
            dbi.create(dbEntry); times = 1;
         }};
    }
      
    @Test 
    public void checkGetAllWithId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
        
        Player one = new Player();
        one.setApiKey("ShinyShoes");
        one.setName("Kitten");
        one.setFavoriteColor("Tangerine");
        one.setId("one");
        one.setLocation("Earth");
        
        Player two = new Player();
        two.setApiKey("Patent");
        two.setName("Block");
        two.setFavoriteColor("Tangerine");
        two.setId("two");
        two.setLocation("Earth");
             
        List<Player> players = new ArrayList<Player>();
        players.add(one);
        players.add(two);
        
        String playerId = "one";
        new Expectations() {{
            request.getAttribute("player.id"); returns(playerId);
            dbi.queryView((ViewQuery)any,Player.class); returns(players);
        }};
                
        List<Player> result = tested.getAllPlayers();   
                               
        assertEquals("Players list is not the expected result", players, result);
        assertEquals("Player two api key should have been censored",result.get(1).getApiKey(),"ACCESS_DENIED");   
    }
    
    @Test 
    public void checkGetAllWithSystemId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
            
        Player one = new Player();
        one.setApiKey("ShinyShoes");
        one.setName("Kitten");
        one.setFavoriteColor("Tangerine");
        one.setId("one");
        one.setLocation("Earth");
        
        Player two = new Player();
        two.setApiKey("Patent");
        two.setName("Block");
        two.setFavoriteColor("Tangerine");
        two.setId("two");
        two.setLocation("Earth");
             
        List<Player> players = new ArrayList<Player>();
        players.add(one);
        players.add(two);
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(systemId);
            dbi.queryView((ViewQuery)any,Player.class); returns(players);
        }};
                
        List<Player> result = tested.getAllPlayers();   
                               
        assertEquals("Players list is not the expected result", players, result);
        assertEquals("Player two api key should not have been censored",result.get(1).getApiKey(),"Patent"); 
     
    }
    
    @Test 
    public void checkGetAllWithoutId(@Mocked Response response, @Mocked ResponseBuilder builder) throws IOException{
            
        Player one = new Player();
        one.setApiKey("ShinyShoes");
        one.setName("Kitten");
        one.setFavoriteColor("Tangerine");
        one.setId("one");
        one.setLocation("Earth");
        
        Player two = new Player();
        two.setApiKey("Patent");
        two.setName("Block");
        two.setFavoriteColor("Tangerine");
        two.setId("two");
        two.setLocation("Earth");
             
        List<Player> players = new ArrayList<Player>();
        players.add(one);
        players.add(two);
        
        new Expectations() {{
            request.getAttribute("player.id"); returns(null);
            dbi.queryView((ViewQuery)any,Player.class); returns(players);
        }};
                
        List<Player> result = tested.getAllPlayers();   
                               
        assertEquals("Players list is not the expected result", players, result);
        assertEquals("Player two api key should have been censored",result.get(1).getApiKey(),"ACCESS_DENIED"); 
        assertEquals("Player one api key should have been censored",result.get(0).getApiKey(),"ACCESS_DENIED"); 
    }
 
}