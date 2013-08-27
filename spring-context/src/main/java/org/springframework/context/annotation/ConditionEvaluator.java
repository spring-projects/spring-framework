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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ConfigurationCondition.ConfigurationPhase;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * Internal class used to evaluate {@link Conditional} annotations.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class ConditionEvaluator {

	private final ConditionContextImpl context;


	/**
	 * Create a new {@link ConditionEvaluator} instance.
	 */
	public ConditionEvaluator(BeanDefinitionRegistry registry, Environment environment,
			ApplicationContext applicationContext, ClassLoader classLoader, ResourceLoader resourceLoader) {

		this.context = new ConditionContextImpl(registry, environment, applicationContext, classLoader, resourceLoader);
	}


	/**
	 * Determine if an item should be skipped based on {@code @Conditional} annotations.
	 * The {@link ConfigurationPhase} will be deduced from the type of item (i.e. a
	 * {@code @Configuration} class will be {@link ConfigurationPhase#PARSE_CONFIGURATION})
	 * @param metadata the meta data
	 * @return if the item should be skipped
	 */
	public boolean shouldSkip(AnnotatedTypeMetadata metadata) {
		return shouldSkip(metadata, null);
	}

	/**
	 * Determine if an item should be skipped based on {@code @Conditional} annotations.
	 * @param metadata the meta data
	 * @param phase the phase of the call
	 * @return if the item should be skipped
	 */
	public boolean shouldSkip(AnnotatedTypeMetadata metadata, ConfigurationPhase phase) {
		if (metadata == null || !metadata.isAnnotated(Conditional.class.getName())) {
			return false;
		}

		if (phase == null) {
			if (metadata instanceof AnnotationMetadata &&
					ConfigurationClassUtils.isConfigurationCandidate((AnnotationMetadata) metadata)) {
				return shouldSkip(metadata, ConfigurationPhase.PARSE_CONFIGURATION);
			}
			return shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN);
		}

		for (String[] conditionClasses : getConditionClasses(metadata)) {
			for (String conditionClass : conditionClasses) {
				Condition condition = getCondition(conditionClass, context.getClassLoader());
				ConfigurationPhase requiredPhase = null;
				if (condition instanceof ConfigurationCondition) {
					requiredPhase = ((ConfigurationCondition) condition).getConfigurationPhase();
				}
				if (requiredPhase == null || requiredPhase == phase) {
					if (!condition.matches(context, metadata)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private List<String[]> getConditionClasses(AnnotatedTypeMetadata metadata) {
		MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(Conditional.class.getName(), true);
		Object values = (attributes != null ? attributes.get("value") : null);
		return (List<String[]>) (values != null ? values : Collections.emptyList());
	}

	private Condition getCondition(String conditionClassName, ClassLoader classloader) {
		Class<?> conditionClass = ClassUtils.resolveClassName(conditionClassName, classloader);
		return (Condition) BeanUtils.instantiateClass(conditionClass);
	}


	/**
	 * Implementation of a {@link ConditionContext}.
	 */
	private static class ConditionContextImpl implements ConditionContext {

		private BeanDefinitionRegistry registry;

		private ConfigurableListableBeanFactory beanFactory;

		private Environment environment;

		private ApplicationContext applicationContext;

		private ClassLoader classLoader;

		private ResourceLoader resourceLoader;

		public ConditionContextImpl(BeanDefinitionRegistry registry,
				Environment environment, ApplicationContext applicationContext,
				ClassLoader classLoader, ResourceLoader resourceLoader) {
			this.registry = registry;
			this.beanFactory = deduceBeanFactory(registry);
			this.environment = environment;
			this.applicationContext = applicationContext;
			this.classLoader = classLoader;
			this.resourceLoader = resourceLoader;
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

		@Override
		public BeanDefinitionRegistry getRegistry() {
			if (this.registry != null) {
				return this.registry;
			}
			if (getBeanFactory() instanceof BeanDefinitionRegistry) {
				return (BeanDefinitionRegistry) getBeanFactory();
			}
			return null;
		}

		@Override
		public Environment getEnvironment() {
			if (this.environment != null) {
				return this.environment;
			}
			if (getRegistry() instanceof EnvironmentCapable) {
				return ((EnvironmentCapable) getRegistry()).getEnvironment();
			}
			return null;
		}

		@Override
		public ConfigurableListableBeanFactory getBeanFactory() {
			Assert.state(this.beanFactory != null, "Unable to locate the BeanFactory");
			return this.beanFactory;
		}

		@Override
		public ResourceLoader getResourceLoader() {
			if (this.resourceLoader != null) {
				return this.resourceLoader;
			}
			if (this.registry instanceof ResourceLoader) {
				return (ResourceLoader) registry;
			}
			return null;
		}

		@Override
		public ClassLoader getClassLoader() {
			if (this.classLoader != null) {
				return this.classLoader;
			}
			if (getResourceLoader() != null) {
				return getResourceLoader().getClassLoader();
			}
			return null;
		}

		@Override
		public ApplicationContext getApplicationContext() {
			if (this.applicationContext != null) {
				return this.applicationContext;
			}
			if (getRegistry() instanceof ApplicationContext) {
				return (ApplicationContext) getRegistry();
			}
			return null;
		}
	}

}
