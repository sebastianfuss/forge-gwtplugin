/**
 * Copyright (C) 2012 Sandro Sonntag sso@adorsys.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.adorsys.forge.gwt;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyResolver;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
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
		getShell().execute("mvn install");
	}

	@Test
	public void testSetupValidation() throws Exception {
		Project p = initializeProject(PackagingType.WAR);
		queueInputLines("y");
		getShell().execute("gwt setup --no-mvp4g");
		getShell().execute("mvn install");
	}

	@Test
	public void testSetupMvp4g() throws Exception {
		Project p = initializeProject(PackagingType.WAR);
		queueInputLines("y");
		getShell().execute("gwt setup --no-bean-validation");
		getShell().execute("mvn install");
	}

	@Test
	public void testCreateMVP() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup");
		getShell().execute("gwt new-mvp foobar");
		getShell().execute("mvn install");
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
		JavaInterface eventBus = (JavaInterface) eventBusResource
				.getJavaSource();

		Method<JavaInterface> addMethod = eventBus
				.addMethod("void myEventMethod(String param)");
		addMethod.addAnnotation("com.mvp4g.client.annotation.Event")
				.setLiteralValue("handlers",
						"de.adorsys.foo.foobar.FoobarPresenterImpl.class");
		javaFacet.saveJavaSource(eventBus);
		getShell().execute("gwt wire-events");
		//dup check
		getShell().execute("gwt wire-events");
		getShell().execute("mvn install");
	}

	@Test
	@Ignore
	public void testGwtRun() throws Exception {
		Project p = initializeProject(PackagingType.WAR);

		queueInputLines("y");

		getShell().execute("gwt setup");
		getShell().execute("gwt run");

	}

	protected Project initializeProject(final PackagingType type)
			throws Exception {
		getShell().setCurrentResource(createTempFolder());
		queueInputLines("");
		getShell().execute(
				"new-project --named adorsys.foo --topLevelPackage de.adorsys.foo --type "
						+ type.toString());
		return getProject();
	}
}