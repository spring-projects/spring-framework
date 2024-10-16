/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context.bean.override;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An internal class used to track {@link BeanOverrideHandler}-related state after
 * the bean factory has been processed and to provide field injection utilities
 * for test execution listeners.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideRegistrar {

	private final Map<BeanOverrideHandler, String> beanNameRegistry = new HashMap<>();

	private final Map<String, BeanOverrideHandler> wrappingBeanOverrideHandlers = new HashMap<>();

	private final ConfigurableBeanFactory beanFactory;


	BeanOverrideRegistrar(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ConfigurableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * Register the provided {@link BeanOverrideHandler} and associate it with the
	 * given {@code beanName}.
	 */
	void registerBeanOverrideHandler(BeanOverrideHandler handler, String beanName) {
		this.beanNameRegistry.put(handler, beanName);
	}

	/**
	 * Register the provided {@link BeanOverrideHandler} as a
	 * {@linkplain BeanOverrideStrategy#WRAP "wrapping"} handler and associate it
	 * with the given {@code beanName}, allowing for subsequent wrapping of the
	 * bean via {@link #wrapIfNecessary(Object, String)}.
	 */
	void registerWrappingBeanOverrideHandler(BeanOverrideHandler handler, String beanName) {
		this.wrappingBeanOverrideHandlers.put(beanName, handler);
	}

	/**
	 * Check {@linkplain #registerWrappingBeanOverrideHandler(BeanOverrideHandler, String)
	 * wrapping handler} records and use the corresponding {@link BeanOverrideHandler}
	 * to create an override instance by wrapping the provided bean, if relevant.
	 */
	Object wrapIfNecessary(Object bean, String beanName) throws BeansException {
		BeanOverrideHandler handler = this.wrappingBeanOverrideHandlers.get(beanName);
		if (handler != null && handler.getStrategy() == BeanOverrideStrategy.WRAP) {
			bean = handler.createOverrideInstance(beanName, null, bean);
			handler.trackOverrideInstance(bean, this.beanFactory);
		}
		return bean;
	}

	void inject(Object target, BeanOverrideHandler handler) {
		String beanName = this.beanNameRegistry.get(handler);
		Assert.state(StringUtils.hasLength(beanName),
				() -> "No bean found for BeanOverrideHandler: " + handler);
		inject(handler.getField(), target, beanName);
	}

	private void inject(Field field, Object target, String beanName) {
		try {
			Object bean = this.beanFactory.getBean(beanName, field.getType());
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, target, bean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject field '" + field + "'", ex);
		}
	}

}
