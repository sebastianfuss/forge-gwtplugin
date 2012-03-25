package de.adorsys.forge.plugin.gwtplugin;

import java.io.FileNotFoundException;
import java.io.StringWriter;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.forge.parser.JavaParser;
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
			@Option(name = "beanValidation", flagOnly = true) boolean validation,
			@Option(name = "mvp4g", flagOnly = true) boolean mvp4g,
			PipeOut out) {
		if (!project.hasFacet(GWTFacet.class))
	           event.fire(new InstallFacets(GWTFacet.class));
	       else
	           ShellMessages.info(out, "GWT is installed.");

		GWTFacet facet = project.getFacet(GWTFacet.class);
		
		if(validation) {
			facet.createBeanValidation();
		}
		
		if(mvp4g) {
			facet.createMVP4G();
		}
	}

	@Command(value = "create-mvp", help = "creates a mvp package")
	public void createMVP(
			@Option(name = "name", required = true, type = PromptType.JAVA_VARIABLE_NAME)
			String name,
			final PipeOut out) throws FileNotFoundException {
		
		GWTFacet facet = project.getFacet(GWTFacet.class);
		facet.createMVP(name);
	}

	@Command(value = "add-event", help = "creates a mvp package")
	public void addBusEvent(
			@Option(name = "presenter", required = true, type = PromptType.JAVA_CLASS) JavaResource presenter,
			@Option(name = "name", required = true, type = PromptType.JAVA_VARIABLE_NAME)
			String name,
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
			JavaSource<?> ps = presenter.getJavaSource();
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
		eventMethod.addAnnotation("com.mvp4g.client.annotation.Event").setLiteralValue("handlers", "{" + presenterJavaSource.getQualifiedName() + "}");
		presenterJavaSource.addMethod("public void on" + StringUtils.capitalize(name) + "();");
		
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		java.saveJavaSource(eventBus);
		java.saveJavaSource(presenterJavaSource);
		
		pickup.fire(new PickupResource(eventBusResource));
		pickup.fire(new PickupResource(presenter));
	}

}