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

package org.springframework.test.bean.override;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} used to register and inject overriding
 * bean metadata with the {@link ApplicationContext}. A set of
 * {@link OverrideMetadata} must be passed to the processor.
 * A {@link BeanOverrideParser} can typically be used to parse these from test
 * classes that use any annotation meta-annotated with {@link BeanOverride} to
 * mark override sites.
 *
 * <p>This processor supports two {@link BeanOverrideStrategy}:
 * <ul>
 * <li>replacing a given bean's definition, immediately preparing a singleton
 * instance</li>
 * <li>intercepting the actual bean instance upon creation and wrapping it,
 * using the early bean definition mechanism of
 * {@link SmartInstantiationAwareBeanPostProcessor}).</li>
 * </ul>
 *
 * <p>This processor also provides support for injecting the overridden bean
 * instances into their corresponding annotated {@link Field fields}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public class BeanOverrideBeanPostProcessor implements InstantiationAwareBeanPostProcessor,
		BeanFactoryAware, BeanFactoryPostProcessor, Ordered {

	private static final String INFRASTRUCTURE_BEAN_NAME = BeanOverrideBeanPostProcessor.class.getName();
	private static final String EARLY_INFRASTRUCTURE_BEAN_NAME = BeanOverrideBeanPostProcessor.WrapEarlyBeanPostProcessor.class.getName();

	private final Set<OverrideMetadata> overrideMetadata;
	private final Map<String, OverrideMetadata> earlyOverrideMetadata = new HashMap<>();

	private ConfigurableListableBeanFactory beanFactory;

	private final Map<OverrideMetadata, String> beanNameRegistry = new HashMap<>();

	private final Map<Field, String> fieldRegistry = new HashMap<>();

	/**
	 * Create a new {@link BeanOverrideBeanPostProcessor} instance with the
	 * given {@link OverrideMetadata} set.
	 * @param overrideMetadata the initial override metadata
	 */
	public BeanOverrideBeanPostProcessor(Set<OverrideMetadata> overrideMetadata) {
		this.overrideMetadata = overrideMetadata;
	}


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"Beans overriding can only be used with a ConfigurableListableBeanFactory");
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	/**
	 * Return this processor's {@link OverrideMetadata} set.
	 */
	protected Set<OverrideMetadata> getOverrideMetadata() {
		return this.overrideMetadata;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Assert.state(this.beanFactory == beanFactory, "Unexpected beanFactory to postProcess");
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"Bean overriding annotations can only be used on bean factories that implement "
						+ "BeanDefinitionRegistry");
		postProcessWithRegistry((BeanDefinitionRegistry) beanFactory);
	}

	private void postProcessWithRegistry(BeanDefinitionRegistry registry) {
		//Note that a tracker bean is registered down the line only if there is some overrideMetadata parsed
		Set<OverrideMetadata> overrideMetadata = getOverrideMetadata();
		for (OverrideMetadata metadata : overrideMetadata) {
			registerBeanOverride(registry, metadata);
		}
	}

	/**
	 * Copy the details of a {@link BeanDefinition} to the definition created by
	 * this processor for a given {@link OverrideMetadata}. Defaults to copying
	 * the {@link BeanDefinition#isPrimary()} attribute and scope.
	 */
	protected void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
		to.setPrimary(from.isPrimary());
		to.setScope(from.getScope());
	}

	private void registerBeanOverride(BeanDefinitionRegistry registry, OverrideMetadata overrideMetadata) {
		switch (overrideMetadata.getBeanOverrideStrategy()) {
			case REPLACE_DEFINITION -> registerReplaceDefinition(registry, overrideMetadata, true);
			case REPLACE_OR_CREATE_DEFINITION -> registerReplaceDefinition(registry, overrideMetadata, false);
			case WRAP_EARLY_BEAN -> registerWrapEarly(overrideMetadata);
		}
	}

	private void registerReplaceDefinition(BeanDefinitionRegistry registry, OverrideMetadata overrideMetadata,
			boolean enforceExistingDefinition) {
		RootBeanDefinition beanDefinition = createBeanDefinition(overrideMetadata);
		String beanName = overrideMetadata.getExpectedBeanName();

		BeanDefinition existingBeanDefinition = null;
		if (registry.containsBeanDefinition(beanName)) {
			existingBeanDefinition = registry.getBeanDefinition(beanName);
			copyBeanDefinitionDetails(existingBeanDefinition, beanDefinition);
			registry.removeBeanDefinition(beanName);
		}
		else if (enforceExistingDefinition) {
			throw new IllegalStateException("Unable to override " + overrideMetadata.getBeanOverrideDescription() +
					" bean, expected a bean definition to replace with name '" + beanName + "'");
		}
		registry.registerBeanDefinition(beanName, beanDefinition);

		Object override = overrideMetadata.createOverride(beanName, existingBeanDefinition, null);
		if (this.beanFactory.isSingleton(beanName)) {
			// Now we have an instance (the override) that we can register.
			// At this stage we don't expect a singleton instance to be present,
			// and this call will throw if there is such an instance already.
			this.beanFactory.registerSingleton(beanName, override);
		}

		overrideMetadata.track(override, this.beanFactory);
		this.beanNameRegistry.put(overrideMetadata, beanName);
		this.fieldRegistry.put(overrideMetadata.field(), beanName);
	}

	/**
	 * Check that the expected bean name is registered and matches the type to override.
	 * If so, put the override metadata in the early tracking map.
	 * The map will later be checked to see if a given bean should be wrapped
	 * upon creation, during the {@link WrapEarlyBeanPostProcessor#getEarlyBeanReference(Object, String)}
	 * phase
	 */
	private void registerWrapEarly(OverrideMetadata metadata) {
		Set<String> existingBeanNames = getExistingBeanNames(metadata.typeToOverride());
		String beanName = metadata.getExpectedBeanName();
		if (!existingBeanNames.contains(beanName)) {
			throw new IllegalStateException("Unable to override wrap-early bean named '" + beanName + "', not found among " +
					existingBeanNames);
		}
		this.earlyOverrideMetadata.put(beanName, metadata);
		this.beanNameRegistry.put(metadata, beanName);
		this.fieldRegistry.put(metadata.field(), beanName);
	}

	/**
	 * Check early overrides records and use the {@link OverrideMetadata} to
	 * create an override instance from the provided bean, if relevant.
	 * <p>Called during the {@link SmartInstantiationAwareBeanPostProcessor}
	 * phases (see {@link WrapEarlyBeanPostProcessor#getEarlyBeanReference(Object, String)}
	 * and {@link WrapEarlyBeanPostProcessor#postProcessAfterInitialization(Object, String)}).
	 */
	protected final Object wrapIfNecessary(Object bean, String beanName) throws BeansException {
		final OverrideMetadata metadata = this.earlyOverrideMetadata.get(beanName);
		if (metadata != null && metadata.getBeanOverrideStrategy() == BeanOverrideStrategy.WRAP_EARLY_BEAN) {
			bean = metadata.createOverride(beanName, null, bean);
			metadata.track(bean, this.beanFactory);
		}
		return bean;
	}

	private RootBeanDefinition createBeanDefinition(OverrideMetadata metadata) {
		RootBeanDefinition definition = new RootBeanDefinition(metadata.typeToOverride().resolve());
		definition.setTargetType(metadata.typeToOverride());
		return definition;
	}

	private Set<String> getExistingBeanNames(ResolvableType resolvableType) {
		Set<String> beans = new LinkedHashSet<>(
				Arrays.asList(this.beanFactory.getBeanNamesForType(resolvableType, true, false)));
		Class<?> type = resolvableType.resolve(Object.class);
		for (String beanName : this.beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(beanName);
			Object attribute = beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
			if (resolvableType.equals(attribute) || type.equals(attribute)) {
				beans.add(beanName);
			}
		}
		beans.removeIf(this::isScopedTarget);
		return beans;
	}

	private boolean isScopedTarget(String beanName) {
		try {
			return ScopedProxyUtils.isScopedTarget(beanName);
		}
		catch (Throwable ex) {
			return false;
		}
	}

	private void postProcessField(Object bean, Field field) {
		String beanName = this.fieldRegistry.get(field);
		if (StringUtils.hasText(beanName)) {
			inject(field, bean, beanName);
		}
	}

	@Override
	public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName)
			throws BeansException {
		ReflectionUtils.doWithFields(bean.getClass(), field -> postProcessField(bean, field));
		return pvs;
	}

	void inject(Field field, Object target, OverrideMetadata overrideMetadata) {
		String beanName = this.beanNameRegistry.get(overrideMetadata);
		Assert.state(StringUtils.hasLength(beanName), () -> "No bean found for overrideMetadata " + overrideMetadata);
		inject(field, target, beanName);
	}

	private void inject(Field field, Object target, String beanName) {
		try {
			field.setAccessible(true);
			Object existingValue = ReflectionUtils.getField(field, target);
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

	/**
	 * Register the processor with a {@link BeanDefinitionRegistry}.
	 * Not required when using the Spring TestContext Framework, as registration
	 * is automatic via the {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
	 * mechanism.
	 * @param registry the bean definition registry
	 * @param overrideMetadata the initial override metadata set
	 */
	public static void register(BeanDefinitionRegistry registry, @Nullable Set<OverrideMetadata> overrideMetadata) {
		//early processor
		getOrAddInfrastructureBeanDefinition(registry, WrapEarlyBeanPostProcessor.class, EARLY_INFRASTRUCTURE_BEAN_NAME,
				constructorArguments -> constructorArguments.addIndexedArgumentValue(0,
						new RuntimeBeanReference(INFRASTRUCTURE_BEAN_NAME)));

		//main processor
		BeanDefinition definition = getOrAddInfrastructureBeanDefinition(registry, BeanOverrideBeanPostProcessor.class,
				INFRASTRUCTURE_BEAN_NAME, constructorArguments -> constructorArguments
						.addIndexedArgumentValue(0, new LinkedHashSet<OverrideMetadata>()));
		ConstructorArgumentValues.ValueHolder constructorArg = definition.getConstructorArgumentValues()
				.getIndexedArgumentValue(0, Set.class);
		@SuppressWarnings("unchecked")
		Set<OverrideMetadata> existing = (Set<OverrideMetadata>) constructorArg.getValue();
		if (overrideMetadata != null && existing != null) {
			existing.addAll(overrideMetadata);
		}
	}

	private static BeanDefinition getOrAddInfrastructureBeanDefinition(BeanDefinitionRegistry registry,
			Class<?> clazz, String beanName, Consumer<ConstructorArgumentValues> constructorArgumentsConsumer) {
		if (!registry.containsBeanDefinition(beanName)) {
			RootBeanDefinition definition = new RootBeanDefinition(clazz);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
			constructorArgumentsConsumer.accept(constructorArguments);
			registry.registerBeanDefinition(beanName, definition);
			return definition;
		}
		return registry.getBeanDefinition(beanName);
	}

	private static final class WrapEarlyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
			PriorityOrdered {

		private final BeanOverrideBeanPostProcessor mainProcessor;
		private final Map<String, Object> earlyReferences;

		private WrapEarlyBeanPostProcessor(BeanOverrideBeanPostProcessor mainProcessor) {
			this.mainProcessor = mainProcessor;
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
			return this.mainProcessor.wrapIfNecessary(bean, beanName);
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof FactoryBean) {
				return bean;
			}
			if (this.earlyReferences.remove(getCacheKey(bean, beanName)) != bean) {
				return this.mainProcessor.wrapIfNecessary(bean, beanName);
			}
			return bean;
		}

		private String getCacheKey(Object bean, String beanName) {
			return StringUtils.hasLength(beanName) ? beanName : bean.getClass().getName();
		}

	}
}
