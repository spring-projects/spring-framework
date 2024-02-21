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

package org.springframework.context.aot;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.aot.AotServices;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} implementation that processes
 * {@link RuntimeHintsRegistrar} implementations declared as
 * {@code spring.factories} or using
 * {@link ImportRuntimeHints @ImportRuntimeHints} annotated configuration
 * classes or bean methods.
 *
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @since 6.0
 */
class RuntimeHintsBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	private static final Log logger = LogFactory.getLog(RuntimeHintsBeanFactoryInitializationAotProcessor.class);


	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		Map<Class<? extends RuntimeHintsRegistrar>, RuntimeHintsRegistrar> registrars = AotServices
				.factories(beanFactory.getBeanClassLoader()).load(RuntimeHintsRegistrar.class).stream()
				.collect(LinkedHashMap::new, (map, item) -> map.put(item.getClass(), item), Map::putAll);
		extractFromBeanFactory(beanFactory).forEach(registrarClass ->
				registrars.computeIfAbsent(registrarClass, BeanUtils::instantiateClass));
		return new RuntimeHintsRegistrarContribution(registrars.values(), beanFactory.getBeanClassLoader());
	}

	private Set<Class<? extends RuntimeHintsRegistrar>> extractFromBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		Set<Class<? extends RuntimeHintsRegistrar>> registrarClasses = new LinkedHashSet<>();
		for (String beanName : beanFactory.getBeanDefinitionNames()) {
			beanFactory.findAllAnnotationsOnBean(beanName, ImportRuntimeHints.class, true)
					.forEach(annotation -> registrarClasses.addAll(extractFromBeanDefinition(beanName, annotation)));
		}
		return registrarClasses;
	}

	private Set<Class<? extends RuntimeHintsRegistrar>> extractFromBeanDefinition(String beanName,
			ImportRuntimeHints annotation) {

		Class<? extends RuntimeHintsRegistrar>[] registrarClasses = annotation.value();
		Set<Class<? extends RuntimeHintsRegistrar>> registrars = CollectionUtils.newLinkedHashSet(registrarClasses.length);
		for (Class<? extends RuntimeHintsRegistrar> registrarClass : registrarClasses) {
			if (logger.isTraceEnabled()) {
				logger.trace(LogMessage.format("Loaded [%s] registrar from annotated bean [%s]",
						registrarClass.getCanonicalName(), beanName));
			}
			registrars.add(registrarClass);
		}
		return registrars;
	}


	static class RuntimeHintsRegistrarContribution implements BeanFactoryInitializationAotContribution {

		private final Iterable<RuntimeHintsRegistrar> registrars;

		@Nullable
		private final ClassLoader beanClassLoader;

		RuntimeHintsRegistrarContribution(Iterable<RuntimeHintsRegistrar> registrars,
				@Nullable ClassLoader beanClassLoader) {

			this.registrars = registrars;
			this.beanClassLoader = beanClassLoader;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {

			RuntimeHints hints = generationContext.getRuntimeHints();
			this.registrars.forEach(registrar -> {
				if (logger.isTraceEnabled()) {
					logger.trace(LogMessage.format(
							"Processing RuntimeHints contribution from [%s]",
							registrar.getClass().getCanonicalName()));
				}
				registrar.registerHints(hints, this.beanClassLoader);
			});
		}
	}

}
