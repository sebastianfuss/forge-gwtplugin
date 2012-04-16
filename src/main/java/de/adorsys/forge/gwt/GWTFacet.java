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
/**
 * 
 */
package de.adorsys.forge.gwt;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Repository;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.parser.JavaParser;
import org.jboss.forge.parser.java.JavaType;
import org.jboss.forge.project.dependencies.Dependency;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.facets.BaseFacet;
import org.jboss.forge.project.facets.DependencyFacet;
import org.jboss.forge.project.facets.JavaSourceFacet;
import org.jboss.forge.project.facets.ResourceFacet;
import org.jboss.forge.project.facets.WebResourceFacet;
import org.jboss.forge.resources.FileResource;
import org.jboss.forge.resources.java.JavaResource;
import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.RequiresFacet;

/**
 * This is the GWT project facet. 
 * @author sandro sonntag
 */
@Alias("gwtfacet")
@RequiresFacet({ MavenCoreFacet.class, JavaSourceFacet.class,
		DependencyFacet.class, WebResourceFacet.class })
public class GWTFacet extends BaseFacet {
	
	private final VelocityEngine velocityEngine;
	
	public GWTFacet() {
		super();
		velocityEngine = new VelocityEngine();
		velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		velocityEngine.setProperty("classpath.resource.loader.class", 
		    ClasspathResourceLoader.class.getName());
		velocityEngine.setProperty( RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
			      "org.apache.velocity.runtime.log.JdkLogChute" );
		
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
	
	public void setupBeanValidation() {
		createJavaSource("ValidationMessageResolver.java.vm");
		createJavaSource("ValidatorFactory.java.vm");
		createJavaSource("SampleModelClass.java.vm");
	}
	
	public void setupMVP4G() {
		createJavaSource("EntryPoint.java.vm");
		createJavaSource("EventBus.java.vm");
		createJavaSource("GinClientModule.java.vm");
		createJavaSource("ReverseCompositeView.java.vm");
		createMVP("application");
	}
	
	
	private void createWebResources() {
		final WebResourceFacet webResource = project.getFacet(WebResourceFacet.class);
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		final JavaSourceFacet javaFacet = project.getFacet(JavaSourceFacet.class);
		
		StringWriter writer = new StringWriter();
		VelocityContext context = new VelocityContext();
		
		velocityEngine.mergeTemplate("web.xml.vm", "UTF-8", context, writer);
		webResource.createWebResource(writer.toString(), "WEB-INF/web.xml");

		context = new VelocityContext();
		context.put("basePackage", javaFacet.getBasePackage());
		context.put("description", mvnFacet.getPOM().getName());
		writer = new StringWriter();
		velocityEngine.mergeTemplate("index.html.vm", "UTF-8", context, writer);
		webResource.createWebResource(writer.toString(), "index.html");
	}

	public JavaResource createMVP(String name) {
		HashMap<String, Object> contextData = new HashMap<String, Object>();
		String nameClassPrefix = StringUtils.capitalize(name);
		name = name.toLowerCase();
		contextData.put("nameClassPrefix", nameClassPrefix);
		contextData.put("name", name);
		
		JavaResource presenter = createJavaSource("mvp/PresenterImpl.java.vm", contextData);
		createJavaSource("mvp/View.java.vm", contextData);
		createJavaSource("mvp/ViewImpl.java.vm", contextData);
		createViewXML(name, nameClassPrefix);
		return presenter;
	}

	private void installGwtConfiguration() {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		Model pom = mvnFacet.getPOM();

		org.apache.maven.model.Plugin plugin = new org.apache.maven.model.Plugin();

		plugin.setArtifactId("gwt-maven-plugin");
		plugin.setGroupId("org.codehaus.mojo");
		
		String gwtModule = getModuleName();
		String gwtMessages = getMessagesQualified();

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
		Repository mvp4gRepo = new Repository();
		mvp4gRepo.setId("mvp4g");
		mvp4gRepo.setUrl("http://mvp4g.googlecode.com/svn/maven2/releases");
		pom.getRepositories().add(mvp4gRepo);
		mvnFacet.setPOM(pom);
		pom.getBuild().setOutputDirectory("${webappDirectory}/WEB-INF/classes");
		pom.getProperties().put("webappDirectory", "src/main/webapp");
		mvnFacet.setPOM(pom);
	}

	public String getMessagesQualified() {
		final JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
		String basePackage = javaSourceFacet.getBasePackage();
		String gwtMessages = basePackage + ".Messages";
		return gwtMessages;
	}
	
	public String getModuleName() {
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
//			if (!installer.isInstalled(project, requirement)) {
//				if (!facet.hasDirectManagedDependency(requirement)) {
					facet.addDirectManagedDependency(requirement);
					facet.addDirectDependency(DependencyBuilder.create(requirement).setVersion(null));
//				}
				System.out.println(requirement);
//			}
		}
	}

