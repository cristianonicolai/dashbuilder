package org.dashbuilder.backend;

import java.net.URI;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.errai.security.shared.api.identity.User;
import org.jboss.errai.security.shared.service.AuthenticationService;
import org.uberfire.backend.server.IOWatchServiceAllImpl;
import org.uberfire.backend.server.IOWatchServiceNonDotImpl;
import org.uberfire.commons.cluster.ClusterServiceFactory;
import org.uberfire.commons.services.cdi.Startup;
import org.uberfire.commons.services.cdi.StartupType;
import org.uberfire.io.IOService;
import org.uberfire.io.impl.IOServiceDotFileImpl;
import org.uberfire.io.impl.IOServiceNio2WrapperImpl;
import org.uberfire.io.impl.cluster.IOServiceClusterImpl;
import org.uberfire.java.nio.file.FileSystem;

@Startup(StartupType.BOOTSTRAP)
@ApplicationScoped
public class ApplicationScopedProducer {

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private IOWatchServiceAllImpl watchService;

    @Inject
    @Named("clusterServiceFactory")
    private ClusterServiceFactory clusterServiceFactory;

    private IOService ioService;

    @Inject
    @Named("configIO")
    private IOService configIO;

    private FileSystem systemFS;

    @PostConstruct
    public void setup() {
        if ( clusterServiceFactory == null ) {
            ioService = new IOServiceDotFileImpl(watchService);
        } else {
            ioService = new IOServiceClusterImpl(new IOServiceDotFileImpl(watchService), clusterServiceFactory);
        }
        try {
            systemFS = configIO.getFileSystem( URI.create( "git://system" ));
        }
        catch ( Exception e ){

            systemFS = configIO.newFileSystem( URI.create( "git://system" ),
                                               new HashMap<String, Object>() {{
                                                   put( "init", Boolean.TRUE );
                                                   put( "internal", Boolean.TRUE );
                                               }} );
        }
    }

    @Produces
    @Named("ioStrategy")
    public IOService ioService() {
        return ioService;
    }

    @Produces
    @Named("systemFS")
    public FileSystem systemFS() {
        return systemFS;
    }

    @Produces
    @RequestScoped
    public User getIdentity() {
        return authenticationService.getUser();
    }
}
