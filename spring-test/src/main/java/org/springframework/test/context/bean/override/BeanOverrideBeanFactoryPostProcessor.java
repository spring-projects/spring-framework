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
import java.util.Set;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.aot.AbstractAotProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link BeanFactoryPostProcessor} implementation that processes identified
 * use of {@link BeanOverride @BeanOverride} and adapts the {@link BeanFactory}
 * accordingly.
 *
 * <p>For each override, the bean factory is prepared according to the chosen
 * {@linkplain BeanOverrideStrategy override strategy}. The bean override instance
 * is created, if necessary, and the related infrastructure is updated to allow
 * the bean override instance to be injected into the corresponding
 * {@linkplain BeanOverrideHandler#getField() field} of the test class.
 *
 * <p>This processor does not work against a particular test class but rather
 * only prepares the bean factory for the identified, unique set of bean overrides.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.2
 */
class BeanOverrideBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {

	private static final String PSEUDO_BEAN_NAME_PLACEHOLDER = "<<< PSEUDO BEAN NAME PLACEHOLDER >>>";

	private static final BeanNameGenerator beanNameGenerator = DefaultBeanNameGenerator.INSTANCE;

	private final Set<BeanOverrideHandler> beanOverrideHandlers;

	private final BeanOverrideRegistry beanOverrideRegistry;


	/**
	 * Create a new {@code BeanOverrideBeanFactoryPostProcessor} with the supplied
	 * set of {@link BeanOverrideHandler BeanOverrideHandlers} to process, using
	 * the given {@link BeanOverrideRegistry}.
	 * @param beanOverrideHandlers the bean override handlers to process
	 * @param beanOverrideRegistry the registry used to track bean override handlers
	 */
	BeanOverrideBeanFactoryPostProcessor(Set<BeanOverrideHandler> beanOverrideHandlers,
			BeanOverrideRegistry beanOverrideRegistry) {

		this.beanOverrideHandlers = beanOverrideHandlers;
		this.beanOverrideRegistry = beanOverrideRegistry;
	}


	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 10;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		for (BeanOverrideHandler handler : this.beanOverrideHandlers) {
			registerBeanOverride(beanFactory, handler);
		}
	}

	private void registerBeanOverride(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler) {
		String beanName = handler.getBeanName();
		Field field = handler.getField();
		Assert.state(!BeanFactoryUtils.isFactoryDereference(beanName),() -> """
				Unable to override bean '%s' for field '%s.%s': a FactoryBean cannot be overridden. \
				To override the bean created by the FactoryBean, remove the '&' prefix.""".formatted(
					beanName, field.getDeclaringClass().getSimpleName(), field.getName()));

		switch (handler.getStrategy()) {
			case REPLACE -> replaceOrCreateBean(beanFactory, handler, true);
			case REPLACE_OR_CREATE -> replaceOrCreateBean(beanFactory, handler, false);
			case WRAP -> wrapBean(beanFactory, handler);
		}
	}

	private void replaceOrCreateBean(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler,
			boolean requireExistingBean) {

		// NOTE: This method supports 3 distinct scenarios which must be accounted for.
		//
		// 1) JVM runtime
		// 2) AOT processing
		// 3) AOT runtime

		String beanName = handler.getBeanName();
		BeanDefinition existingBeanDefinition = null;
		if (beanName == null) {
			beanName = getBeanNameForType(beanFactory, handler, requireExistingBean);
			if (beanName != null) {
				// We are overriding an existing bean by-type.
				beanName = BeanFactoryUtils.transformedBeanName(beanName);
				// If we are overriding a manually registered singleton, we won't find
				// an existing bean definition.
				if (beanFactory.containsBeanDefinition(beanName)) {
					existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
				}
			}
			else {
				// We will later generate a name for the nonexistent bean, but since NullAway
				// will reject leaving the beanName set to null, we set it to a placeholder.
				beanName = PSEUDO_BEAN_NAME_PLACEHOLDER;
			}
		}
		else {
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, handler, false);
			if (candidates.contains(beanName)) {
				// We are overriding an existing bean by-name.
				existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
			}
			else if (requireExistingBean) {
				throw new IllegalStateException("""
						Unable to override bean: there is no bean to replace \
						with name [%s] and type [%s]."""
							.formatted(beanName, handler.getBeanType()));
			}
		}

		if (existingBeanDefinition != null) {
			// Validate the existing bean definition.
			//
			// Applies during "JVM runtime", "AOT processing", and "AOT runtime".
			validateBeanDefinition(beanFactory, beanName);
		}
		else if (Boolean.getBoolean(AbstractAotProcessor.AOT_PROCESSING)) {
			// There was no existing bean definition, but during "AOT processing" we
			// do not register the "pseudo" bean definition since our AOT support
			// cannot automatically convert that to a functional bean definition for
			// use at "AOT runtime". Furthermore, by not registering a bean definition
			// for a nonexistent bean, we allow the "JVM runtime" and "AOT runtime"
			// to operate the same in the following else-block.
		}
		else {
			// There was no existing bean definition, so we register a "pseudo" bean
			// definition to ensure that a suitable bean definition exists for the given
			// bean name for proper autowiring candidate resolution.
			//
			// Applies during "JVM runtime" and "AOT runtime".

			if (!(beanFactory instanceof BeanDefinitionRegistry registry)) {
				throw new IllegalStateException("Cannot process bean override with a BeanFactory " +
						"that doesn't implement BeanDefinitionRegistry: " + beanFactory.getClass().getName());
			}

			RootBeanDefinition pseudoBeanDefinition = createPseudoBeanDefinition(handler);

			// Generate a name for the nonexistent bean.
			if (PSEUDO_BEAN_NAME_PLACEHOLDER.equals(beanName)) {
				beanName = beanNameGenerator.generateBeanName(pseudoBeanDefinition, registry);
			}

			registry.registerBeanDefinition(beanName, pseudoBeanDefinition);
		}

		Object override = handler.createOverrideInstance(beanName, existingBeanDefinition, null, beanFactory);
		this.beanOverrideRegistry.registerBeanOverrideHandler(handler, beanName);

		// Now we have an instance (the override) that we can manually register as a singleton.
		//
		// However, we need to remove any existing singleton instance -- for example, a
		// manually registered singleton or a singleton that was registered as a side effect
		// of the isSingleton() check in validateBeanDefinition().
		//
		// As a bonus, by manually registering a singleton during "AOT processing", we allow
		// GenericApplicationContext's preDetermineBeanType() method to transparently register
		// runtime hints for a proxy generated by the above createOverrideInstance() invocation --
		// for example, when @MockitoBean creates a mock based on a JDK dynamic proxy.
		if (beanFactory.containsSingleton(beanName)) {
			destroySingleton(beanFactory, beanName);
		}
		beanFactory.registerSingleton(beanName, override);
	}

	/**
	 * Check that a bean with the specified {@link BeanOverrideHandler#getBeanName() name}
	 * and {@link BeanOverrideHandler#getBeanType() type} is registered.
	 * <p>If so, put the {@link BeanOverrideHandler} in the early tracking map.
	 * <p>The map will later be checked to see if a given bean should be wrapped
	 * upon creation, during the {@link WrapEarlyBeanPostProcessor#getEarlyBeanReference}
	 * phase.
	 */
	private void wrapBean(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler) {
		String beanName = handler.getBeanName();
		if (beanName == null) {
			Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, handler, true);
			int candidateCount = candidateNames.size();
			if (candidateCount != 1) {
				Field field = handler.getField();
				throw new IllegalStateException("""
						Unable to select a bean to override by wrapping: found %d bean instances of type %s \
						(as required by annotated field '%s.%s')%s"""
						.formatted(candidateCount, handler.getBeanType(),
							field.getDeclaringClass().getSimpleName(), field.getName(),
							(candidateCount > 0 ? ": " + candidateNames : "")));
			}
			beanName = BeanFactoryUtils.transformedBeanName(candidateNames.iterator().next());
		}
		else {
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, handler, false);
			if (!candidates.contains(beanName)) {
				throw new IllegalStateException("""
						Unable to override bean by wrapping: there is no existing bean \
						with name [%s] and type [%s]."""
							.formatted(beanName, handler.getBeanType()));
			}
		}
		validateBeanDefinition(beanFactory, beanName);
		this.beanOverrideRegistry.registerBeanOverrideHandler(handler, beanName);
	}

	@Nullable
	private String getBeanNameForType(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler,
			boolean requireExistingBean) {

		Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, handler, true);
		int candidateCount = candidateNames.size();
		if (candidateCount == 1) {
			return candidateNames.iterator().next();
		}
		else if (candidateCount == 0) {
			if (requireExistingBean) {
				Field field = handler.getField();
				throw new IllegalStateException(
						"Unable to override bean: no beans of type %s (as required by annotated field '%s.%s')"
							.formatted(handler.getBeanType(), field.getDeclaringClass().getSimpleName(), field.getName()));
			}
			return null;
		}

		Field field = handler.getField();
		throw new IllegalStateException("""
				Unable to select a bean to override: found %s beans of type %s \
				(as required by annotated field '%s.%s'): %s"""
					.formatted(candidateCount, handler.getBeanType(), field.getDeclaringClass().getSimpleName(),
						field.getName(), candidateNames));
	}

	private Set<String> getExistingBeanNamesByType(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler,
			boolean checkAutowiredCandidate) {

		ResolvableType resolvableType = handler.getBeanType();
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
			DependencyDescriptor descriptor = new DependencyDescriptor(handler.getField(), true);
			beanNames.removeIf(beanName -> !beanFactory.isAutowireCandidate(beanName, descriptor));
		}
		// Filter out scoped proxy targets.
		beanNames.removeIf(ScopedProxyUtils::isScopedTarget);

		// In case of multiple matches, fall back on the field's name as a last resort.
		if (beanNames.size() > 1) {
			String fieldName = handler.getField().getName();
			if (beanNames.contains(fieldName)) {
				return Set.of(fieldName);
			}
		}
		return beanNames;
	}

	/**
	 * Create a pseudo-{@link BeanDefinition} for the supplied {@link BeanOverrideHandler},
	 * whose {@linkplain RootBeanDefinition#getTargetType() target type} and
	 * {@linkplain RootBeanDefinition#getQualifiedElement() qualified element} are
	 * the {@linkplain BeanOverrideHandler#getBeanType() bean type} and
	 * the {@linkplain BeanOverrideHandler#getField() field} of the {@code BeanOverrideHandler},
	 * respectively.
	 * <p>The returned bean definition should <strong>not</strong> be used to create
	 * a bean instance but rather only for the purpose of having suitable bean
	 * definition metadata available in the {@link BeanFactory} &mdash; for example,
	 * for autowiring candidate resolution.
	 */
	private static RootBeanDefinition createPseudoBeanDefinition(BeanOverrideHandler handler) {
		RootBeanDefinition definition = new RootBeanDefinition(handler.getBeanType().resolve());
		definition.setTargetType(handler.getBeanType());
		definition.setQualifiedElement(handler.getField());
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

	private static void destroySingleton(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (!(beanFactory instanceof DefaultListableBeanFactory dlbf)) {
			throw new IllegalStateException("Cannot process bean override with a BeanFactory " +
					"that doesn't implement DefaultListableBeanFactory: " + beanFactory.getClass().getName());
		}
		dlbf.destroySingleton(beanName);
	}

}
