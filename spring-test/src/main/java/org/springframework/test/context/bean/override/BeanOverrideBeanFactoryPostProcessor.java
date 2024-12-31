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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
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
import org.springframework.util.Assert;

/**
 * A {@link BeanFactoryPostProcessor} implementation that processes identified
 * use of {@link BeanOverride @BeanOverride} and adapts the {@code BeanFactory}
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
		Set<String> generatedBeanNames = new HashSet<>();
		for (BeanOverrideHandler handler : this.beanOverrideHandlers) {
			registerBeanOverride(beanFactory, handler, generatedBeanNames);
		}
	}

	private void registerBeanOverride(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler,
			Set<String> generatedBeanNames) {

		String beanName = handler.getBeanName();
		Field field = handler.getField();
		Assert.state(!BeanFactoryUtils.isFactoryDereference(beanName),() -> """
				Unable to override bean '%s' for field '%s.%s': a FactoryBean cannot be overridden. \
				To override the bean created by the FactoryBean, remove the '&' prefix.""".formatted(
					beanName, field.getDeclaringClass().getSimpleName(), field.getName()));

		switch (handler.getStrategy()) {
			case REPLACE -> replaceOrCreateBean(beanFactory, handler, generatedBeanNames, true);
			case REPLACE_OR_CREATE -> replaceOrCreateBean(beanFactory, handler, generatedBeanNames, false);
			case WRAP -> wrapBean(beanFactory, handler);
		}
	}

	private void replaceOrCreateBean(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler,
			Set<String> generatedBeanNames, boolean requireExistingBean) {

		// NOTE: This method supports 3 distinct scenarios which must be accounted for.
		//
		// - JVM runtime
		// - AOT processing
		// - AOT runtime
		//
		// In addition, this method supports 4 distinct use cases.
		//
		// 1) Override existing bean by-type
		// 2) Create bean by-type, with a generated name
		// 3) Override existing bean by-name
		// 4) Create bean by-name, with a provided name

		String beanName = handler.getBeanName();
		Field field = handler.getField();
		BeanDefinition existingBeanDefinition = null;
		if (beanName == null) {
			beanName = getBeanNameForType(beanFactory, handler, requireExistingBean);
			// If the generatedBeanNames set already contains the beanName that we
			// just found by-type, that means we are experiencing a "phantom read"
			// (i.e., we found a bean that was not previously there). Consequently,
			// we cannot "override the override", because we would lose one of the
			// overrides. Instead, we must create a new override for the current
			// handler. For example, if one handler creates an override for a SubType
			// and a subsequent handler creates an override for a SuperType of that
			// SubType, we must end up with overrides for both SuperType and SubType.
			if (beanName != null && !generatedBeanNames.contains(beanName)) {
				// 1) We are overriding an existing bean by-type.
				beanName = BeanFactoryUtils.transformedBeanName(beanName);
				// If we are overriding a manually registered singleton, we won't find
				// an existing bean definition.
				if (beanFactory.containsBeanDefinition(beanName)) {
					existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
				}
			}
			else {
				// 2) We are creating a bean by-type, with a generated name.
				// Since NullAway will reject leaving the beanName set to null,
				// we set it to a placeholder that will be replaced later.
				beanName = PSEUDO_BEAN_NAME_PLACEHOLDER;
			}
		}
		else {
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, handler, false);
			if (candidates.contains(beanName)) {
				// 3) We are overriding an existing bean by-name.
				existingBeanDefinition = beanFactory.getBeanDefinition(beanName);
			}
			else if (requireExistingBean) {
				throw new IllegalStateException("""
						Unable to replace bean: there is no bean with name '%s' and type %s \
						(as required by field '%s.%s')."""
							.formatted(beanName, handler.getBeanType(),
								field.getDeclaringClass().getSimpleName(), field.getName()));
			}
			// 4) We are creating a bean by-name with the provided beanName.
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
						"that does not implement BeanDefinitionRegistry: " + beanFactory.getClass().getName());
			}

			RootBeanDefinition pseudoBeanDefinition = createPseudoBeanDefinition(handler);

			// Generate a name for the nonexistent bean.
			if (PSEUDO_BEAN_NAME_PLACEHOLDER.equals(beanName)) {
				beanName = beanNameGenerator.generateBeanName(pseudoBeanDefinition, registry);
				generatedBeanNames.add(beanName);
			}

			registry.registerBeanDefinition(beanName, pseudoBeanDefinition);
		}

		Object override = handler.createOverrideInstance(beanName, existingBeanDefinition, null, beanFactory);
		this.beanOverrideRegistry.registerBeanOverrideHandler(handler, beanName);

		// Now we have an instance (the override) that we can manually register as a singleton.
		//
		// However, we need to remove any existing singleton instance -- for example, a
		// manually registered singleton.
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
	 * or {@link BeanOverrideHandler#getBeanType() type} has already been registered
	 * in the {@code BeanFactory}.
	 * <p>If so, register the {@link BeanOverrideHandler} and the corresponding bean
	 * name in the {@link BeanOverrideRegistry}.
	 * <p>The registry will later be checked to see if a given bean should be wrapped
	 * upon creation, during the early bean post-processing phase.
	 * @see BeanOverrideRegistry#registerBeanOverrideHandler(BeanOverrideHandler, String)
	 * @see WrapEarlyBeanPostProcessor#getEarlyBeanReference(Object, String)
	 */
	private void wrapBean(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler) {
		String beanName = handler.getBeanName();
		Field field = handler.getField();
		ResolvableType beanType = handler.getBeanType();

		if (beanName == null) {
			// We are wrapping an existing bean by-type.
			Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, handler, true);
			int candidateCount = candidateNames.size();
			if (candidateCount == 1) {
				beanName = candidateNames.iterator().next();
			}
			else {
				String primaryCandidate = determinePrimaryCandidate(beanFactory, candidateNames, beanType.toClass());
				if (primaryCandidate != null) {
					beanName = primaryCandidate;
				}
				else {
					String message = "Unable to select a bean to wrap: ";
					if (candidateCount == 0) {
						message += "there are no beans of type %s (as required by field '%s.%s')."
								.formatted(beanType, field.getDeclaringClass().getSimpleName(), field.getName());
					}
					else {
						message += "found %d beans of type %s (as required by field '%s.%s'): %s"
								.formatted(candidateCount, beanType, field.getDeclaringClass().getSimpleName(),
										field.getName(), candidateNames);
					}
					throw new IllegalStateException(message);
				}
			}
			beanName = BeanFactoryUtils.transformedBeanName(beanName);
		}
		else {
			// We are wrapping an existing bean by-name.
			Set<String> candidates = getExistingBeanNamesByType(beanFactory, handler, false);
			if (!candidates.contains(beanName)) {
				throw new IllegalStateException("""
						Unable to wrap bean: there is no bean with name '%s' and type %s \
						(as required by field '%s.%s')."""
							.formatted(beanName, beanType, field.getDeclaringClass().getSimpleName(),
								field.getName()));
			}
		}

		validateBeanDefinition(beanFactory, beanName);
		this.beanOverrideRegistry.registerBeanOverrideHandler(handler, beanName);
	}

	private @Nullable String getBeanNameForType(ConfigurableListableBeanFactory beanFactory, BeanOverrideHandler handler,
			boolean requireExistingBean) {

		Field field = handler.getField();
		ResolvableType beanType = handler.getBeanType();

		Set<String> candidateNames = getExistingBeanNamesByType(beanFactory, handler, true);
		int candidateCount = candidateNames.size();
		if (candidateCount == 1) {
			return candidateNames.iterator().next();
		}
		else if (candidateCount == 0) {
			if (requireExistingBean) {
				throw new IllegalStateException(
						"Unable to override bean: there are no beans of type %s (as required by field '%s.%s')."
							.formatted(beanType, field.getDeclaringClass().getSimpleName(), field.getName()));
			}
			return null;
		}

		String primaryCandidate = determinePrimaryCandidate(beanFactory, candidateNames, beanType.toClass());
		if (primaryCandidate != null) {
			return primaryCandidate;
		}

		throw new IllegalStateException(
				"Unable to select a bean to override: found %d beans of type %s (as required by field '%s.%s'): %s"
					.formatted(candidateCount, beanType, field.getDeclaringClass().getSimpleName(),
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
			Class<?> producedType = beanFactory.getType(beanName, false);
			if (type.equals(producedType)) {
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
	 * Determine the primary candidate in the given set of bean names.
	 * <p>Honors both <em>primary</em> and <em>fallback</em> semantics.
	 * @return the name of the primary candidate, or {@code null} if none found
	 * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#determinePrimaryCandidate(Map, Class)
	 */
	private static @Nullable String determinePrimaryCandidate(
			ConfigurableListableBeanFactory beanFactory, Set<String> candidateBeanNames, Class<?> beanType) {

		if (candidateBeanNames.isEmpty()) {
			return null;
		}

		String primaryBeanName = null;
		// First pass: identify unique primary candidate
		for (String candidateBeanName : candidateBeanNames) {
			if (beanFactory.containsBeanDefinition(candidateBeanName)) {
				BeanDefinition beanDefinition = beanFactory.getBeanDefinition(candidateBeanName);
				if (beanDefinition.isPrimary()) {
					if (primaryBeanName != null) {
						throw new NoUniqueBeanDefinitionException(beanType, candidateBeanNames.size(),
							"more than one 'primary' bean found among candidates: " + candidateBeanNames);
					}
					primaryBeanName = candidateBeanName;
				}
			}
		}
		// Second pass: identify unique non-fallback candidate
		if (primaryBeanName == null) {
			for (String candidateBeanName : candidateBeanNames) {
				if (beanFactory.containsBeanDefinition(candidateBeanName)) {
					BeanDefinition beanDefinition = beanFactory.getBeanDefinition(candidateBeanName);
					if (!beanDefinition.isFallback()) {
						if (primaryBeanName != null) {
							// More than one non-fallback bean found among candidates.
							return null;
						}
						primaryBeanName = candidateBeanName;
					}
				}
			}
		}
		return primaryBeanName;
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
	 * definition metadata available in the {@code BeanFactory} &mdash; for example,
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
	 * <p>If there is no registered {@code BeanDefinition} for the supplied bean name,
	 * no validation is performed.
	 */
	private static void validateBeanDefinition(ConfigurableListableBeanFactory beanFactory, String beanName) {
		// Due to https://github.com/spring-projects/spring-framework/issues/33800, we do NOT invoke
		// beanFactory.isSingleton(beanName), since doing so can result in a BeanCreationException for
		// certain beans -- for example, a Spring Data FactoryBean for a JpaRepository.
		if (beanFactory.containsBeanDefinition(beanName)) {
			BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
			Assert.state(beanDefinition.isSingleton(),
					() -> "Unable to override bean '" + beanName + "': only singleton beans can be overridden.");
		}
	}

	private static void destroySingleton(ConfigurableListableBeanFactory beanFactory, String beanName) {
		if (!(beanFactory instanceof DefaultListableBeanFactory dlbf)) {
			throw new IllegalStateException("Cannot process bean override with a BeanFactory " +
					"that does not implement DefaultListableBeanFactory: " + beanFactory.getClass().getName());
		}
		dlbf.destroySingleton(beanName);
	}

}
