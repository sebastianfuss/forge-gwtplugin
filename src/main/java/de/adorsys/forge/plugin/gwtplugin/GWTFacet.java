/**
 * 
 */
package de.adorsys.forge.plugin.gwtplugin;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginExecution;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaInterface;
import org.jboss.forge.parser.java.JavaType;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.dependencies.DependencyInstaller;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.events.PickupResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;

/**
 * @author sandro sonntag
 * 
 */
@Alias("gwtfacet")
@RequiresFacet({ MavenCoreFacet.class, JavaSourceFacet.class,
		DependencyFacet.class, WebResourceFacet.class })
public class GWTFacet extends BaseFacet {
	
	static {
		Properties properties = new Properties();
		properties.setProperty("resource.loader", "class");
		properties
				.setProperty("class.resource.loader.class",
						"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

		Velocity.init(properties);
	}

	private final DependencyInstaller installer;
	
	@Inject
	private Event<PickupResource> pickup;

	@Inject
	public GWTFacet(DependencyInstaller installer) {
		super();
		this.installer = installer;
	}
	
	@Override
	public boolean install() {
		installGwtConfiguration();
		installDependencies();
		createGWTMessages();
		createGWTModule();
		createWebResources();
		return true;
	}
	
	public void createBeanValidation() {
		createJavaSource("ValidationMessageResolver.java.ftl");
		createJavaSource("ValidatorFactory.java.ftl");
	}
	
	public void createMVP4G() {
		createJavaSource("EntryPoint.java.ftl");
		createJavaSource("EventBus.java.ftl");
		createJavaSource("GinClientModule.java.ftl");
		createJavaSource("ReverseCompositeView.java.ftl");
		createMVP("application");
	}
	
	
	private void createWebResources() {
		final WebResourceFacet webResource = project.getFacet(WebResourceFacet.class);
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		final JavaSourceFacet javaFacet = project.getFacet(JavaSourceFacet.class);
		
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
		
		Velocity.mergeTemplate("web.xml.ftl", "UTF-8", context, writer);
		FileResource<?> webXml = webResource.createWebResource(writer.toString(), "WEB-INF/web.xml");
		pickup.fire(new PickupResource(webXml));
		
		context = new VelocityContext();
		context.put("basePackage", javaFacet.getBasePackage());
		context.put("description", mvnFacet.getPOM().getName());
		writer = new StringWriter();
		Velocity.mergeTemplate("index.html.ftl", "UTF-8", context, writer);
		FileResource<?> indexHtml = webResource.createWebResource(writer.toString(), "index.html");
		pickup.fire(new PickupResource(indexHtml));
	}

	public void createMVP(String name) {
		HashMap<String, Object> contextData = new HashMap<String, Object>();
		String nameClassPrefix = StringUtils.capitalize(name);
		name = name.toLowerCase();
		contextData.put("nameClassPrefix", nameClassPrefix);
		contextData.put("name", name);
		
		createJavaSource("mvp/PresenterImpl.java.ftl", contextData);
		createJavaSource("mvp/View.java.ftl", contextData);
		createJavaSource("mvp/ViewImpl.java.ftl", contextData);
		createViewXML(name, nameClassPrefix);
	}

	private void installGwtConfiguration() {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		Model pom = mvnFacet.getPOM();

		org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

		plugin.setArtifactId("gwt-maven-plugin");
		plugin.setGroupId("org.codehaus.mojo");
		
		String gwtModule = getGwtModuleName();
		String gwtMessages = getGwtMessages();

		Xpp3Dom dom;
		try {
			dom = Xpp3DomBuilder
					.build(new ByteArrayInputStream(
							("<configuration>"
									+ "<i18nMessagesBundles>"
									+ "			<i18nMessagesBundle>" + gwtMessages + "</i18nMessagesBundle>"
									+ "</i18nMessagesBundles>"
									+ "	<runTarget>index.html</runTarget>"
									+ "	<hostedWebapp>${webappDirectory}</hostedWebapp>"
									+ "<modules>"
									+ "		<module>" + gwtModule + "</module>"
									+ "</modules>"
									+ "</configuration>")
									.getBytes()), "UTF-8");
		} catch (XmlPullParserException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		List<PluginExecution> executions = plugin.getExecutions();
		PluginExecution execution = new PluginExecution();
		execution.addGoal("resources");
		execution.addGoal("i18n");
		execution.addGoal("test");
		execution.addGoal("compile");
		executions.add(execution);

		plugin.setConfiguration(dom);
		pom.getBuild().getPlugins().add(plugin);
		mvnFacet.setPOM(pom);

		pom.getBuild().setOutputDirectory("${webappDirectory}/WEB-INF/classes");
		pom.getProperties().put("webappDirectory", "src/main/webapp");
		mvnFacet.setPOM(pom);
	}

	private String getGwtMessages() {
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		String basePackage = javaSourceFacet.getBasePackage();
		String gwtMessages = basePackage + ".Messages";
		return gwtMessages;
	}
	
	private String getGwtModuleName() {
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		String basePackage = javaSourceFacet.getBasePackage();
		String artifactId = mvnFacet.getMavenProject().getArtifactId();
		String gwtModule = basePackage + "." + artifactId;
		return gwtModule;
	}

	private void installDependencies() {
		DependencyFacet facet = project.getFacet(DependencyFacet.class);
		for (Dependency requirement : getRequiredDependencies()) {
			if (!installer.isInstalled(project, requirement)) {
				if (!facet.hasDirectManagedDependency(requirement)) {
					facet.addDirectManagedDependency(requirement);
				}
				installer.install(project, requirement,
						requirement.getScopeTypeEnum());
			}
		}
	}

	private Collection<Dependency> getRequiredDependencies() {
		Dependency gwtUser = DependencyBuilder
				.create("com.google.gwt:gwt-user:2.4.0:compile:jar");
		Dependency slf4jGwt = DependencyBuilder
				.create("org.jvnet.hudson.main:hudson-gwt-slf4j:2.1.1:runtime:jar");
		Dependency slf4j = DependencyBuilder
				.create("org.slf4j:slf4j-api:1.6.1:compile:jar");
		Dependency jaxRs = DependencyBuilder
				.create("javax.ws.rs:jsr311-api:1.1.1:compile:jar");

		Dependency restyGwt = DependencyBuilder
				.create("org.fusesource.restygwt:restygwt:1.2:compile:jar");
		
		Dependency mvp4g = DependencyBuilder
				.create("com.googlecode.mvp4g:mvp4g:1.4.0:compile:jar");
		mvp4g.getExcludedDependencies().add(DependencyBuilder.create("gwt-servlet:com.google.gwt"));

		Dependency hibernateValidatorSources = DependencyBuilder
				.create("org.hibernate:hibernate-validator:4.2.0.Final:compile:jar:sources");

		Dependency hibernateValidator = DependencyBuilder
				.create("org.hibernate:hibernate-validator:4.2.0.Final:compile:jar");
		return Arrays.asList(gwtUser, slf4j, slf4jGwt, jaxRs, restyGwt,
				hibernateValidatorSources, hibernateValidator, mvp4g);
	}

	@Override
	public boolean isInstalled() {
		boolean dependencysInstalled = isDepsInstalled();
		boolean pluginInstalled = isPluginInstalled();
		boolean isInstalled = dependencysInstalled && pluginInstalled;
		return isInstalled;
	}


	private boolean isPluginInstalled() {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		org.apache.maven.model.Plugin gwtPlugin = new org.apache.maven.model.Plugin();
		gwtPlugin.setArtifactId("gwt-maven-plugin");
		gwtPlugin.setGroupId("org.codehaus.mojo");
		boolean pluginInstalled = mvnFacet.getPOM().getBuild().getPlugins().contains(gwtPlugin);
		return pluginInstalled;
	}


	private boolean isDepsInstalled() {
		DependencyFacet deps = project.getFacet(DependencyFacet.class);
		boolean dependencysInstalled = true;
		for (Dependency requirement : getRequiredDependencies()) {
			if (!deps.hasEffectiveDependency(requirement)) {
				return dependencysInstalled = false;
			}
		}
		return dependencysInstalled;
	}
	
	private void createGWTMessages() {
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		String name = java.getBasePackage();
		
		VelocityContext velocityContext = new VelocityContext();
		StringWriter stringWriter = new StringWriter();
		Velocity.mergeTemplate("Messages.ftl", "UTF-8", velocityContext, stringWriter);
		
		FileResource<?> MessagesResource = resources.createResource(stringWriter.toString().toCharArray(), name.replace('.', '/') + "/Messages.properties");
		
		pickup.fire(new PickupResource(MessagesResource));
	}
	
	private void createViewXML(String name, String classPrefix) {
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		VelocityContext velocityContext = new VelocityContext();
		
		velocityContext.put("basePackage", java.getBasePackage());
		
		StringWriter stringWriter = new StringWriter();
		Velocity.mergeTemplate("mvp/ViewImpl.ui.xml.ftl", "UTF-8", velocityContext, stringWriter);
		
		FileResource<?> messagesResource = resources.createResource(stringWriter.toString().toCharArray(), java.getBasePackage().replace('.', '/') + String.format("/%s/%sViewImpl.ui.xml", name, classPrefix));
		pickup.fire(new PickupResource(messagesResource));
	}
	
	private void createGWTModule() {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		VelocityContext velocityContext = new VelocityContext();
		
		String name = java.getBasePackage();
		String classPrefix = getClassPrefix(mvnFacet);
		velocityContext.put("classPrefix", classPrefix);
		velocityContext.put("basePackage", java.getBasePackage());
		
		StringWriter stringWriter = new StringWriter();
		Velocity.mergeTemplate("Module.gwt.xml.ftl", "UTF-8", velocityContext, stringWriter);
		
		FileResource<?> messagesResource = resources.createResource(stringWriter.toString().toCharArray(),  name.replace('.', '/')  + String.format("/%s.gwt.xml", classPrefix));
		pickup.fire(new PickupResource(messagesResource));
	}
	
	private void createJavaSource(String template) {
		createJavaSource(template, new HashMap<String, Object>());
	}
	
	private void createJavaSource(String template, Map<String, Object> parameter) {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		VelocityContext velocityContext = new VelocityContext(parameter);
		
		String classPrefix = getClassPrefix(mvnFacet);
		velocityContext.put("classPrefix", classPrefix);
		velocityContext.put("basePackage", java.getBasePackage());
		
		
		StringWriter stringWriter = new StringWriter();
		Velocity.mergeTemplate(template, "UTF-8", velocityContext, stringWriter);
		
		JavaType<?> serviceClass = JavaParser.parse(JavaType.class,
				stringWriter.toString());
		try {
			java.saveJavaSource(serviceClass);
			pickup.fire(new PickupResource(java.getJavaResource(serviceClass)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
	}

	private String getClassPrefix(final MavenCoreFacet mvnFacet) {
		String artifactId = mvnFacet.getPOM().getArtifactId();
		return StringUtils.capitalize(artifactId);
	}

	public JavaResource getEventBus() throws FileNotFoundException {
		final JavaSourceFacet javaFacet = project.getFacet(JavaSourceFacet.class);
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		String classPrefix = getClassPrefix(mvnFacet);
		return javaFacet.getJavaResource(javaFacet.getBasePackage() + "." + classPrefix + "EventBus");
	}

}
