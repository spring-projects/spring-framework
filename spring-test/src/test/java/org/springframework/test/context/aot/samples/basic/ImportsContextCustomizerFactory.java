/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
