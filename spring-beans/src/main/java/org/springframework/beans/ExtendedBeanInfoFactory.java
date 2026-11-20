/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.lang.reflect.Method;

import org.springframework.core.Ordered;

/**
 * Extension of {@link StandardBeanInfoFactory} that supports "non-standard"
 * JavaBeans setter methods through introspection by Spring's
 * (package-visible) {@code ExtendedBeanInfo} implementation.
 *
 * <p>To be configured via a {@code META-INF/spring.factories} file with the following content:
 *
 * <p>{@code org.springframework.beans.BeanInfoFactory=org.springframework.beans.ExtendedBeanInfoFactory}
 *
 * <p>Ordered at {@link Ordered#LOWEST_PRECEDENCE} to allow other user-defined
 * {@link BeanInfoFactory} types to take precedence.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.2
 * @see StandardBeanInfoFactory
 * @see CachedIntrospectionResults
 */
public class ExtendedBeanInfoFactory extends StandardBeanInfoFactory {

	@Override
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		BeanInfo beanInfo = super.getBeanInfo(beanClass);
		return (supports(beanClass) ? new ExtendedBeanInfo(beanInfo) : beanInfo);
	}

	/**
	 * Return whether the given bean class declares or inherits any non-void
	 * returning bean property or indexed property setter methods.
	 */
	private boolean supports(Class<?> beanClass) {
		for (Method method : beanClass.getMethods()) {
			if (ExtendedBeanInfo.isCandidateWriteMethod(method)) {
				return true;
			}
		}
		return false;
	}

}
