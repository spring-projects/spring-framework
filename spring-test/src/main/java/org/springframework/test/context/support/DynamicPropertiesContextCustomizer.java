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
import org.springframework.core.env.MutablePropertySources;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link ContextCustomizer} which supports
 * {@link DynamicPropertySource @DynamicPropertySource} methods and registers a
 * {@link DynamicPropertyRegistry} as a singleton bean in the container for use
 * in {@code @Configuration} classes and {@code @Bean} methods.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2.5
 * @see DynamicPropertiesContextCustomizerFactory
 * @see DefaultDynamicPropertyRegistry
 * @see DynamicPropertySourceBeanInitializer
 */
class DynamicPropertiesContextCustomizer implements ContextCustomizer {

	private static final String DYNAMIC_PROPERTY_REGISTRY_BEAN_NAME =
			DynamicPropertiesContextCustomizer.class.getName() + ".dynamicPropertyRegistry";

	private static final String DYNAMIC_PROPERTY_SOURCE_BEAN_INITIALIZER_BEAN_NAME =
			DynamicPropertiesContextCustomizer.class.getName() + ".dynamicPropertySourceBeanInitializer";


	private final Set<Method> methods;


	DynamicPropertiesContextCustomizer(Set<Method> methods) {
		methods.forEach(DynamicPropertiesContextCustomizer::assertValid);
		this.methods = methods;
	}


	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableEnvironment environment = context.getEnvironment();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (!(beanFactory instanceof BeanDefinitionRegistry beanDefinitionRegistry)) {
			throw new IllegalStateException("BeanFactory must be a BeanDefinitionRegistry");
		}

		DefaultDynamicPropertyRegistry dynamicPropertyRegistry =
				new DefaultDynamicPropertyRegistry(environment, this.methods.isEmpty());
		beanFactory.registerSingleton(DYNAMIC_PROPERTY_REGISTRY_BEAN_NAME, dynamicPropertyRegistry);

		if (!beanDefinitionRegistry.containsBeanDefinition(DYNAMIC_PROPERTY_SOURCE_BEAN_INITIALIZER_BEAN_NAME)) {
			BeanDefinition beanDefinition = new RootBeanDefinition(DynamicPropertySourceBeanInitializer.class);
			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			beanDefinitionRegistry.registerBeanDefinition(
					DYNAMIC_PROPERTY_SOURCE_BEAN_INITIALIZER_BEAN_NAME, beanDefinition);
		}

		if (!this.methods.isEmpty()) {
			MutablePropertySources propertySources = environment.getPropertySources();
			propertySources.addFirst(new DynamicValuesPropertySource(dynamicPropertyRegistry.valueSuppliers));
			this.methods.forEach(method -> {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, null, dynamicPropertyRegistry);
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
