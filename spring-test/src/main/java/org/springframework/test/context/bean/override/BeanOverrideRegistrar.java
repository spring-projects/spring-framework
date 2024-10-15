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
 * An internal class used to track {@link OverrideMetadata}-related state after
 * the bean factory has been processed and to provide field injection utilities
 * for test execution listeners.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideRegistrar {

	private final Map<OverrideMetadata, String> beanNameRegistry = new HashMap<>();

	private final Map<String, OverrideMetadata> earlyOverrideMetadata = new HashMap<>();

	private final ConfigurableBeanFactory beanFactory;


	BeanOverrideRegistrar(ConfigurableBeanFactory beanFactory) {
		Assert.notNull(beanFactory, "ConfigurableBeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	/**
	 * Check {@linkplain #markWrapEarly(OverrideMetadata, String) early override}
	 * records and use the {@link OverrideMetadata} to create an override
	 * instance based on the provided bean, if relevant.
	 */
	Object wrapIfNecessary(Object bean, String beanName) throws BeansException {
		OverrideMetadata metadata = this.earlyOverrideMetadata.get(beanName);
		if (metadata != null && metadata.getStrategy() == BeanOverrideStrategy.WRAP) {
			bean = metadata.createOverride(beanName, null, bean);
			metadata.track(bean, this.beanFactory);
		}
		return bean;
	}

	/**
	 * Register the provided {@link OverrideMetadata} and associate it with the
	 * supplied {@code beanName}.
	 */
	void registerNameForMetadata(OverrideMetadata metadata, String beanName) {
		this.beanNameRegistry.put(metadata, beanName);
	}

	/**
	 * Mark the provided {@link OverrideMetadata} and {@code beanName} as "wrap
	 * early", allowing for later bean override using {@link #wrapIfNecessary(Object, String)}.
	 */
	void markWrapEarly(OverrideMetadata metadata, String beanName) {
		this.earlyOverrideMetadata.put(beanName, metadata);
	}

	void inject(Object target, OverrideMetadata overrideMetadata) {
		String beanName = this.beanNameRegistry.get(overrideMetadata);
		Assert.state(StringUtils.hasLength(beanName),
				() -> "No bean found for OverrideMetadata: " + overrideMetadata);
		inject(overrideMetadata.getField(), target, beanName);
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