	private Collection<Dependency> getRequiredDependencies() {
		Dependency gwtUser = DependencyBuilder
				.create("com.google.gwt:gwt-user:2.4.0:compile:jar");
		Dependency slf4jGwt = DependencyBuilder
				.create("org.jvnet.hudson.main:hudson-gwt-slf4j:2.1.1:compile:jar");
		Dependency slf4j = DependencyBuilder
				.create("org.slf4j:slf4j-api:1.6.1:compile:jar");
		Dependency jaxRs = DependencyBuilder
				.create("javax.ws.rs:jsr311-api:1.1.1:compile:jar");

		Dependency restyGwt = DependencyBuilder
				.create("org.fusesource.restygwt:restygwt:1.2:compile:jar");
		
		Dependency mvp4g = DependencyBuilder
				.create("com.googlecode.mvp4g:mvp4g:1.4.0:compile:jar");
		mvp4g.getExcludedDependencies().add(DependencyBuilder.create("com.google.gwt:gwt-servlet"));

		Dependency hibernateValidatorSources = DependencyBuilder
				.create("org.hibernate:hibernate-validator:4.2.0.Final:compile:jar").setClassifier("sources");

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
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		VelocityContext velocityContext = new VelocityContext();
		StringWriter stringWriter = new StringWriter();
		velocityEngine.mergeTemplate("Messages.vm", "UTF-8", velocityContext, stringWriter);
		resources.createResource(stringWriter.toString().toCharArray(),getMessagePropertiesPath());
	}

	public String getMessagePropertiesPath() {
		return getMessagesQualified().replace('.', '/') + ".properties";
	}
	
	public void addMessages(Map<String, String> messages){
		Properties properties = new Properties();
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		MavenCoreFacet maven = project.getFacet(MavenCoreFacet.class);
		FileResource<?> resource = resources.getResource(getMessagePropertiesPath());
		InputStream is = resource.getResourceInputStream();
		FileOutputStream fileOutputStream = null;
		try {
			//prefer the user messages
			properties.putAll(messages);
			properties.load(new InputStreamReader(is, "UTF-8"));
			is.close();
			fileOutputStream = new FileOutputStream(resource.getUnderlyingResourceObject());
			properties.store(new OutputStreamWriter(fileOutputStream, "UTF-8"), null);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				is.close();
				fileOutputStream.close();
			} catch (IOException e) {
			}
		}
		
		maven.executeMaven(Arrays.asList("generate-resources"));
		
	}
	
	private void createViewXML(String name, String classPrefix) {
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		VelocityContext velocityContext = new VelocityContext();
		
		velocityContext.put("basePackage", java.getBasePackage());
		
		StringWriter stringWriter = new StringWriter();
		velocityEngine.mergeTemplate("mvp/ViewImpl.ui.xml.vm", "UTF-8", velocityContext, stringWriter);
		resources.createResource(stringWriter.toString().toCharArray(), java.getBasePackage().replace('.', '/') + String.format("/%s/%sViewImpl.ui.xml", name, classPrefix));
	}
	
	private void createGWTModule() {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		ResourceFacet resources = project.getFacet(ResourceFacet.class);
		VelocityContext velocityContext = new VelocityContext();
		
		String name = java.getBasePackage();
		velocityContext.put("classPrefix", getClassPrefix(mvnFacet));
		velocityContext.put("basePackage", java.getBasePackage());
		
		StringWriter stringWriter = new StringWriter();
		velocityEngine.mergeTemplate("Module.gwt.xml.vm", "UTF-8", velocityContext, stringWriter);
		resources.createResource(stringWriter.toString().toCharArray(),  name.replace('.', '/')  + String.format("/%s.gwt.xml", mvnFacet.getPOM().getArtifactId()));
	}
	
	private void createJavaSource(String template) {
		createJavaSource(template, new HashMap<String, Object>());
	}
	
	private JavaResource createJavaSource(String template, Map<String, Object> parameter) {
		final MavenCoreFacet mvnFacet = project.getFacet(MavenCoreFacet.class);
		JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
		VelocityContext velocityContext = new VelocityContext(parameter);
		
		String classPrefix = getClassPrefix(mvnFacet);
		velocityContext.put("classPrefix", classPrefix);
		velocityContext.put("basePackage", java.getBasePackage());
		
		
		StringWriter stringWriter = new StringWriter();
		velocityEngine.mergeTemplate(template, "UTF-8", velocityContext, stringWriter);
		
		JavaType<?> serviceClass = JavaParser.parse(JavaType.class,
				stringWriter.toString());
		try {
			JavaResource saveJavaSource = java.saveJavaSource(serviceClass);
			return saveJavaSource;
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
