package ${basePackage};

import org.hibernate.validator.ValidationMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.validation.client.AbstractValidationMessageResolver;
import com.google.gwt.validation.client.ProviderValidationMessageResolver;

public class ${classPrefix}ValidationMessageResolver extends
    AbstractValidationMessageResolver implements
    ProviderValidationMessageResolver {

  public ${classPrefix}ValidationMessageResolver() {
    super((ConstantsWithLookup) GWT.create(ValidationMessages.class));
  }
}