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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} implementation that processes test classes
 * and adapt the {@link BeanDefinitionRegistry} for any {@link BeanOverride} it
 * may define.
 *
 * <p>A set of classes from which to parse {@link OverrideMetadata} must be
 * provided to this processor. Each test class is expected to use any
 * annotation meta-annotated with {@link BeanOverride @BeanOverride} to mark
 * beans to override. The {@link BeanOverrideParsingUtils#hasBeanOverride(Class)}
 * method can be used to check if a class matches the above criteria.
 *
 * <p>The provided classes are fully parsed at creation to build a metadata set.
 * This processor implements several {@link BeanOverrideStrategy overriding
 * strategy} and chooses the correct one according to each override metadata
 * {@link OverrideMetadata#getStrategy()} method. Additionally, it provides
 * support for injecting the overridden bean instances into their corresponding
 * annotated {@link Field fields}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
class BeanOverrideBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

	private static final String INFRASTRUCTURE_BEAN_NAME = BeanOverrideBeanFactoryPostProcessor.class.getName();

	private static final String EARLY_INFRASTRUCTURE_BEAN_NAME =
			BeanOverrideBeanFactoryPostProcessor.WrapEarlyBeanPostProcessor.class.getName();

	private final BeanOverrideRegistrar overrideRegistrar;


	/**
	 * Create a new {@code BeanOverrideBeanFactoryPostProcessor} instance with
	 * the given {@link BeanOverrideRegistrar}, which contains a set of parsed
	 * {@link OverrideMetadata}.
	 * @param overrideRegistrar the {@link BeanOverrideRegistrar} used to track
	 * metadata
	 */
	public BeanOverrideBeanFactoryPostProcessor(BeanOverrideRegistrar overrideRegistrar) {
		this.overrideRegistrar = overrideRegistrar;
	}


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory,
				"Bean overriding annotations can only be used on a DefaultListableBeanFactory");
		postProcessWithRegistry((DefaultListableBeanFactory) beanFactory);
	}

	private void postProcessWithRegistry(DefaultListableBeanFactory registry) {
		for (OverrideMetadata metadata : this.overrideRegistrar.getOverrideMetadata()) {
			registerBeanOverride(registry, metadata);
		}
	}

	/**
	 * Copy certain details of a {@link BeanDefinition} to the definition created by
	 * this processor for a given {@link OverrideMetadata}.
	 * <p>The default implementation copies the {@linkplain BeanDefinition#isPrimary()
	 * primary flag} and the {@linkplain BeanDefinition#getScope() scope}.
	 */
	protected void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
		to.setPrimary(from.isPrimary());
		to.setScope(from.getScope());
	}

	private void registerBeanOverride(DefaultListableBeanFactory beanFactory, OverrideMetadata overrideMetadata) {
		switch (overrideMetadata.getStrategy()) {
			case REPLACE_DEFINITION -> registerReplaceDefinition(beanFactory, overrideMetadata, true);
			case REPLACE_OR_CREATE_DEFINITION -> registerReplaceDefinition(beanFactory, overrideMetadata, false);
			case WRAP_BEAN -> registerWrapBean(beanFactory, overrideMetadata);
		}
	}

	private void registerReplaceDefinition(DefaultListableBeanFactory beanFactory, OverrideMetadata overrideMetadata, boolean enforceExistingDefinition) {

		RootBeanDefinition beanDefinition = createBeanDefinition(overrideMetadata);
		String beanName = overrideMetadata.getBeanName();

		BeanDefinition existingBeanDefinition = null;
		if (beanFactory.containsBeanDefinition(beanName)) {
			existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
			copyBeanDefinitionDetails(existingBeanDefinition, beanDefinition);
			beanFactory.removeBeanDefinition(beanName);
		}
		else if (enforceExistingDefinition) {
			throw new IllegalStateException("Unable to override bean '" + beanName + "'; there is no" +
					" bean definition to replace with that name");
		}
		beanFactory.registerBeanDefinition(beanName, beanDefinition);

		Object override = overrideMetadata.createOverride(beanName, existingBeanDefinition, null);
		if (beanFactory.isSingleton(beanName)) {
			// Now we have an instance (the override) that we can register.
			// At this stage we don't expect a singleton instance to be present,
			// and this call will throw if there is such an instance already.
			beanFactory.registerSingleton(beanName, override);
		}

		overrideMetadata.track(override, beanFactory);
		this.overrideRegistrar.registerNameForMetadata(overrideMetadata, beanName);
	}

	/**
	 * Check that the expected bean name is registered and matches the type to override.
	 * <p>If so, put the override metadata in the early tracking map.
	 * <p>The map will later be checked to see if a given bean should be wrapped
	 * upon creation, during the {@link WrapEarlyBeanPostProcessor#getEarlyBeanReference(Object, String)}
	 * phase.
	 */
	private void registerWrapBean(DefaultListableBeanFactory beanFactory, OverrideMetadata metadata) {
		Set<String> existingBeanNames = getExistingBeanNames(beanFactory, metadata.getBeanType());
		String beanName = metadata.getBeanName();
		if (!existingBeanNames.contains(beanName)) {
			throw new IllegalStateException("Unable to override bean '" + beanName + "' by wrapping," +
					" no existing bean instance by this name of type " + metadata.getBeanType());
		}
		this.overrideRegistrar.markWrapEarly(metadata, beanName);
		this.overrideRegistrar.registerNameForMetadata(metadata, beanName);
	}

	private RootBeanDefinition createBeanDefinition(OverrideMetadata metadata) {
		RootBeanDefinition definition = new RootBeanDefinition();
		definition.setTargetType(metadata.getBeanType());
		return definition;
	}

	private Set<String> getExistingBeanNames(DefaultListableBeanFactory beanFactory, ResolvableType resolvableType) {
		Set<String> beans = new LinkedHashSet<>(
				Arrays.asList(beanFactory.getBeanNamesForType(resolvableType, true, false)));
		Class<?> type = resolvableType.resolve(Object.class);
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			Object attribute = beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
			if (resolvableType.equals(attribute) || type.equals(attribute)) {
				beans.add(beanName);
			}
		}
		beans.removeIf(ScopedProxyUtils::isScopedTarget);
		return beans;
	}

	/**
	 * Register a {@link BeanOverrideBeanFactoryPostProcessor} with a {@link BeanDefinitionRegistry}.
	 * <p>Not required when using the Spring TestContext Framework, as registration
	 * is automatic via the
	 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
	 * mechanism.
	 * @param registry the bean definition registry
	 */
	public static void register(BeanDefinitionRegistry registry) {
		RuntimeBeanReference registrarReference = new RuntimeBeanReference(BeanOverrideRegistrar.INFRASTRUCTURE_BEAN_NAME);
		// Early processor
		addInfrastructureBeanDefinition(
				registry, WrapEarlyBeanPostProcessor.class, EARLY_INFRASTRUCTURE_BEAN_NAME, constructorArgs ->
					constructorArgs.addIndexedArgumentValue(0, registrarReference));

		// Main processor
		addInfrastructureBeanDefinition(
				registry, BeanOverrideBeanFactoryPostProcessor.class, INFRASTRUCTURE_BEAN_NAME, constructorArgs ->
						constructorArgs.addIndexedArgumentValue(0, registrarReference));
	}

	private static void addInfrastructureBeanDefinition(BeanDefinitionRegistry registry,
			Class<?> clazz, String beanName, Consumer<ConstructorArgumentValues> constructorArgumentsConsumer) {

		if (!registry.containsBeanDefinition(beanName)) {
			RootBeanDefinition definition = new RootBeanDefinition(clazz);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
			constructorArgumentsConsumer.accept(constructorArguments);
			registry.registerBeanDefinition(beanName, definition);
		}
	}


	static final class WrapEarlyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
			PriorityOrdered {

		private final BeanOverrideRegistrar overrideRegistrar;

		private final Map<String, Object> earlyReferences;


		private WrapEarlyBeanPostProcessor(BeanOverrideRegistrar registrar) {
			this.overrideRegistrar = registrar;
			this.earlyReferences = new ConcurrentHashMap<>(16);
		}


		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}

		@Override
		public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			this.earlyReferences.put(getCacheKey(bean, beanName), bean);
			return this.overrideRegistrar.wrapIfNecessary(bean, beanName);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			if (this.earlyReferences.remove(getCacheKey(bean, beanName)) != bean) {
				return this.overrideRegistrar.wrapIfNecessary(bean, beanName);
			}
			return bean;
		}

		private String getCacheKey(Object bean, String beanName) {
			return (StringUtils.hasLength(beanName) ? beanName : bean.getClass().getName());
		}

	}

}
