package ${basePackage}.${name};

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import ${basePackage}.${name}.${nameClassPrefix}View.${nameClassPrefix}Presenter;
import ${basePackage}.ReverseCompositeView;

public class ${nameClassPrefix}ViewImpl extends ReverseCompositeView<${nameClassPrefix}Presenter> implements ${nameClassPrefix}View {

    private static RootViewUiBinder uiBinder = GWT.create( RootViewUiBinder.class );

    interface RootViewUiBinder extends UiBinder<Widget, ApplicationViewImpl> {
    }

    public ${nameClassPrefix}ViewImpl() {
        initWidget( uiBinder.createAndBindUi( this ) );
    }

}