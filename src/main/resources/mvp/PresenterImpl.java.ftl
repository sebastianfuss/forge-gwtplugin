package ${basePackage}.${name};

import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.mvp4g.client.annotation.Presenter;
import com.mvp4g.client.presenter.BasePresenter;

import ${basePackage}.${classPrefix}EventBus;
import ${basePackage}.${name}.${nameClassPrefix}View.${nameClassPrefix}Presenter;

@Presenter( view = ${nameClassPrefix}ViewImpl.class )
public class ${nameClassPrefix}PresenterImpl extends BasePresenter<${nameClassPrefix}View, ${classPrefix}EventBus> implements ${nameClassPrefix}Presenter {
	
    public void onInit() {
    }
    
}