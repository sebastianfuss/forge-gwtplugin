package de.adorsys.forge.gwt;

import java.io.FileNotFoundException;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.events.InstallFacets;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.PromptType;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellMessages;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.Command;
import org.jboss.forge.shell.plugins.Current;
import org.jboss.forge.shell.plugins.Help;
import org.jboss.forge.shell.plugins.Option;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 * @author sandro sonntag
 */
@Alias("gwt")
@RequiresFacet(GWTFacet.class)
@Help("A plugin that helps to build gwt interfaces.")
public class GWTPlugin implements Plugin {

	@Inject
	private Event<InstallFacets> event;

	@Inject
	private Project project;

	@Inject
	private Event<PickupResource> pickup;

	@Inject
	@Current
	private JavaResource resource;

	@Inject
	private Shell shell;

	@SetupCommand
	@Command(value = "setup", help = "Setup a gwt project")
	public void setup(
			@Option(name = "no-bean-validation", flagOnly = true) boolean validation,
			@Option(name = "no-mvp4g", flagOnly = true) boolean mvp4g,
			PipeOut out) {
		if (!project.hasFacet(GWTFacet.class))
	           event.fire(new InstallFacets(GWTFacet.class));
	       else
	           ShellMessages.info(out, "GWT is installed.");

		GWTFacet facet = project.getFacet(GWTFacet.class);
		
		if(!validation) {
			facet.setupBeanValidation();
		}
		
		if(!mvp4g) {
			facet.setupMVP4G();
		}
	}
	
	@Command(value = "setup-beanvalidation", help = "add beanvalidation to the gwt project")
	public void addBeanValidation() {
		GWTFacet facet = project.getFacet(GWTFacet.class);
		facet.setupBeanValidation();
	}
	
	@Command(value = "setup-mvp4g", help = "add mvp4g to the gwt project")
	public void addMvp4g() {
		GWTFacet facet = project.getFacet(GWTFacet.class);
		facet.setupMVP4G();
	}

	@Command(value = "create-mvp", help = "creates a mvp package")
	public void createMVP(
			@Option(required = true, type = PromptType.JAVA_VARIABLE_NAME, help="the mvp artifactname that builds the created package")
			String name,
			final PipeOut out) throws FileNotFoundException {
		
		GWTFacet facet = project.getFacet(GWTFacet.class);
		JavaResource presenter = facet.createMVP(name);
		pickup.fire(new PickupResource(presenter));
	}
	
	@Command(value = "add-event", help = "creates a mvp package")
	public void addBusEvent(
			@Option(required = true, type = PromptType.JAVA_VARIABLE_NAME, help="name of the event") String name,
			final PipeOut out) throws FileNotFoundException  {
		
		GWTFacet facet = project.getFacet(GWTFacet.class);
		JavaResource eventBusResource;
		JavaInterface eventBus;
		try {
			eventBusResource = facet.getEventBus();
			eventBus = (JavaInterface) eventBusResource.getJavaSource();
		} catch (FileNotFoundException e) {
			ShellMessages.error(out, "EventBus source not found!" + e);
			return;
		}
		
		JavaClass presenterJavaSource;
		try {
			JavaSource<?> ps = resource.getJavaSource();
			if (!ps.isClass()) {
				ShellMessages.error(out, "Presenter is not a class!");
				return;
			}
			presenterJavaSource = (JavaClass) ps;
		} catch (FileNotFoundException e) {
			ShellMessages.error(out, "Presenter source not found!");
			return;
		}
		
		Method<JavaInterface> eventMethod = eventBus.addMethod("void " + name + "();");
		eventMethod.addAnnotation("com.mvp4g.client.annotation.Event").setLiteralValue("handlers", "{" + presenterJavaSource.getQualifiedName() + ".class}");
		presenterJavaSource.addMethod("public void on" + StringUtils.capitalize(name) + "() {\n};");
		
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		java.saveJavaSource(eventBus);
		java.saveJavaSource(presenterJavaSource);
		
	}

}