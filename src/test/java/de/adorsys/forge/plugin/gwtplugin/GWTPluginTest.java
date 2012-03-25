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
		Project p = initializeProject(PackagingType.WAR);
		queueInputLines("y");
		getShell().execute("gwt setup");
	}
	
	@Test
	public void testSetupValidation() throws Exception {
		Project p = initializeProject(PackagingType.WAR);
		queueInputLines("y");
		getShell().execute("gwt setup --beanValidation");
	}
	
	@Test
	public void testSetupMvp4g() throws Exception {
		Project p = initializeProject(PackagingType.WAR);
		queueInputLines("y");
		getShell().execute("gwt setup --mvp4g");
	}

	
	@Test
	public void testCreateMVP() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup --mvp4g");
		getShell().execute("gwt create-mvp --name foobar");
	}
	
	
	@Test
	public void testAddEvent() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup --mvp4g");
		getShell().execute("gwt create-mvp --name foobar");
		getShell().execute("gwt add-event --name goToFoobar --presenter com.test.foobar.FoobarPresenterImpl");
	}
}