/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.springframework.core.Ordered;

/**
 * {@link BeanInfoFactory} implementation that evaluates whether bean classes have
 * "non-standard" JavaBeans setter methods and are thus candidates for introspection by
 * Spring's {@link ExtendedBeanInfo}.
 *
 * <p>Ordered at {@link Ordered#LOWEST_PRECEDENCE} to allow other user-defined
 * {@link BeanInfoFactory} types to take precedence.
 *
 * @author Chris Beams
 * @since 3.2
 * @see BeanInfoFactory
 */
public class ExtendedBeanInfoFactory implements Ordered, BeanInfoFactory {

	/**
	 * Return a new {@link ExtendedBeanInfo} for the given bean class.
	 */
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		return supports(beanClass) ?
				new ExtendedBeanInfo(Introspector.getBeanInfo(beanClass)) : null;
	}

	/**
	 * Return whether the given bean class declares or inherits any non-void returning
	 * JavaBeans or <em>indexed property</em> setter methods.
	 */
	private boolean supports(Class<?> beanClass) {
		for (Method method : beanClass.getMethods()) {
			String methodName = method.getName();
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (Modifier.isPublic(method.getModifiers())
					&& methodName.length() > 3
					&& methodName.startsWith("set")
					&& (parameterTypes.length == 1
						|| (parameterTypes.length == 2 && parameterTypes[0].equals(int.class)))
					&& !void.class.isAssignableFrom(method.getReturnType())) {
				return true;
			}
		}
		return false;
	}

	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
