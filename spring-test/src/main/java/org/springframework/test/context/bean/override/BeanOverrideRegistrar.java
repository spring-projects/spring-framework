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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * An internal class used to track {@link OverrideMetadata}-related state after
 * the bean factory has been processed and to provide field injection utilities
 * for test execution listeners.
 *
 * @author Simon Baslé
 * @since 6.2
 */
class BeanOverrideRegistrar implements BeanFactoryAware {

	private final Map<OverrideMetadata, String> beanNameRegistry = new HashMap<>();

	private final Map<String, OverrideMetadata> earlyOverrideMetadata = new HashMap<>();

	@Nullable
	private ConfigurableBeanFactory beanFactory;


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof ConfigurableBeanFactory cbf)) {
			throw new IllegalStateException("Cannot process bean override with a BeanFactory " +
					"that doesn't implement ConfigurableBeanFactory: " + beanFactory.getClass());
		}
		this.beanFactory = cbf;
	}

	/**
	 * Check {@link #markWrapEarly(OverrideMetadata, String) early override}
	 * records and use the {@link OverrideMetadata} to create an override
	 * instance from the provided bean, if relevant.
	 */
	Object wrapIfNecessary(Object bean, String beanName) throws BeansException {
		OverrideMetadata metadata = this.earlyOverrideMetadata.get(beanName);
		if (metadata != null && metadata.getStrategy() == BeanOverrideStrategy.WRAP_BEAN) {
			Assert.state(this.beanFactory != null, "ConfigurableBeanFactory must not be null");
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
			ReflectionUtils.makeAccessible(field);
			Object existingValue = ReflectionUtils.getField(field, target);
			Assert.state(this.beanFactory != null, "ConfigurableBeanFactory must not be null");
			Object bean = this.beanFactory.getBean(beanName, field.getType());
			if (existingValue == bean) {
				return;
			}
			Assert.state(existingValue == null, () -> "The existing value '" + existingValue +
					"' of field '" + field + "' is not the same as the new value '" + bean + "'");
			ReflectionUtils.setField(field, target, bean);
		}
		catch (Throwable ex) {
			throw new BeanCreationException("Could not inject field '" + field + "'", ex);
		}
	}

}
