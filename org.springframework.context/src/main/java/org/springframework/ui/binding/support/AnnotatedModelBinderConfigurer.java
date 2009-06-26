package org.springframework.ui.binding.support;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.ui.binding.Bound;
import org.springframework.ui.binding.Model;

final class AnnotatedModelBinderConfigurer {

	public void configure(GenericBinder binder) {
		Class<?> modelClass = binder.getModel().getClass();
		Model m = AnnotationUtils.findAnnotation(modelClass, Model.class);
		if (m != null) {
			binder.setStrict(m.strictBinding());
		}
		if (binder.isStrict()) {
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
					binder.configureBinding(new BindingConfiguration(prop.getName(), null));
				}
			}
		}
 	}
	
}
