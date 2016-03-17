package net.wasdev.gameon.player;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.impl.StdCouchDbConnector;

public class CouchInjector {
    @Resource(lookup="couchdb/connector")
    protected CouchDbInstance dbi;
    
        @Produces
        public CouchDbConnector expose() {            
            CouchDbConnector db = new StdCouchDbConnector("playerdb", dbi); 
            db.createDatabaseIfNotExists();
            return db;              
        }
}
