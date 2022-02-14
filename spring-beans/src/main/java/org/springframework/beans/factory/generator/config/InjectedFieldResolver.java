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

package org.springframework.beans.factory.generator.config;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * An {@link InjectedElementResolver} for a {@link Field}.
 *
 * @author Stephane Nicoll
 */
class InjectedFieldResolver implements InjectedElementResolver {

	private final Field field;

	private final String beanName;


	/**
	 * Create a new instance.
	 * @param field the field to handle
	 * @param beanName the name of the bean, or {@code null}
	 */
	InjectedFieldResolver(Field field, String beanName) {
		this.field = field;
		this.beanName = beanName;
	}

	@Override
	public InjectedElementAttributes resolve(DefaultListableBeanFactory beanFactory, boolean required) {
		DependencyDescriptor desc = new DependencyDescriptor(this.field, required);
		desc.setContainingClass(this.field.getType());
		Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		try {
			Object value = beanFactory.resolveDependency(desc, this.beanName, autowiredBeanNames, typeConverter);
			if (value == null && !required) {
				return new InjectedElementAttributes(null);
			}
			return new InjectedElementAttributes(Collections.singletonList(value));
		}
		catch (BeansException ex) {
			throw new UnsatisfiedDependencyException(null, this.beanName, new InjectionPoint(this.field), ex);
		}
	}

}
