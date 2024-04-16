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

import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.bean.override.BeanOverrideBeanFactoryPostProcessor.WrapEarlyBeanPostProcessor;

/**
 * {@link ContextCustomizer} implementation that registers the necessary
 * infrastructure to support {@linkplain BeanOverride bean overriding}.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 */
class BeanOverrideContextCustomizer implements ContextCustomizer {

	private static final String REGISTRAR_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalBeanOverrideRegistrar";

	private static final String INFRASTRUCTURE_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalBeanOverridePostProcessor";

	private static final String EARLY_INFRASTRUCTURE_BEAN_NAME =
			"org.springframework.test.context.bean.override.internalWrapEarlyBeanPostProcessor";


	private final Set<Class<?>> detectedClasses;

	BeanOverrideContextCustomizer(Set<Class<?>> detectedClasses) {
		this.detectedClasses = detectedClasses;
	}

	static void registerInfrastructure(BeanDefinitionRegistry registry, Set<Class<?>> detectedClasses) {
		addInfrastructureBeanDefinition(registry, BeanOverrideRegistrar.class, REGISTRAR_BEAN_NAME,
				constructorArgs -> constructorArgs.addIndexedArgumentValue(0, detectedClasses));
		RuntimeBeanReference registrarReference = new RuntimeBeanReference(REGISTRAR_BEAN_NAME);
		addInfrastructureBeanDefinition(
				registry, WrapEarlyBeanPostProcessor.class, EARLY_INFRASTRUCTURE_BEAN_NAME,
				constructorArgs -> constructorArgs.addIndexedArgumentValue(0, registrarReference));
		addInfrastructureBeanDefinition(
				registry, BeanOverrideBeanFactoryPostProcessor.class, INFRASTRUCTURE_BEAN_NAME,
				constructorArgs -> constructorArgs.addIndexedArgumentValue(0, registrarReference));
	}

	private static void addInfrastructureBeanDefinition(BeanDefinitionRegistry registry,
			Class<?> clazz, String beanName, Consumer<ConstructorArgumentValues> constructorArgumentsConsumer) {

		if (!registry.containsBeanDefinition(beanName)) {
			RootBeanDefinition definition = new RootBeanDefinition(clazz);
			definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			ConstructorArgumentValues constructorArguments = definition.getConstructorArgumentValues();
			constructorArgumentsConsumer.accept(constructorArguments);
			registry.registerBeanDefinition(beanName, definition);
		}
	}

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		if (context instanceof BeanDefinitionRegistry registry) {
			registerInfrastructure(registry, this.detectedClasses);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		BeanOverrideContextCustomizer other = (BeanOverrideContextCustomizer) obj;
		return this.detectedClasses.equals(other.detectedClasses);
	}

	@Override
	public int hashCode() {
		return this.detectedClasses.hashCode();
	}

}
