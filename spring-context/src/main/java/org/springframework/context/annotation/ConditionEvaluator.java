/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * Utility class used to evaluate {@link Conditional} annotations.
 *
 * @author Phillip Webb
 * @since 4.0
 */
abstract class ConditionEvaluator {

	private static final String CONDITIONAL_ANNOTATION = Conditional.class.getName();

	private static final ConditionEvaluator NONE = new ConditionEvaluator() {

		@Override
		public boolean shouldSkip(BeanDefinitionRegistry registry, Environment environment) {
			return false;
		}

	};


	/**
	 * Evaluate if any condition does not match and hence registration should be skipped.
	 * @param registry the registry or {@code null}
	 * @param environment the environment or {@code null}
	 * @return if the registration should be skipped
	 */
	public abstract boolean shouldSkip(BeanDefinitionRegistry registry,
			Environment environment);


	/**
	 * Returns a {@link ConditionEvaluator} instance of the specified metadata.
	 * @param metadata the metadata to test
	 * @param deferIfConfigurationCandidate if the evaluator should be deferred when the
	 *        metadata is from a {@code @Configuration} candidate.
	 * @return the evaluator instance
	 */
	public static ConditionEvaluator get(AnnotatedTypeMetadata metadata,
			boolean deferIfConfigurationCandidate) {
		if (metadata == null || !metadata.isAnnotated(CONDITIONAL_ANNOTATION)) {
			// Shortcut to save always creating a ConditionEvaluator
			return NONE;
		}

		// Defer @Conditional @Configuration classes until later when the
		// ConfigurationClassPostProcessor will evaluate them. Allows @Conditional
		// implementations that inspect beans created by @Configuration to work
		if (deferIfConfigurationCandidate && metadata instanceof AnnotationMetadata
				&& ConfigurationClassUtils.isConfigurationCandidate((AnnotationMetadata) metadata)) {
			return NONE;
		}

		return new ConditionEvaluatorImpl(metadata);
	}


	/**
	 * Implementation of {@link ConditionEvaluator}.
	 */
	private static class ConditionEvaluatorImpl extends ConditionEvaluator {

		private AnnotatedTypeMetadata metadata;


		public ConditionEvaluatorImpl(AnnotatedTypeMetadata metadata) {
			this.metadata = metadata;
		}


		@Override
		public boolean shouldSkip(BeanDefinitionRegistry registry, Environment environment) {
			ConditionContext context = new ConditionContextImpl(registry, environment);
			if (this.metadata != null) {
				for (String[] conditionClasses : getConditionClasses(metadata)) {
					for (String conditionClass : conditionClasses) {
						if (!getCondition(conditionClass, context.getClassLoader()).matches(
								context, metadata)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@SuppressWarnings("unchecked")
		private static List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
			MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(
					CONDITIONAL_ANNOTATION, true);
			Object values = attributes == null ? null : attributes.get("value");
			return (List<String[]>) (values == null ? Collections.emptyList() : values);
		}

		private static Condition getCondition(String conditionClassName,
				ClassLoader classloader) {
			Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName,
					classloader);
			return (Condition) BeanUtils.instantiateClass(conditionClass);
		}
	}

	/**
	 * Implementation of a {@link ConditionContext}.
	 */
	private static class ConditionContextImpl implements ConditionContext {

		private BeanDefinitionRegistry registry;

		private ConfigurableListableBeanFactory beanFactory;

		private Environment environment;


		public ConditionContextImpl(BeanDefinitionRegistry registry,
				Environment environment) {
			this.registry = registry;
			this.beanFactory = deduceBeanFactory(registry);
			this.environment = environment;
			if (this.environment == null) {
				this.environment = deduceEnvironment(registry);
			}
		}


		private ConfigurableListableBeanFactory deduceBeanFactory(Object source) {
			if (source == null) {
				return null;
			}
			if (source instanceof ConfigurableListableBeanFactory) {
				return (ConfigurableListableBeanFactory) source;
			}
			else if (source instanceof ConfigurableApplicationContext) {
				return deduceBeanFactory(((ConfigurableApplicationContext) source).getBeanFactory());
			}
			return null;
		}

		private Environment deduceEnvironment(BeanDefinitionRegistry registry) {
			if (registry == null) {
				return null;
			}
			if (registry instanceof EnvironmentCapable) {
				return ((EnvironmentCapable) registry).getEnvironment();
			}
			return null;
		}

		@Override
		public BeanDefinitionRegistry getRegistry() {
			return this.registry;
		}

		@Override
		public Environment getEnvironment() {
			return this.environment;
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			Assert.state(this.beanFactory != null, "Unable to locate the BeanFactory");
			return this.beanFactory;
		}

		@Override
		public ResourceLoader getResourceLoader() {
			if (registry instanceof ResourceLoader) {
				return (ResourceLoader) registry;
			}
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			ResourceLoader resourceLoader = getResourceLoader();
			return (resourceLoader == null ? null : resourceLoader.getClassLoader());
		}
	}

}
