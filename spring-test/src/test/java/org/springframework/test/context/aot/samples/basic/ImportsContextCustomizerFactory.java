/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot.samples.basic;

import java.util.Arrays;
import java.util.List;

import org.springframework.aot.AotDetector;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Emulates {@code ImportsContextCustomizerFactory} from Spring Boot's testing support.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class ImportsContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		if (AotDetector.useGeneratedArtifacts()) {
			return null;
		}
		if (testClass.getName().startsWith("org.springframework.test.context.aot.samples") &&
				testClass.isAnnotationPresent(Import.class)) {
			return new ImportsContextCustomizer(testClass);
		}
		return null;
	}

	/**
	 * Emulates {@code ImportsContextCustomizer} from Spring Boot's testing support.
	 */
	private static class ImportsContextCustomizer implements ContextCustomizer {

		private final Class<?> testClass;

		ImportsContextCustomizer(Class<?> testClass) {
			this.testClass = testClass;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			AnnotatedBeanDefinitionReader annotatedBeanDefinitionReader =
					new AnnotatedBeanDefinitionReader((GenericApplicationContext) context);
			Arrays.stream(this.testClass.getAnnotation(Import.class).value())
					.forEach(annotatedBeanDefinitionReader::register);
		}
	}

}
