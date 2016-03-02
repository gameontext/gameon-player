package net.wasdev.gameon.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.ektorp.CouchDbConnector;
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

    
    
}
