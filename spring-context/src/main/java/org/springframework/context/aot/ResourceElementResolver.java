/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.context.aot;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.lang.model.element.Element;

import jakarta.annotation.Resource;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Base class for resolvers that support injection of named beans on
 * an {@link Element}.
 *
 * @author Stephane Nicoll
 * @since 6.1
 * @see Resource
 */
public abstract class ResourceElementResolver {

	protected final String name;

	protected final boolean defaultName;

	protected ResourceElementResolver(String name, boolean defaultName) {
		this.name = name;
		this.defaultName = defaultName;
	}

	/**
	 * Resolve the field value for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved field value
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T resolve(RegisteredBean registeredBean) {
		return (T) resolveObject(registeredBean);
	}

	/**
	 * Resolve the field value for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved field value
	 */
	public Object resolveObject(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		return resolveValue(registeredBean);
	}


	/**
	 * Create a suitable {@link DependencyDescriptor} for the specified bean.
	 * @param bean the registered bean
	 * @return a descriptor for that bean
	 */
	protected abstract DependencyDescriptor createDependencyDescriptor(RegisteredBean bean);

	/**
	 * Resolve the value to inject for this instance.
	 * @param bean the bean registration
	 * @return the value to inject
	 */
	protected Object resolveValue(RegisteredBean bean) {
		ConfigurableListableBeanFactory factory = bean.getBeanFactory();

		Object resource;
		Set<String> autowiredBeanNames;
		DependencyDescriptor descriptor = createDependencyDescriptor(bean);
		if (this.defaultName && !factory.containsBean(this.name)) {
			autowiredBeanNames = new LinkedHashSet<>();
			resource = factory.resolveDependency(descriptor, bean.getBeanName(), autowiredBeanNames, null);
			if (resource == null) {
				throw new NoSuchBeanDefinitionException(descriptor.getDependencyType(), "No resolvable resource object");
			}
		}
		else {
			resource = factory.resolveBeanByName(this.name, descriptor);
			autowiredBeanNames = Collections.singleton(this.name);
		}

		for (String autowiredBeanName : autowiredBeanNames) {
			if (factory.containsBean(autowiredBeanName)) {
				factory.registerDependentBean(autowiredBeanName, bean.getBeanName());
			}
		}
		return resource;
	}


	@SuppressWarnings("serial")
	protected static class LookupDependencyDescriptor extends DependencyDescriptor {

		private final Class<?> lookupType;

		public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
			super(field, true);
			this.lookupType = lookupType;
		}

		public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
			super(new MethodParameter(method, 0), true);
			this.lookupType = lookupType;
		}

		@Override
		public Class<?> getDependencyType() {
			return this.lookupType;
		}
	}

}
