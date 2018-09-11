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
      if ( db != null && db.getDbInfo() != null ) {
          return HealthCheckResponse.named(PlayersResource.class.getSimpleName())
                                    .withData(db.getDatabaseName(), "available").up().build();
      }
      System.out.println("db = " + db);
      return HealthCheckResponse.named(PlayersResource.class.getSimpleName())
                                .withData(db.getDatabaseName(), "down").down()
                                .build();
    }
}
