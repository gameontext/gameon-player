/*******************************************************************************
 * Copyright (c) 2018 IBM Corp.
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

import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.ektorp.CouchDbConnector;
import org.ektorp.http.HttpClient;
import org.gameontext.player.utils.Log;

@ApplicationScoped
public class CouchDbHealth {

    @Inject
    protected CouchDbConnector db;

    HttpClient client;
    volatile Instant last = null;
    volatile boolean healthCheck = false;


    @PostConstruct
    public void init() {
        client = db.getConnection();
    }

    // doesn't need to be synchronized.
    // If it happens twice, will be close enough
    public boolean isHealthy() {
        if ( client == null ) {
            return false;
        }

        Instant current = Instant.now();
        boolean result = healthCheck;

        if ( last == null || Duration.between(last, current).toMillis() > 30000 ) {
           try {
                client.head("/");
                result = healthCheck = true;
                last = current;
            } catch (Exception e) {
                Log.log(Level.SEVERE, this, "Unable to connect to {0}", getDatabaseName(), e);
                result = healthCheck = false;
            }
        }

        return result;
    }

	public String getDatabaseName() {
		return CouchInjector.DB_NAME;
	}
}
