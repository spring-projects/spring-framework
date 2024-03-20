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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
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

	static final String INFRASTRUCTURE_BEAN_NAME = BeanOverrideRegistrar.class.getName();

	private final Map<OverrideMetadata, String> beanNameRegistry;
	private final Map<String, OverrideMetadata> earlyOverrideMetadata;
	private final Set<OverrideMetadata> overrideMetadata;

	@Nullable
	private ConfigurableBeanFactory beanFactory;

	/**
	 * Construct a new registrar and immediately parse the provided classes.
	 * @param classesToParse the initial set of classes that have been
	 * detected to contain bean overriding annotations, to be parsed immediately.
	 */
	BeanOverrideRegistrar(Set<Class<?>> classesToParse) {
		Set<OverrideMetadata> metadata = BeanOverrideParsingUtils.parse(classesToParse);
		Assert.state(!metadata.isEmpty(), "Expected metadata to be produced by parser");
		this.overrideMetadata = metadata;
		this.beanNameRegistry = new HashMap<>();
		this.earlyOverrideMetadata = new HashMap<>();
	}

	/**
	 * Return this processor's {@link OverrideMetadata} set.
	 */
	Set<OverrideMetadata> getOverrideMetadata() {
		return this.overrideMetadata;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory,
				"Bean OverrideRegistrar can only be used with a ConfigurableBeanFactory");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	/**
	 * Check {@link #markWrapEarly(OverrideMetadata, String) early override}
	 * records and use the {@link OverrideMetadata} to create an override
	 * instance from the provided bean, if relevant.
	 */
	final Object wrapIfNecessary(Object bean, String beanName) throws BeansException {
		final OverrideMetadata metadata = this.earlyOverrideMetadata.get(beanName);
		if (metadata != null && metadata.getStrategy() == BeanOverrideStrategy.WRAP_BEAN) {
			bean = metadata.createOverride(beanName, null, bean);
			Assert.state(this.beanFactory != null, "ConfigurableBeanFactory must not be null");
			metadata.track(bean, this.beanFactory);
		}
		return bean;
	}

	/**
	 * Register the provided {@link OverrideMetadata} and associated it with a
	 * {@code beanName}.
	 */
	void registerNameForMetadata(OverrideMetadata metadata, String beanName) {
		this.beanNameRegistry.put(metadata, beanName);
	}

	/**
	 * Mark the provided {@link OverrideMetadata} and {@code beanName} as "wrap
	 * early", allowing for later bean override using {@link #wrapIfNecessary(Object, String)}.
	 */
	public void markWrapEarly(OverrideMetadata metadata, String beanName) {
		this.earlyOverrideMetadata.put(beanName, metadata);
	}

	/**
	 * Register a bean definition for a {@link BeanOverrideRegistrar} if it does
	 * not yet exist. Additionally, each call adds the provided
	 * {@code detectedTestClasses} to the set that will be used as constructor
	 * argument.
	 * <p>The resulting complete set of test classes will be parsed as soon as
	 * the {@link BeanOverrideRegistrar} is constructed.
	 * @param registry the bean definition registry
	 * @param detectedTestClasses a partial {@link Set} of {@link Class classes}
	 * that are expected to contain bean overriding annotations
	 */
	public static void register(BeanDefinitionRegistry registry, @Nullable Set<Class<?>> detectedTestClasses) {
		BeanDefinition definition;
		if (!registry.containsBeanDefinition(BeanOverrideRegistrar.INFRASTRUCTURE_BEAN_NAME)) {
			definition = new RootBeanDefinition(BeanOverrideRegistrar.class);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
			constructorArguments.addIndexedArgumentValue(0, new LinkedHashSet<Class<?>>());
			registry.registerBeanDefinition(INFRASTRUCTURE_BEAN_NAME, definition);
		}
		else {
			definition = registry.getBeanDefinition(BeanOverrideRegistrar.INFRASTRUCTURE_BEAN_NAME);
		}

		ConstructorArgumentValues.ValueHolder constructorArg =
				definition.getConstructorArgumentValues().getIndexedArgumentValue(0, Set.class);
		@SuppressWarnings({"unchecked", "NullAway"})
		Set<Class<?>> existing = (Set<Class<?>>) constructorArg.getValue();
		if (detectedTestClasses != null && existing != null) {
			existing.addAll(detectedTestClasses);
		}
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
			Assert.state(this.beanFactory != null, "beanFactory must not be null");
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
