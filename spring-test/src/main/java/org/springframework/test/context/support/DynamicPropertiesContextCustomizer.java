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

package org.springframework.test.context.support;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} which supports
 * {@link org.springframework.test.context.DynamicPropertySource @DynamicPropertySource}
 * methods in test classes and registers a {@link DynamicPropertyRegistrarBeanInitializer}
 * in the container to eagerly initialize
 * {@link org.springframework.test.context.DynamicPropertyRegistrar DynamicPropertyRegistrar}
 * beans.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertiesContextCustomizerFactory
 * @see DefaultDynamicPropertyRegistry
 * @see DynamicPropertyRegistrarBeanInitializer
 */
class DynamicPropertiesContextCustomizer implements ContextCustomizer {

	private final Set<Method> methods;


	DynamicPropertiesContextCustomizer(Set<Method> methods) {
		methods.forEach(DynamicPropertiesContextCustomizer::assertValid);
		this.methods = methods;
	}


	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (!(beanFactory instanceof BeanDefinitionRegistry beanDefinitionRegistry)) {
			throw new IllegalStateException("BeanFactory must be a BeanDefinitionRegistry");
		}

		if (!beanDefinitionRegistry.containsBeanDefinition(DynamicPropertyRegistrarBeanInitializer.BEAN_NAME)) {
			BeanDefinition beanDefinition = new RootBeanDefinition(DynamicPropertyRegistrarBeanInitializer.class);
			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			beanDefinitionRegistry.registerBeanDefinition(DynamicPropertyRegistrarBeanInitializer.BEAN_NAME, beanDefinition);
		}

		if (!this.methods.isEmpty()) {
			ConfigurableEnvironment environment = context.getEnvironment();
			DynamicValuesPropertySource propertySource = DynamicValuesPropertySource.getOrCreate(environment);
			DynamicPropertyRegistry registry = propertySource.dynamicPropertyRegistry;
			this.methods.forEach(method -> {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, null, registry);
			});
		}
	}

	Set<Method> getMethods() {
		return this.methods;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof DynamicPropertiesContextCustomizer that &&
				this.methods.equals(that.methods)));
	}

	@Override
	public int hashCode() {
		return this.methods.hashCode();
	}


	private static void assertValid(Method method) {
		Assert.state(Modifier.isStatic(method.getModifiers()),
				() -> "@DynamicPropertySource method '" + method.getName() + "' must be static");
		Class<?>[] types = method.getParameterTypes();
		Assert.state(types.length == 1 && types[0] == DynamicPropertyRegistry.class,
				() -> "@DynamicPropertySource method '" + method.getName() +
						"' must accept a single DynamicPropertyRegistry argument");
	}

}
