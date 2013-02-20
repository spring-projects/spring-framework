/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * Helper class used to determine if registration should be skipped based due to a
 * {@code @Conditional} annotation.
 *
 * @author Phillip Webb
 * @since 3.2
 * @see Conditional
 */
abstract class ConditionalAnnotationHelper {

	private static final String CONDITIONAL_ANNOTATION = Conditional.class.getName();


	public static boolean shouldSkip(BeanDefinition beanDefinition,
			BeanDefinitionRegistry registry, Environment environment,
			BeanNameGenerator beanNameGenerator) {
		if (hasCondition(getMetadata(beanDefinition))) {
			ConditionContextImpl context = new ConditionContextImpl(registry,
					environment, beanNameGenerator);
			return shouldSkip(getMetadata(beanDefinition), context);
		}
		return false;
	}

	public static boolean shouldSkip(BeanMethod beanMethod,
			BeanDefinitionRegistry registry, Environment environment,
			BeanNameGenerator beanNameGenerator) {
		if (hasCondition(getMetadata(beanMethod))) {
			ConditionContextImpl context = new ConditionContextImpl(registry,
					environment, beanNameGenerator);
			return shouldSkip(getMetadata(beanMethod), context);
		}
		return false;
	}

	public static boolean shouldSkip(ConfigurationClass configurationClass,
			BeanDefinitionRegistry registry, Environment environment,
			BeanNameGenerator beanNameGenerator) {
		if (hasCondition(configurationClass)) {
			ConditionContextImpl context = new ConditionContextImpl(registry,
					environment, beanNameGenerator);
			return shouldSkip(configurationClass, context);
		}
		return false;
	}

	public static boolean shouldSkip(ConfigurationClass configClass,
			ConditionContextImpl context) {
		if (configClass == null) {
			return false;
		}
		return shouldSkip(configClass.getMetadata(), context)
				|| shouldSkip(configClass.getImportedBy(), context);
	}

	private static boolean shouldSkip(AnnotatedTypeMetadata metadata,
			ConditionContextImpl context) {
		if (metadata != null) {
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

	private static AnnotatedTypeMetadata getMetadata(BeanMethod beanMethod) {
		return (beanMethod == null ? null : beanMethod.getMetadata());
	}

	private static AnnotatedTypeMetadata getMetadata(BeanDefinition beanDefinition) {
		if (beanDefinition != null && beanDefinition instanceof AnnotatedBeanDefinition) {
			return ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
		}
		return null;
	}

	private static boolean hasCondition(ConfigurationClass configurationClass) {
		if (configurationClass == null) {
			return false;
		}
		return hasCondition(configurationClass.getMetadata())
				|| hasCondition(configurationClass.getImportedBy());
	}

	private static boolean hasCondition(AnnotatedTypeMetadata metadata) {
		return (metadata != null) && metadata.isAnnotated(CONDITIONAL_ANNOTATION);
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


	/**
	 * Implementation of a {@link ConditionContext}.
	 */
	private static class ConditionContextImpl implements ConditionContext {

		private BeanDefinitionRegistry registry;

		private ConfigurableListableBeanFactory beanFactory;

		private Environment environment;


		public ConditionContextImpl(BeanDefinitionRegistry registry,
				Environment environment, BeanNameGenerator beanNameGenerator) {
			Assert.notNull(registry, "Registry must not be null");
			this.registry = registry;
			this.beanFactory = deduceBeanFactory(registry);
			this.environment = environment;
			if (this.environment == null) {
				this.environment = deduceEnvironment(registry);
			}
		}


		private ConfigurableListableBeanFactory deduceBeanFactory(Object source) {
			if (source instanceof ConfigurableListableBeanFactory) {
				return (ConfigurableListableBeanFactory) source;
			}
			else if (source instanceof ConfigurableApplicationContext) {
				return deduceBeanFactory(((ConfigurableApplicationContext) source).getBeanFactory());
			}
			return null;
		}

		private Environment deduceEnvironment(BeanDefinitionRegistry registry) {
			if (registry instanceof EnvironmentCapable) {
				return ((EnvironmentCapable) registry).getEnvironment();
			}
			return null;
		}

		public BeanDefinitionRegistry getRegistry() {
			return this.registry;
		}

		public Environment getEnvironment() {
			return this.environment;
		}

		public ConfigurableListableBeanFactory getBeanFactory() {
			Assert.state(this.beanFactory != null, "Unable to locate the BeanFactory");
			return this.beanFactory;
		}

		public ResourceLoader getResourceLoader() {
			if (registry instanceof ResourceLoader) {
				return (ResourceLoader) registry;
			}
			return null;
		}

		public ClassLoader getClassLoader() {
			ResourceLoader resourceLoader = getResourceLoader();
			return (resourceLoader == null ? null : resourceLoader.getClassLoader());
		}
	}

}
