package de.adorsys.forge.plugin.gwtplugin;

import java.io.FileNotFoundException;
import java.io.StringWriter;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaSource;
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
			@Option(name = "validation", flagOnly = true) boolean validation,
			@Option(name = "mvp4g", flagOnly = true) boolean mvp4g,
			PipeOut out) {
		if (!project.hasFacet(GWTFacet.class))
	           event.fire(new InstallFacets(GWTFacet.class));
	       else
	           ShellMessages.info(out, "GWT is installed.");
	}

	@Command(value = "create-mvp", help = "creates a mvp package")
	public void createMVP(
			@Option(name = "name", required = true, type = PromptType.JAVA_VARIABLE_NAME)
			String name,
			final PipeOut out) throws FileNotFoundException {
		
		GWTFacet facet = project.getFacet(GWTFacet.class);
		facet.createMVP(name);
	}

	@Command(value = "add-bus-event", help = "creates a mvp package")
	public void addBusEvent(
			@Option(name = "presenter", required = true, type = PromptType.JAVA_CLASS) JavaResource entity,
			final PipeOut out) throws FileNotFoundException {
	}

	@Command(value = "add-bus-event", help = "creates a mvp package")
	public void addView(
			@Option(name = "presenter", required = true, type = PromptType.JAVA_CLASS) JavaResource entity,
			final PipeOut out) throws FileNotFoundException {
	}

	@Command(value = "create-from", help = "Create a new entity service class with CRUD methods for an existing entity bean.")
	public void createFrom(
			@Option(name = "entity", required = true, type = PromptType.JAVA_CLASS) JavaResource entity,
			final PipeOut out) throws FileNotFoundException {

		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);

		JavaSource<?> entitySource = entity.getJavaSource();

		VelocityContext context = new VelocityContext();
		context.put("package", java.getBasePackage() + ".service");
		context.put("entityImport", entitySource.getQualifiedName());
		context.put("entityName", entitySource.getName());
		context.put("cdiName", entitySource.getName().toLowerCase());

		// Service class
		StringWriter writer = new StringWriter();
		Velocity.mergeTemplate("TemplateService.vtl", "UTF-8", context, writer);

		JavaClass serviceClass = JavaParser.parse(JavaClass.class,
				writer.toString());
		java.saveJavaSource(serviceClass);

		pickup.fire(new PickupResource(java.getJavaResource(serviceClass)));

		// ServiceTest class
		StringWriter writerTest = new StringWriter();
		Velocity.mergeTemplate("TemplateServiceTest.vtl", "UTF-8", context,
				writerTest);

		JavaClass serviceTestClass = JavaParser.parse(JavaClass.class,
				writerTest.toString());
		java.saveTestJavaSource(serviceTestClass);

		pickup.fire(new PickupResource(java
				.getTestJavaResource(serviceTestClass)));
	}
}