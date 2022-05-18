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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.AotFactoriesLoader;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation that processes
 * {@link RuntimeHintsRegistrar} implementations declared as
 * {@code spring.factories} or using
 * {@link ImportRuntimeHints @ImportRuntimeHints} annotated configuration
 * classes or bean methods.
 *
 * @author Brian Clozel
 */
class RuntimeHintsBeanFactoryInitializationAotProcessor
		implements BeanFactoryInitializationAotProcessor {

	private static final Log logger = LogFactory
			.getLog(RuntimeHintsBeanFactoryInitializationAotProcessor.class);


	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(
			ConfigurableListableBeanFactory beanFactory) {
		AotFactoriesLoader loader = new AotFactoriesLoader(beanFactory);
		List<RuntimeHintsRegistrar> registrars = new ArrayList<>(
				loader.load(RuntimeHintsRegistrar.class));
		for (String beanName : beanFactory
				.getBeanNamesForAnnotation(ImportRuntimeHints.class)) {
			ImportRuntimeHints annotation = beanFactory.findAnnotationOnBean(beanName,
					ImportRuntimeHints.class);
			if (annotation != null) {
				registrars.addAll(extracted(beanName, annotation));
			}
		}
		return new RuntimeHintsRegistrarContribution(registrars,
				beanFactory.getBeanClassLoader());
	}

	private List<RuntimeHintsRegistrar> extracted(String beanName,
			ImportRuntimeHints annotation) {
		Class<? extends RuntimeHintsRegistrar>[] registrarClasses = annotation.value();
		List<RuntimeHintsRegistrar> registrars = new ArrayList<>(registrarClasses.length);
		for (Class<? extends RuntimeHintsRegistrar> registrarClass : registrarClasses) {
			logger.trace(
					LogMessage.format("Loaded [%s] registrar from annotated bean [%s]",
							registrarClass.getCanonicalName(), beanName));
			registrars.add(BeanUtils.instantiateClass(registrarClass));
		}
		return registrars;
	}


	static class RuntimeHintsRegistrarContribution
			implements BeanFactoryInitializationAotContribution {


		private final List<RuntimeHintsRegistrar> registrars;

		@Nullable
		private final ClassLoader beanClassLoader;


		RuntimeHintsRegistrarContribution(List<RuntimeHintsRegistrar> registrars,
				@Nullable ClassLoader beanClassLoader) {
			this.registrars = registrars;
			this.beanClassLoader = beanClassLoader;
		}


		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			RuntimeHints hints = generationContext.getRuntimeHints();
			this.registrars.forEach(registrar -> {
				logger.trace(LogMessage.format(
						"Processing RuntimeHints contribution from [%s]",
						registrar.getClass().getCanonicalName()));
				registrar.registerHints(hints, this.beanClassLoader);
			});
		}

	}

}
