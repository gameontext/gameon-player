package org.gameontext.player;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.ektorp.CouchDbConnector;


@Health
@ApplicationScoped
public class PlayerHealth implements HealthCheck {

    @Inject
    protected CouchDbConnector db;

    @Override
    public HealthCheckResponse call() {
      if ( isHealthy()) {
          return HealthCheckResponse.named(PlayersResource.class.getSimpleName())
                                    .withData(db.getDatabaseName(), "available").up().build();
      }
      System.out.println("db = " + db);
      return HealthCheckResponse.named(PlayersResource.class.getSimpleName())
                                .withData(db.getDatabaseName(), "down").down()
                                .build();
    }
    
    public boolean isHealthy() {
    try{
        return db.getConnection() != null && db.getDbInfo() != null;
    }
    catch(Exception e){
        return false;
    }
    }
}
