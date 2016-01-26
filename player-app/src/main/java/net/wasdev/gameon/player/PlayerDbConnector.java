package net.wasdev.gameon.player;

import java.io.IOException;
import java.net.MalformedURLException;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

public class PlayerDbConnector {

    public static CouchDbConnector getConnector() throws IOException{
        try{
            HttpClient httpClient = new StdHttpClient.Builder()
                    .url("http://couchdb:5984")
                    .username("mapUser")
                    .password("myCouchDBSecret")
                    .build();
    
            CouchDbInstance dbInstance = new StdCouchDbInstance(httpClient);
            CouchDbConnector db = new StdCouchDbConnector("playerdb", dbInstance);
    
            db.createDatabaseIfNotExists();
            
            return db;
        }catch(MalformedURLException e){
            throw new IOException(e);
        }
    }
    
}
