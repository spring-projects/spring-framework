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

package org.springframework.context.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.generator.AotContributingBeanFactoryPostProcessor;
import org.springframework.beans.factory.generator.BeanFactoryContribution;
import org.springframework.beans.factory.generator.BeanFactoryInitialization;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;

/**
 * AOT {@code BeanFactoryPostProcessor} that processes {@link RuntimeHintsRegistrar} implementations
 * declared as {@code spring.factories} or using {@link ImportRuntimeHints @ImportRuntimeHints} annotated
 * configuration classes or bean methods.
 * <p>This processor is registered by default in the {@link ApplicationContextAotGenerator} as it is
 * only useful in an AOT context.
 *
 * @author Brian Clozel
 * @see ApplicationContextAotGenerator
 */
class RuntimeHintsPostProcessor implements AotContributingBeanFactoryPostProcessor {

	private static final Log logger = LogFactory.getLog(RuntimeHintsPostProcessor.class);

	@Override
	public BeanFactoryContribution contribute(ConfigurableListableBeanFactory beanFactory) {
		ClassLoader beanClassLoader = beanFactory.getBeanClassLoader();
		List<RuntimeHintsRegistrar> registrars =
				new ArrayList<>(SpringFactoriesLoader.loadFactories(RuntimeHintsRegistrar.class, beanClassLoader));
		Arrays.stream(beanFactory.getBeanNamesForAnnotation(ImportRuntimeHints.class)).forEach(beanDefinitionName -> {
			ImportRuntimeHints importRuntimeHints = beanFactory.findAnnotationOnBean(beanDefinitionName, ImportRuntimeHints.class);
			if (importRuntimeHints != null) {
				Class<? extends RuntimeHintsRegistrar>[] registrarClasses = importRuntimeHints.value();
				for (Class<? extends RuntimeHintsRegistrar> registrarClass : registrarClasses) {
					logger.trace(LogMessage.format("Loaded [%s] registrar from annotated bean [%s]", registrarClass.getCanonicalName(), beanDefinitionName));
					RuntimeHintsRegistrar registrar = BeanUtils.instantiateClass(registrarClass);
					registrars.add(registrar);
				}
			}
		});
		return new RuntimeHintsRegistrarContribution(registrars, beanClassLoader);
	}


	static class RuntimeHintsRegistrarContribution implements BeanFactoryContribution {

		private final List<RuntimeHintsRegistrar> registrars;

		@Nullable
		private final ClassLoader beanClassLoader;

		RuntimeHintsRegistrarContribution(List<RuntimeHintsRegistrar> registrars, @Nullable ClassLoader beanClassLoader) {
			this.registrars = registrars;
			this.beanClassLoader = beanClassLoader;
		}

		@Override
		public void applyTo(BeanFactoryInitialization initialization) {
			this.registrars.forEach(registrar -> {
				logger.trace(LogMessage.format("Processing RuntimeHints contribution from [%s]", registrar.getClass().getCanonicalName()));
				registrar.registerHints(initialization.generatedTypeContext().runtimeHints(), this.beanClassLoader);
			});
		}
	}

}
