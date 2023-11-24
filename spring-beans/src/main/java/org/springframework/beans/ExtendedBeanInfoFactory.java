/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.lang.reflect.Method;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * Extension of {@link StandardBeanInfoFactory} that supports "non-standard"
 * JavaBeans setter methods through introspection by Spring's
 * (package-visible) {@code ExtendedBeanInfo} implementation.
 *
 * <p>To be configured via a {@code META-INF/spring.factories} file with the following content:
 * {@code org.springframework.beans.BeanInfoFactory=org.springframework.beans.ExtendedBeanInfoFactory}
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
	@NonNull
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
