/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.context.aot;

import java.util.Arrays;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.aot.hint.annotation.ReflectiveProcessor;
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;

/**
 * AOT {@code BeanFactoryInitializationAotProcessor} that detects the presence
 * of {@link Reflective @Reflective} on annotated elements of all registered
 * beans and invokes the underlying {@link ReflectiveProcessor} implementations.
 *
 * @author Stephane Nicoll
 * @author Sebastien Deleuze
 */
class ReflectiveProcessorBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private static final ReflectiveRuntimeHintsRegistrar REGISTRAR = new ReflectiveRuntimeHintsRegistrar();

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		Class<?>[] beanTypes = Arrays.stream(beanFactory.getBeanDefinitionNames())
				.map(beanName -> RegisteredBean.of(beanFactory, beanName).getBeanClass())
				.toArray(Class<?>[]::new);
		return new ReflectiveProcessorBeanFactoryInitializationAotContribution(beanTypes);
	}

	private static class ReflectiveProcessorBeanFactoryInitializationAotContribution implements BeanFactoryInitializationAotContribution {

		private final Class<?>[] types;

		public ReflectiveProcessorBeanFactoryInitializationAotContribution(Class<?>[] types) {
			this.types = types;
		}

		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			RuntimeHints runtimeHints = generationContext.getRuntimeHints();
			REGISTRAR.registerRuntimeHints(runtimeHints, this.types);
		}

	}

}
