package net.wasdev.gameon.couchdb.boundary;

import javax.enterprise.inject.Produces;
import org.ektorp.CouchDbInstance;

/**
 *
 * @author airhacks.com
 */
public class CouchIntegrator {

    @Produces
    public CouchDbInstance expose() {
        // changed by Adam Bien -> return fully configured CouchDBInstance
        return null;
    }

}
