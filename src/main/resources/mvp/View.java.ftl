package ${basePackage}.${name};

import com.google.gwt.user.client.ui.IsWidget;
import com.mvp4g.client.presenter.PresenterInterface;
import com.mvp4g.client.view.ReverseViewInterface;

import ${basePackage}.${classPrefix}EventBus;

public interface ${nameClassPrefix}View extends IsWidget, ReverseViewInterface<${nameClassPrefix}View.${nameClassPrefix}Presenter> {
	
	public interface ${nameClassPrefix}Presenter extends PresenterInterface<${nameClassPrefix}View, ${classPrefix}EventBus> {
		
	}

}
