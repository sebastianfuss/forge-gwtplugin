package de.adorsys.forge.plugin.gwtplugin;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

public class GWTPluginTest extends AbstractShellTest {
	@Inject
	private DependencyResolver resolver;

	@Deployment
	public static JavaArchive getDeployment() {
		return AbstractShellTest.getDeployment().addPackages(true,
				GWTPlugin.class.getPackage());
	}

	@Test
	public void testSetup() throws Exception {
		// Create a new barebones Java project
		Project p = initializeProject(PackagingType.WAR);

		// Queue input lines to be read as the Shell executes.
		queueInputLines("y");

		// Execute a command. If any input is required, it will be read from
		// queued input.
		getShell().execute("gwt setup");
		Assert.assertNotNull(null);
	}
}