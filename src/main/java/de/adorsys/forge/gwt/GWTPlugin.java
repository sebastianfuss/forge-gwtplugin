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

import java.io.FileNotFoundException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.Annotation;
import org.jboss.forge.parser.java.Import;
import org.jboss.forge.parser.java.JavaClass;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaSource;
import org.jboss.forge.parser.java.JavaType;
import org.jboss.forge.parser.java.Method;
import org.jboss.forge.parser.java.Parameter;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
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
 * This Plugin supports common gwt commands.
 * @author Sandro Sonntag
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

	private VelocityEngine velocityEngine;
	
	public GWTPlugin() {
		velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", 
		    ClasspathResourceLoader.class.getName());
		velocityEngine.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
			      "org.apache.velocity.runtime.log.JdkLogChute" );
	}

	@SetupCommand
	@Command(value = "setup", help = "Setup a gwt project")
	public void setup(
			@Option(name = "no-bean-validation", flagOnly = true) boolean validation,
			@Option(name = "no-mvp4g", flagOnly = true) boolean mvp4g,
			PipeOut out) throws FileNotFoundException {
		if (!project.hasFacet(GWTFacet.class))
	           event.fire(new InstallFacets(GWTFacet.class));
	       else
	           ShellMessages.info(out, "GWT is installed.");

		GWTFacet gwtFacet = project.getFacet(GWTFacet.class);
		
		if(!validation) {
			gwtFacet.setupBeanValidation();
		}
		
		if(!mvp4g) {
			gwtFacet.setupMVP4G();
		}
		
		ShellMessages.info(out, "Whats next?\n" +
				"    1. run your generated application and type 'gwt run'\n" +
				"    2. open this url in your webbrowser: http://127.0.0.1:8888/index.html?gwt.codesvr=127.0.0.1:9997\n" +
				"    3. extend your application with a new mvp package 'new-mvp contacts'\n" +
				"    4. define new events in " + gwtFacet.getEventBus().getFullyQualifiedName() + " and wire them with 'wire-events' to the presenters\n" +
				"    5. define a 'Contact' model class and generate a widget UI with 'generate-view --edit --list --table'\n" +
				"    6. Publish your app and generate a opensocial gadget with 'setup-gadget'\n");
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
	
	@Command(value = "setup-gadget", help = "make the project to a opensocial gadget project")
	public void addGadget(PipeOut out) {
		GWTFacet gwtFacet = project.getFacet(GWTFacet.class);
		DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);
		MavenCoreFacet mavenFacet = project.getFacet(MavenCoreFacet.class);
		final JavaSourceFacet javaFacet = project.getFacet(JavaSourceFacet.class);
		
		//add the gadgets repo
		gwtFacet.addRepository("gwt-gadgets", "http://gwtquery-plugins.googlecode.com/svn/mavenrepo/");

		//add the gadgets dependencies
		Dependency gwtGadgets = DependencyBuilder
				.create("com.google.gwt.google-apis:gwt-gadgets:1.2.1:compile:jar").setClassifier("2.3.0");
		dependencyFacet.addDirectManagedDependency(gwtGadgets);
		dependencyFacet.addDirectDependency(DependencyBuilder.create(gwtGadgets)
				.setVersion(null));
		
		//generate Gadget
		gwtFacet.createJavaSource("gadget/Gadget.java.vm");
		
		//generate gadget gwt module
		gwtFacet.createResourceAbsolute("gadget/GadgetModule.gwt.xml.vm", gwtFacet.getModuleNameGagdet().replace('.', '/') + ".gwt.xml");
		
		//generate ModulePrefs.txt
		gwtFacet.createResource("gadget/ModulePrefs.txt.vm", "ModulePrefs.txt");
		
		//generate Content.txt
		gwtFacet.createResource("gadget/Content.txt.vm", "Content.txt");

		//set the gadget entry point
		gwtFacet.setGWTModule(gwtFacet.getModuleNameGagdet());
		
		//print where can the gadget.xml could be found
		ShellMessages.success(out, "Gadget support is now configured.");
		try {
			JavaResource javaResource = javaFacet.getJavaResource(gwtFacet.getModuleNameGagdet());
			ShellMessages.info(out, "Please edit your gadget metadata in (" + javaResource.getUnderlyingResourceObject().getAbsolutePath() + ")");
		} catch (FileNotFoundException e) {
		}
		String contextRoot = mavenFacet.getFullProjectBuildingResult().getProject().getBuild().getFinalName();
		
		ShellMessages.info(out, "Where can I find the gadget XML?" +
				String.format("\n    * http://localhost:8080/%s/%s/%s.gadget.xml",
						contextRoot,
						gwtFacet.getModuleNameGagdet(),
						gwtFacet.getEntryPointGadget()) +
				String.format("\n    * %s/target/%s/%s/%s.gadget.xml",
							project.getProjectRoot().getUnderlyingResourceObject().getAbsolutePath(),
							contextRoot,
							gwtFacet.getModuleNameGagdet(),
							gwtFacet.getEntryPointGadget()));
		
		//print how to switch to standalone mode
		ShellMessages.warn(out, "GWT-Developmentmode does not work with gadgets!\n    run 'gwt set-mode STANDALONE' to switch back to standalone version.");
	}
	
	@Command(value = "set-mode", help = "Switch between gadget and standalone mode. This edits the pom for you!")
	public void setMode(@Option(help="mode") GWTMode mode, PipeOut out) {
		GWTFacet gwtFacet = project.getFacet(GWTFacet.class);
		switch (mode) {
			case GADGET:
				gwtFacet.setGWTModule(gwtFacet.getModuleNameGagdet());
				
			case STANDALONE:
				gwtFacet.setGWTModule(gwtFacet.getModuleNameStandalone());
		}
		ShellMessages.success(out, "I set the GWT mode to " + mode + " for you.");
	}
	
	@Command(value = "generate-view", help = "generates a view from a bean")
	public void generateView(
			@Option(required = false) JavaResource[] targets,
			@Option(name="table", description="generates a table widget from the given model type" , flagOnly=true) boolean table,
			@Option(name="edit", description="generates a edit widget from the given model type" , flagOnly=true) boolean edit,
			@Option(name="list", description="generates a list widget from the given model type", flagOnly=true) boolean list,
			final PipeOut out
			) {
		if (targets != null) {
			for (JavaResource javaResource : targets) {
				generateFromModel(list, table, edit, out, javaResource);
			}
		} else if (resource != null) {
			generateFromModel(list, table, edit, out, resource);
		}
	}

	private void generateFromModel(boolean list, boolean table, boolean edit,
			final PipeOut out, JavaResource javaResource) {
		if (list) {
			generateWidget(javaResource, "List", "ListModelViewImpl.java.vm", null, out);
		}
		if (table) {
			generateWidget(javaResource, "Table", "TableModelViewImpl.java.vm", "TableModelViewImpl.ui.xml.vm", out);
		}
		if (edit) {
			generateWidget(javaResource, "", "ModelViewImpl.java.vm", "ModelViewImpl.ui.xml.vm", out);
		}
	}
	
	private void generateWidget(JavaResource resouce, String sufix, String javaTemplate, String uiTemplate, final PipeOut out) {
		try {
			JavaSource<?> javaSource = resouce.getJavaSource();
			if (javaSource instanceof JavaClass) {
				ResourceFacet resources = project.getFacet(ResourceFacet.class);
				JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
				GWTFacet gwtFacet = project.getFacet(GWTFacet.class);

				VelocityContext velocityContext = gwtFacet.createVelocityContext(null);
				
				HashMap<String, String> msgCollector = new HashMap<String, String>();
				velocityContext.put("msgCollector", msgCollector);
				
				StringWriter stringWriter;
				if (uiTemplate != null) {
					stringWriter = new StringWriter();
					velocityEngine.mergeTemplate(uiTemplate, "UTF-8", velocityContext, stringWriter);
					String fqViewName = java.getBasePackage().concat(".widgets.").concat(javaSource.getName()).concat(sufix).concat("Widget");
					resources.createResource(stringWriter.toString().toCharArray(), fqViewName.replace('.', '/') + ".ui.xml");
				}
				
				if (javaTemplate != null) {
					stringWriter = new StringWriter();
					velocityEngine.mergeTemplate(javaTemplate, "UTF-8", velocityContext, stringWriter);
					JavaType<?> serviceClass = JavaParser.parse(JavaType.class,	stringWriter.toString());
					java.saveJavaSource(serviceClass);
				}

				if (!msgCollector.isEmpty()) {
					ShellMessages.info(out, String.format("Collecting %s new messages from UI template", msgCollector.size()));
					gwtFacet.addMessages(msgCollector);
				}
			}
		} catch (FileNotFoundException e) {
			ShellMessages.error(out, "Bean source not found!" + e);
		}
	}

	@Command(value = "new-mvp", help = "creates a mvp package")
	public void createMVP(
			@Option(required = true, type = PromptType.JAVA_VARIABLE_NAME, help="the mvp artifactname that builds the created package")
			String name,
			final PipeOut out) throws FileNotFoundException {
		
		GWTFacet facet = project.getFacet(GWTFacet.class);
		JavaResource presenter = facet.createMVP(name);
		pickup.fire(new PickupResource(presenter));
	}
	
	@Command(value = "wire-events", help = "inspect the eventbus for new event methods")
	public void wireEvents(
			final PipeOut out) throws FileNotFoundException  {
		
		JavaSourceFacet javafacet = project.getFacet(JavaSourceFacet.class);
		
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
		
		List<Method<JavaInterface>> methods = eventBus.getMethods();
		for (Method<JavaInterface> eventMethod : methods) {
			Annotation<JavaInterface> annotation = eventMethod.getAnnotation("com.mvp4g.client.annotation.Event");
			if (annotation != null) {
				String literalValue = annotation.getLiteralValue("handlers");
				if(literalValue == null) {
					continue;
				}
				
				addEventMethod(out, javafacet,
						eventMethod, literalValue);
			}
		}
	}

	private void addEventMethod(final PipeOut out,
			JavaSourceFacet javafacet, Method<JavaInterface> eventMethod,
			String literalValue) throws FileNotFoundException {
		
		String[] presenter = literalValue.replace("{", "").replace("}", "").replaceAll(".class", "").split(",");
		List<Annotation<JavaInterface>> annotations = eventMethod.getAnnotations();
		for (Annotation<JavaInterface> a : annotations) {
			eventMethod.removeAnnotation(a);
		}
		
		String eventName = "on" + StringUtils.capitalize(eventMethod.getName());
		eventMethod.setName(eventName);
		eventMethod.setPublic();
		String method = eventMethod.toString().replace(';', ' ');
		String signature = eventMethod.toSignature().replaceFirst(eventMethod.getName(), eventName);
		
		for (String presenterName : presenter) {
			
			List<Import> imports = eventMethod.getOrigin().getImports();
			for (Import imp : imports) {
				if(imp.getSimpleName().equals(presenterName)) {
					presenterName = imp.getQualifiedName();
					break;
				}
			}
			
			JavaResource presenterResource = javafacet.getJavaResource(presenterName);
			if(!presenterResource.exists()){
				ShellMessages.error(out, String.format("Presenter not found: %s ", presenterName));
				continue;
			}
			
			JavaClass presenterSource = (JavaClass) presenterResource.getJavaSource();
			
			if (!presenterSource.hasMethodSignature(eventMethod)){
				presenterSource.addMethod(method + "{\n}");
				List<Parameter> parameters = eventMethod.getParameters();
				for (Parameter parameter : parameters) {
					presenterSource.addImport(parameter.getTypeInspector().getQualifiedName());
				}
				
				ShellMessages.info(out, String.format(" - %s : created event method %s", presenterSource.getName(), signature));
				javafacet.saveJavaSource(presenterSource);
			}
		}
	}
	
	@Command("run")
	public void run(final PipeOut out, String... a){
		executeCommand(out, "gwt:run", a);
	}
	
	@Command("debug")
	public void debug(final PipeOut out, String... a){
		executeCommand(out, "gwt:debug", a);
	}

	private void executeCommand(final PipeOut out, String command, String... a) {
		final ArrayList<String> args = new ArrayList<String>();
		args.add(command);
		if (a != null) {
			args.addAll(Arrays.asList(a));
		}
		final MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
		maven.executeMaven(out, args.toArray(new String[args.size()]));
	}

}