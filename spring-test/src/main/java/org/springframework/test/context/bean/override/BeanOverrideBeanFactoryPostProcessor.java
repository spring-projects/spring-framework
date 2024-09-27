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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanFactoryPostProcessor} implementation that processes identified
 * use of {@link BeanOverride @BeanOverride} and adapts the {@link BeanFactory}
 * accordingly.
 *
 * <p>For each override, the bean factory is prepared according to the chosen
 * {@linkplain BeanOverrideStrategy override strategy}. The override value is created,
 * if necessary, and the necessary infrastructure is updated to allow the value
 * to be injected in the corresponding {@linkplain OverrideMetadata#getField() field}
 * of the test class.
 *
 * <p>This processor does not work against a particular test class, but rather
 * only prepares the bean factory for the identified, unique set of bean overrides.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

	private static final BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.INSTANCE;

	private final Set<OverrideMetadata> metadata;

	private final BeanOverrideRegistrar overrideRegistrar;


	/**
	 * Create a new {@code BeanOverrideBeanFactoryPostProcessor} with the supplied
	 * set of {@link OverrideMetadata} to process, using the given
	 * {@link BeanOverrideRegistrar}.
	 * @param metadata the {@link OverrideMetadata} instances to process
	 * @param overrideRegistrar the {@code BeanOverrideRegistrar} used to track
	 * metadata
	 */
	public BeanOverrideBeanFactoryPostProcessor(Set<OverrideMetadata> metadata,
			BeanOverrideRegistrar overrideRegistrar) {

		this.metadata = metadata;
		this.overrideRegistrar = overrideRegistrar;
	}


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
			throw new IllegalStateException("Cannot process bean override with a BeanFactory " +
					"that doesn't implement BeanDefinitionRegistry: " + beanFactory.getClass());
		}

		for (OverrideMetadata metadata : this.metadata) {
			registerBeanOverride(beanFactory, registry, metadata);
		}
	}

	private void registerBeanOverride(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			OverrideMetadata overrideMetadata) {

		switch (overrideMetadata.getStrategy()) {
			case REPLACE_DEFINITION -> replaceDefinition(beanFactory, registry, overrideMetadata, true);
			case REPLACE_OR_CREATE_DEFINITION -> replaceDefinition(beanFactory, registry, overrideMetadata, false);
			case WRAP_BEAN -> wrapBean(beanFactory, overrideMetadata);
		}
	}

	private void replaceDefinition(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
			OverrideMetadata overrideMetadata, boolean enforceExistingDefinition) {

		// The following is a "pseudo" bean definition which MUST NOT be used to
		// create an actual bean instance.
		RootBeanDefinition pseudoBeanDefinition = createPseudoBeanDefinition(overrideMetadata);
		String beanName = overrideMetadata.getBeanName();
		String beanNameIncludingFactory;
		BeanDefinition existingBeanDefinition = null;
		if (beanName == null) {
			beanNameIncludingFactory = getBeanNameForType(
					beanFactory, registry, overrideMetadata, pseudoBeanDefinition, enforceExistingDefinition);
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
				throw new IllegalStateException("""
						Unable to override bean: there is no bean definition to replace \
						with name [%s] and type [%s]."""
							.formatted(beanName, overrideMetadata.getBeanType()));
			}
			beanNameIncludingFactory = beanName;
		}

		// Process existing bean definition.
		if (existingBeanDefinition != null) {
			validateBeanDefinition(beanFactory, beanName);
			copyBeanDefinitionProperties(existingBeanDefinition, pseudoBeanDefinition);
			registry.removeBeanDefinition(beanName);
		}

		// At this point, we either removed an existing bean definition above, or
		// there was no bean definition to begin with. So, we register the pseudo bean
		// definition to ensure that a bean definition exists for the given bean name.
		registry.registerBeanDefinition(beanName, pseudoBeanDefinition);

		Object override = overrideMetadata.createOverride(beanName, existingBeanDefinition, null);
		overrideMetadata.track(override, beanFactory);
		this.overrideRegistrar.registerNameForMetadata(overrideMetadata, beanNameIncludingFactory);

		// Now we have an instance (the override) that we can register. At this stage, we don't
		// expect a singleton instance to be present. If for some reason a singleton instance
		// already exists, the following will throw an exception.
		beanFactory.registerSingleton(beanName, override);
	}

	/**
	 * Check that the expected bean name is registered and matches the type to override.
	 * <p>If so, put the override metadata in the early tracking map.
	 * <p>The map will later be checked to see if a given bean should be wrapped
	 * upon creation, during the {@link WrapEarlyBeanPostProcessor#getEarlyBeanReference(Object, String)}
	 * phase.
	 */
	private void wrapBean(ConfigurableListableBeanFactory beanFactory, OverrideMetadata overrideMetadata) {
		String beanName = overrideMetadata.getBeanName();
		if (beanName == null) {
			Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, overrideMetadata, true);
			int candidateCount = candidateNames.size();
			if (candidateCount != 1) {
				Field field = overrideMetadata.getField();
				throw new IllegalStateException("Unable to select a bean to override by wrapping: found " +
						candidateCount + " bean instances of type " + overrideMetadata.getBeanType() +
						" (as required by annotated field '" + field.getDeclaringClass().getSimpleName() +
						"." + field.getName() + "')" + (candidateCount > 0 ? ": " + candidateNames : ""));
			}
			beanName = BeanFactoryUtils.transformedBeanName(candidateNames.iterator().next());
		}
		else {
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, overrideMetadata, false);
			if (!candidates.contains(beanName)) {
				throw new IllegalStateException("""
						Unable to override bean by wrapping: there is no existing bean definition \
						with name [%s] and type [%s]."""
							.formatted(beanName, overrideMetadata.getBeanType()));
			}
		}
		validateBeanDefinition(beanFactory, beanName);
		this.overrideRegistrar.markWrapEarly(overrideMetadata, beanName);
		this.overrideRegistrar.registerNameForMetadata(overrideMetadata, beanName);
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
			return beanNameGenerator.generateBeanName(beanDefinition, registry);
		}

		Field field = overrideMetadata.getField();
		throw new IllegalStateException("""
				Unable to select a bean definition to override: found %s bean definitions of type %s \
				(as required by annotated field '%s.%s'): %s"""
					.formatted(candidateCount, overrideMetadata.getBeanType(), field.getDeclaringClass().getSimpleName(),
						field.getName(), candidateNames));
	}

	private Set<String> getExistingBeanNamesByType(ConfigurableListableBeanFactory beanFactory, OverrideMetadata metadata,
			boolean checkAutowiredCandidate) {

		ResolvableType resolvableType = metadata.getBeanType();
		Class<?> type = resolvableType.toClass();

		// Start with matching bean names for type, excluding FactoryBeans.
		Set<String> beanNames = new LinkedHashSet<>(
				Arrays.asList(beanFactory.getBeanNamesForType(resolvableType, true, false)));

		// Add matching FactoryBeans as well.
		for (String beanName : beanFactory.getBeanNamesForType(FactoryBean.class, true, false)) {
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			Object attribute = beanDefinition.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
			if (resolvableType.equals(attribute) || type.equals(attribute)) {
				beanNames.add(beanName);
			}
		}

		// Filter out non-matching autowire candidates.
		if (checkAutowiredCandidate) {
			DependencyDescriptor descriptor = new DependencyDescriptor(metadata.getField(), true);
			beanNames.removeIf(beanName -> !beanFactory.isAutowireCandidate(beanName, descriptor));
		}
		// Filter out scoped proxy targets.
		beanNames.removeIf(ScopedProxyUtils::isScopedTarget);

		// In case of multiple matches, fall back on the field's name as a last resort.
		if (beanNames.size() > 1) {
			String fieldName = metadata.getField().getName();
			if (beanNames.contains(fieldName)) {
				return Set.of(fieldName);
			}
		}
		return beanNames;
	}

	/**
	 * Create a pseudo-{@link BeanDefinition} for the supplied {@link OverrideMetadata},
	 * whose {@linkplain RootBeanDefinition#getTargetType() target type} and
	 * {@linkplain RootBeanDefinition#getQualifiedElement() qualified element} are
	 * the {@linkplain OverrideMetadata#getBeanType() bean type} and
	 * the {@linkplain OverrideMetadata#getField() field} of the {@code OverrideMetadata},
	 * respectively.
	 * <p>The returned bean definition should <strong>not</strong> be used to create
	 * a bean instance but rather only for the purpose of having suitable bean
	 * definition metadata available in the {@link BeanFactory} &mdash; for example,
	 * for autowiring candidate resolution.
	 */
	private static RootBeanDefinition createPseudoBeanDefinition(OverrideMetadata metadata) {
		RootBeanDefinition definition = new RootBeanDefinition(metadata.getBeanType().resolve());
		definition.setTargetType(metadata.getBeanType());
		definition.setQualifiedElement(metadata.getField());
		return definition;
	}

	/**
	 * Validate that the {@link BeanDefinition} for the supplied bean name is suitable
	 * for being replaced by a bean override.
	 */
	private static void validateBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName) {
		Assert.state(beanFactory.isSingleton(beanName),
				() -> "Unable to override bean '" + beanName + "': only singleton beans can be overridden.");
	}

	/**
	 * Copy the following properties of the source {@link BeanDefinition} to the
	 * target: the {@linkplain BeanDefinition#isPrimary() primary flag} and the
	 * {@linkplain BeanDefinition#isFallback() fallback flag}.
	 */
	private static void copyBeanDefinitionProperties(BeanDefinition source, RootBeanDefinition target) {
		target.setPrimary(source.isPrimary());
		target.setFallback(source.isFallback());
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
