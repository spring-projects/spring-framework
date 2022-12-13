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

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Collection;

import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;

/**
 * {@link BeanInfoFactory} implementation that bypasses the standard {@link java.beans.Introspector}
 * for faster introspection, reduced to basic property determination (as commonly needed in Spring).
 *
 * <p>Used by default in 6.0 through direct invocation from {@link CachedIntrospectionResults}.
 * Potentially configured via a {@code META-INF/spring.factories} file with the following content,
 * overriding other custom {@code org.springframework.beans.BeanInfoFactory} declarations:
 * {@code org.springframework.beans.BeanInfoFactory=org.springframework.beans.SimpleBeanInfoFactory}
 *
 * <p>Ordered at {@code Ordered.LOWEST_PRECEDENCE - 1} to override {@link ExtendedBeanInfoFactory}
 * (registered by default in 5.3) if necessary while still allowing other user-defined
 * {@link BeanInfoFactory} types to take precedence.
 *
 * @author Juergen Hoeller
 * @since 5.3.24
 * @see ExtendedBeanInfoFactory
 * @see CachedIntrospectionResults
 */
class SimpleBeanInfoFactory implements BeanInfoFactory, Ordered {

	@Override
	@NonNull
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		Collection<? extends PropertyDescriptor> pds =
				PropertyDescriptorUtils.determineBasicProperties(beanClass);

		return new SimpleBeanInfo() {
			@Override
			public BeanDescriptor getBeanDescriptor() {
				return new BeanDescriptor(beanClass);
			}
			@Override
			public PropertyDescriptor[] getPropertyDescriptors() {
				return pds.toArray(PropertyDescriptorUtils.EMPTY_PROPERTY_DESCRIPTOR_ARRAY);
			}
		};
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

}
