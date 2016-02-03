package net.wasdev.gameon.configuration.boundary;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author airhacks.com
 */
@ApplicationScoped
public class Configurator {

    // changed by Adam Bien -> look at JC2 for more inspiration: https://github.com/AdamBien/jc2
    @Produces
    public String configure(InjectionPoint ip) {
        String key = ip.getMember().getName();
        return resolve(key);
    }

    String resolve(String key) {
        return System.getProperty(key, "-empty-");
    }

}
