package de.adorsys.forge.gwt;

import java.io.FileNotFoundException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import de.adorsys.forge.gwt.GWTPlugin;

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
		getShell().execute("gwt setup --no-mvp4g");
	}
	
	@Test
	public void testSetupMvp4g() throws Exception {
		Project p = initializeProject(PackagingType.WAR);
		queueInputLines("y");
		getShell().execute("gwt setup --no-bean-validation");
	}

	
	@Test
	public void testCreateMVP() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup");
		getShell().execute("gwt create-mvp foobar");
	}
	
	
	@Test
	public void testWireEvents() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup");
		getShell().execute("gwt new-mvp foobar");
		
		GWTFacet gwtFacet = p.getFacet(GWTFacet.class);
		JavaSourceFacet javaFacet = p.getFacet(JavaSourceFacet.class);
		JavaResource eventBusResource = gwtFacet.getEventBus();
		JavaInterface eventBus = (JavaInterface) eventBusResource.getJavaSource();
		
		Method<JavaInterface> addMethod = eventBus.addMethod("void myEventMethod(String param)");
		addMethod.addAnnotation("com.mvp4g.client.annotation.Event").setLiteralValue("handlers", "com.test.foobar.FoobarPresenterImpl.class");
		javaFacet.saveJavaSource(eventBus);
		getShell().execute("gwt wire-events");
		getShell().execute("gwt wire-events");
	}
	
	@Test
	@Ignore
	public void testGwtRun() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup");
		getShell().execute("gwt run");

		
	}
}