package ${basePackage};

import com.google.gwt.user.client.ui.IsWidget;
import com.mvp4g.client.annotation.Debug;
import com.mvp4g.client.annotation.Event;
import com.mvp4g.client.annotation.Events;
import com.mvp4g.client.annotation.InitHistory;
import com.mvp4g.client.annotation.Start;
import com.mvp4g.client.event.EventBusWithLookup;

import ${basePackage}.application.ApplicationPresenterImpl;

@Events(startPresenter=ApplicationPresenterImpl.class, ginModules=${classPrefix}GinClientModule.class)
public interface ${classPrefix}EventBus extends EventBusWithLookup {
	
	@Start
	@Event
	void start();
	
}