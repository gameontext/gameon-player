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
package org.gameon.player.entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PlayerTest {
    @Test
    public void testApiKeyGeneration(){
        PlayerDbRecord p = new PlayerDbRecord();
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
