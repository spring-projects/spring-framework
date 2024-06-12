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

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} implementation that processes identified
 * use of {@link BeanOverride} and adapts the {@link BeanFactory} accordingly.
 *
 * <p>For each override, the bean factory is prepared according to the chosen
 * {@link BeanOverrideStrategy overriding strategy}. The override value is created,
 * if necessary, and the necessary infrastructure is updated to allow the value
 * to be injected in the corresponding {@linkplain OverrideMetadata#getField() field}
 * of the test class.
 *
 * <p>This processor does not work against a particular test class, it only prepares
 * the bean factory for the identified, unique, set of bean overrides.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
class BeanOverrideBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

	private final Set<OverrideMetadata> metadata;

	private final BeanOverrideRegistrar overrideRegistrar;

	private final BeanNameGenerator beanNameGenerator = new DefaultBeanNameGenerator();


	/**
	 * Create a new {@code BeanOverrideBeanFactoryPostProcessor} instance with
	 * the set of {@link OverrideMetadata} to process, using the given
	 * {@link BeanOverrideRegistrar}.
	 * @param metadata the {@link OverrideMetadata} instances to process
	 * @param overrideRegistrar the {@link BeanOverrideRegistrar} used to track
	 * metadata
	 */
	public BeanOverrideBeanFactoryPostProcessor(Set<OverrideMetadata> metadata,
			BeanOverrideRegistrar overrideRegistrar) {

		this.metadata = metadata;
		this.overrideRegistrar = overrideRegistrar;
	}


	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
			throw new IllegalStateException("Cannot process bean override with a BeanFactory " +
					"that doesn't implement BeanDefinitionRegistry: " + beanFactory.getClass());
		}
		postProcessWithRegistry(beanFactory, registry);
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	private void postProcessWithRegistry(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry) {
		for (OverrideMetadata metadata : this.metadata) {
			registerBeanOverride(beanFactory, registry, metadata);
		}
	}

	/**
	 * Copy certain details of a {@link BeanDefinition} to the definition created by
	 * this processor for a given {@link OverrideMetadata}.
	 * <p>The default implementation copies the {@linkplain BeanDefinition#isPrimary()
	 * primary flag}, @{@linkplain BeanDefinition#isFallback() fallback flag}
	 * and the {@linkplain BeanDefinition#getScope() scope}.
	 */
	protected void copyBeanDefinitionDetails(BeanDefinition from, RootBeanDefinition to) {
		to.setPrimary(from.isPrimary());
		to.setFallback(from.isFallback());
		to.setScope(from.getScope());
	}

	private void registerBeanOverride(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			OverrideMetadata overrideMetadata) {

		switch (overrideMetadata.getStrategy()) {
			case REPLACE_DEFINITION ->
					registerReplaceDefinition(beanFactory, registry, overrideMetadata, true);
			case REPLACE_OR_CREATE_DEFINITION ->
					registerReplaceDefinition(beanFactory, registry, overrideMetadata, false);
			case WRAP_BEAN -> registerWrapBean(beanFactory, overrideMetadata);
		}
	}

	private void registerReplaceDefinition(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			OverrideMetadata overrideMetadata, boolean enforceExistingDefinition) {

		RootBeanDefinition beanDefinition = createBeanDefinition(overrideMetadata);
		String beanName = overrideMetadata.getBeanName();
		String beanNameIncludingFactory;
		BeanDefinition existingBeanDefinition = null;
		if (beanName == null) {
			beanNameIncludingFactory = getBeanNameForType(beanFactory, registry, overrideMetadata, beanDefinition, enforceExistingDefinition);
			beanName = BeanFactoryUtils.transformedBeanName(beanNameIncludingFactory);
			if (registry.containsBeanDefinition(beanName)) {
				existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
			}
		}
		else {
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, overrideMetadata, false);
			if (candidates.contains(beanName)) {
				existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
			}
			else if (enforceExistingDefinition) {
				throw new IllegalStateException("Unable to override bean '" + beanName + "': there is no " +
						"bean definition to replace with that name of type " + overrideMetadata.getBeanType());
			}
			beanNameIncludingFactory = beanName;
		}

		if (existingBeanDefinition != null) {
			copyBeanDefinitionDetails(existingBeanDefinition, beanDefinition);
			registry.removeBeanDefinition(beanName);
		}
		registry.registerBeanDefinition(beanName, beanDefinition);

		Object override = overrideMetadata.createOverride(beanName, existingBeanDefinition, null);
		if (beanFactory.isSingleton(beanNameIncludingFactory)) {
			// Now we have an instance (the override) that we can register.
			// At this stage we don't expect a singleton instance to be present,
			// and this call will throw if there is such an instance already.
			beanFactory.registerSingleton(beanName, override);
		}

		overrideMetadata.track(override, beanFactory);
		this.overrideRegistrar.registerNameForMetadata(overrideMetadata, beanNameIncludingFactory);
	}

	private String getBeanNameForType(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			OverrideMetadata overrideMetadata, RootBeanDefinition beanDefinition, boolean enforceExistingDefinition) {
		Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, overrideMetadata, true);
		int candidateCount = candidateNames.size();
		if (candidateCount == 1) {
			return candidateNames.iterator().next();
		}
		else if (candidateCount == 0) {
			if (enforceExistingDefinition) {
				Field field = overrideMetadata.getField();
				throw new IllegalStateException(
						"Unable to override bean: no bean definitions of type %s (as required by annotated field '%s.%s')"
								.formatted(overrideMetadata.getBeanType(), field.getDeclaringClass().getSimpleName(), field.getName()));
			}
			return this.beanNameGenerator.generateBeanName(beanDefinition, registry);
		}
		Field field = overrideMetadata.getField();
		throw new IllegalStateException(String.format(
				"Unable to select a bean definition to override: found %s bean definitions of type %s " +
						"(as required by annotated field '%s.%s'): %s",
				candidateCount, overrideMetadata.getBeanType(), field.getDeclaringClass().getSimpleName(),
				field.getName(), candidateNames));
	}

	/**
	 * Check that the expected bean name is registered and matches the type to override.
	 * <p>If so, put the override metadata in the early tracking map.
	 * <p>The map will later be checked to see if a given bean should be wrapped
	 * upon creation, during the {@link WrapEarlyBeanPostProcessor#getEarlyBeanReference(Object, String)}
	 * phase.
	 */
	private void registerWrapBean(ConfigurableListableBeanFactory beanFactory, OverrideMetadata metadata) {
		String beanName = metadata.getBeanName();
		if (beanName == null) {
			Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, metadata, true);
			int candidateCount = candidateNames.size();
			if (candidateCount != 1) {
				Field field = metadata.getField();
				throw new IllegalStateException("Unable to select a bean to override by wrapping: found " +
						candidateCount + " bean instances of type " + metadata.getBeanType() +
						" (as required by annotated field '" + field.getDeclaringClass().getSimpleName() +
						"." + field.getName() + "')" + (candidateCount > 0 ? ": " + candidateNames : ""));
			}
			beanName = BeanFactoryUtils.transformedBeanName(candidateNames.iterator().next());
		}
		else {
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, metadata, false);
			if (!candidates.contains(beanName)) {
				throw new IllegalStateException("Unable to override bean '" + beanName + "' by wrapping: there is no " +
						"existing bean instance with that name of type " + metadata.getBeanType());
			}
		}
		this.overrideRegistrar.markWrapEarly(metadata, beanName);
		this.overrideRegistrar.registerNameForMetadata(metadata, beanName);
	}

	RootBeanDefinition createBeanDefinition(OverrideMetadata metadata) {
		RootBeanDefinition definition = new RootBeanDefinition(metadata.getBeanType().resolve());
		definition.setTargetType(metadata.getBeanType());
		definition.setQualifiedElement(metadata.getField());
		return definition;
	}

	private Set<String> getExistingBeanNamesByType(ConfigurableListableBeanFactory beanFactory, OverrideMetadata metadata,
			boolean checkAutowiredCandidate) {

		ResolvableType resolvableType = metadata.getBeanType();
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
		if (checkAutowiredCandidate) {
			DependencyDescriptor descriptor = new DependencyDescriptor(metadata.getField(), true);
			beans.removeIf(beanName -> ScopedProxyUtils.isScopedTarget(beanName) ||
					!beanFactory.isAutowireCandidate(beanName, descriptor));
		}
		else {
			beans.removeIf(ScopedProxyUtils::isScopedTarget);
		}
		// In case of multiple matches, last resort fallback on the field's name
		if (beans.size() > 1) {
			String fieldName = metadata.getField().getName();
			if (beans.contains(fieldName)) {
				return Set.of(fieldName);
			}
		}
		return beans;
	}


	static class WrapEarlyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor,
			PriorityOrdered {

		private final Map<String, Object> earlyReferences = new ConcurrentHashMap<>(16);

		private final BeanOverrideRegistrar overrideRegistrar;


		WrapEarlyBeanPostProcessor(BeanOverrideRegistrar registrar) {
			this.overrideRegistrar = registrar;
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
