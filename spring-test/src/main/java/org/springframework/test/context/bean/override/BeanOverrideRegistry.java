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
class BeanOverrideRegistry {

	private final Map<BeanOverrideHandler, String> handlerToBeanNameMap = new HashMap<>();

	private final Map<String, BeanOverrideHandler> wrappingBeanOverrideHandlers = new HashMap<>();

	private final ConfigurableBeanFactory beanFactory;


	BeanOverrideRegistry(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ConfigurableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * Register the provided {@link BeanOverrideHandler} and associate it with the
	 * given {@code beanName}.
	 * <p>Also associates a {@linkplain BeanOverrideStrategy#WRAP "wrapping"} handler
	 * with the given {@code beanName}, allowing for subsequent wrapping of the
	 * bean via {@link #wrapBeanIfNecessary(Object, String)}.
	 */
	void registerBeanOverrideHandler(BeanOverrideHandler handler, String beanName) {
		this.handlerToBeanNameMap.put(handler, beanName);
		if (handler.getStrategy() == BeanOverrideStrategy.WRAP) {
			this.wrappingBeanOverrideHandlers.put(beanName, handler);
		}
	}

	/**
	 * Use the registered {@linkplain BeanOverrideStrategy#WRAP "wrapping"}
	 * {@link BeanOverrideHandler} to create an override instance by wrapping the
	 * supplied bean.
	 * <p>If no suitable {@code BeanOverrideHandler} has been registered, this
	 * method returns the supplied bean unmodified.
	 * @see #registerBeanOverrideHandler(BeanOverrideHandler, String)
	 */
	Object wrapBeanIfNecessary(Object bean, String beanName) {
		if (!this.wrappingBeanOverrideHandlers.containsKey(beanName)) {
			return bean;
		}
		BeanOverrideHandler handler = this.wrappingBeanOverrideHandlers.get(beanName);
		Assert.state(handler != null,
				() -> "Failed to find wrapping BeanOverrideHandler for bean '" + beanName + "'");
		return handler.createOverrideInstance(beanName, null, bean, this.beanFactory);
	}

	void inject(Object target, BeanOverrideHandler handler) {
		String beanName = this.handlerToBeanNameMap.get(handler);
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
