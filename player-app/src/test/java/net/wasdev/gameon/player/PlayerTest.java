package net.wasdev.gameon.player;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PlayerTest {
    @Test
    public void testApiKeyGeneration(){
        Player p = new Player();
        String before = p.getApiKey();
        p.generateApiKey();
        String after1 = p.getApiKey();
        p.generateApiKey();
        String after2 = p.getApiKey();
        
        assertNull("Newly built player object should have no api key",before);
        assertNotNull("Apikey should not be null after generation",after1);
        assertFalse("Apikey should have changed if generated twice before:"+after1+" after:"+after2,after1.equals(after2));
    }
}
