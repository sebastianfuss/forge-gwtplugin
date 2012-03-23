<?xml version="1.0" encoding="UTF-8"?>
<module rename-to='${basePackage}'>
	<inherits name='com.google.gwt.user.User' />
	<inherits name="com.google.gwt.user.Debug" />

	<!--  inherits name='com.google.gwt.user.theme.standard.Standard'/ -->
	<inherits name='org.fusesource.restygwt.RestyGWT' />
	<inherits name='com.mvp4g.Mvp4gModule' />
	<inherits name="com.google.gwt.validation.Validation" />

	<inherits name='org.hibernate.validator.HibernateValidator' />
	<inherits name='com.google.gwt.editor.Editor' />
	<set-property name="user.agent" value="safari,gecko1_8" />
	<inherits name='org.slf4j.Slf4j' />
	<set-property name="gwt.logging.logLevel" value="FINE" />
	<set-property name="gwt.logging.popupHandler" value="DISABLED" />

	<source path='' />

	<entry-point class='${basePackage}.${classPrefix}EntryPoint' />

	<replace-with class="${basePackage}.${classPrefix}ValidatorFactory">
		<when-type-is class="javax.validation.ValidatorFactory" />
	</replace-with>

	<replace-with
		class="${basePackage}.${classPrefix}ValidationMessageResolver">
		<when-type-is
			class="com.google.gwt.validation.client.ProviderValidationMessageResolver" />
	</replace-with>
</module>