package com.github.randomcodeorg.netmaven.netmaven.nuget;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultRepositoryConnectorProvider;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

import com.github.randomcodeorg.netmaven.netmaven.PlexusRedirectLogger;

@Component(role = RepositoryConnectorFactory.class, hint = "nuget")
public class NugetRepositoryConnectorFactory implements RepositoryConnectorFactory, Service {

	private final DefaultRepositoryConnectorProvider provider = new DefaultRepositoryConnectorProvider();
	
	@Requirement
	private Logger logger;
	
	public NugetRepositoryConnectorFactory() {
		
	}

	@Override
	public void initService(ServiceLocator locator) {
	}

	@Override
	public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
			throws NoRepositoryConnectorException {
		if("nuget".equalsIgnoreCase(repository.getContentType())){
			return new NugetRepositoryConnector2(repository, PlexusRedirectLogger.named(logger, "NugetConnector"));
		}
		return provider.newRepositoryConnector(session, repository);
	}

	@Override
	public float getPriority() {
		return 0;
	}

}
