This Forge-Plugin generates a complete GWT-Project  based on MVP with UIBinder, Eventbus, Validation, i18n and RESTful webservices access thanks the following components:

* MVP4G
* GIN
* RestyGWT

How to install:
===============
  
* Checkout my Forge bugfix fork (will be removed in the future) `git clone https://xandrox@github.com/xandrox/core.git`
* build it `cd core` and `mvn install`
* move dist to your forge installation folder `mv dist/target/forge-distribution-1.0.3-SNAPSHOT ~/forge`
* `git clone https://xandrox@github.com/xandrox/forge-gwtplugin.git`
* `cd forge-gwtplugin`
* run 'forge'
* install the gwt plugin

	`[forge-gwt] gwtplugin $ forge source-plugin 
 	? [project directory (of type org.jboss.forge.resources.Resource)]: .`

How to start:
=============

* Create your project

	[no project] adorsys $ new-project --named gwtmvp --topLevelPackage de.adorsys.gwtmvp --type war
	 ? Use [/Users/sso/Documents/dev/adorsys/gwtmvp] as project directory? [Y/n] Y
	***SUCCESS*** Created project [gwtmvp] in new working directory [/Users/sso/Documents/dev/adorsys/gwtmvp]
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/pom.xml
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/test/java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/webapp
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/resources
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/test/resources
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/resources/META-INF/forge.xml
	
* Setup GWT base configuration

	[gwtmvp] gwtmvp $ gwt setup
	com.google.gwt:gwt-user:jar::2.4.0
	org.slf4j:slf4j-api:jar::1.6.1
	org.jvnet.hudson.main:hudson-gwt-slf4j:jar::2.1.1
	javax.ws.rs:jsr311-api:jar::1.1.1
	org.fusesource.restygwt:restygwt:jar::1.2
	org.hibernate:hibernate-validator:jar:sources:4.2.0.Final
	org.hibernate:hibernate-validator:jar::4.2.0.Final
	com.googlecode.mvp4g:mvp4g:jar::1.4.0
	***SUCCESS*** Installed [gwtfacet] successfully.
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/pom.xml
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/resources/de/adorsys/gwtmvp/Messages.properties
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/resources/de/adorsys/gwtmvp/Gwtmvp.gwt.xml
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/webapp/WEB-INF/web.xml
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/webapp/index.html
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/GwtmvpValidationMessageResolver.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/GwtmvpValidatorFactory.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/model/GwtmvpModel.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/GwtmvpEntryPoint.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/GwtmvpEventBus.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/GwtmvpGinClientModule.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/ReverseCompositeView.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/application/ApplicationPresenterImpl.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/application/ApplicationView.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/application/ApplicationViewImpl.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/resources/de/adorsys/gwtmvp/application/ApplicationViewImpl.ui.xml
	
* Create a MVP dialog
	
	[gwtmvp] MenuePresenterImpl.java $ gwt new-mvp menu

	Picked up type <JavaResource>: de.adorsys.gwtmvp.menu.MenuPresenterImpl
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/menu/MenuPresenterImpl.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/menu/MenuView.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/java/de/adorsys/gwtmvp/menu/MenuViewImpl.java
	Wrote /Users/sso/Documents/dev/adorsys/gwtmvp/src/main/resources/de/adorsys/gwtmvp/menu/MenuViewImpl.ui.xml

* Wire a event between eventbus and presenter

Add your event (myEvent) to the EventBus Interface

	@Events(startPresenter=ApplicationPresenterImpl.class, ginModules=GwtmvpGinClientModule.class)
	public interface GwtmvpEventBus extends EventBusWithLookup {
	
		@Start
		@Event
		void start();
	
		@Event(handlers=MenuPresenterImpl.class)
		void myEvent();
	}
	
	gwt wire-events 
	
* Test your new app

	* gwt run
	
	* gwt debug
	

/Happy coding
	

