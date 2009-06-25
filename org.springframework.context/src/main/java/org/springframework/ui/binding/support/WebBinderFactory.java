package org.springframework.ui.binding.support;

import org.springframework.ui.binding.Binder;
import org.springframework.ui.binding.BinderFactory;

public class WebBinderFactory implements BinderFactory {

	public Binder getBinder(Object model) {
		WebBinder binder = new WebBinder(model);
		new AnnotatedModelBinderConfigurer().configure(binder);
		return binder;
	}
	
	// internal helpers

}
