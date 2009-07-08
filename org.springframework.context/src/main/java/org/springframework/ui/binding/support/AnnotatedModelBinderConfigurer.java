package org.springframework.ui.binding.support;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.binding.Bound;

final class AnnotatedModelBinderConfigurer {

	public void configure(GenericBinder binder) {
		Class<?> modelClass = binder.getModel().getClass();
		BeanInfo beanInfo;
		try {
			beanInfo = Introspector.getBeanInfo(modelClass);
		} catch (IntrospectionException e) {
			throw new IllegalStateException("Unable to introspect model " + binder.getModel(), e);
		}
		// TODO do we have to still flush introspector cache here?
		for (PropertyDescriptor prop : beanInfo.getPropertyDescriptors()) {
			Method getter = prop.getReadMethod();
			Bound b = AnnotationUtils.getAnnotation(getter, Bound.class);
			if (b != null) {
				// TODO should we wire formatter here if using a format annotation - an optimization?
				binder.addBinding(prop.getName());
			}
		}
	}

}
