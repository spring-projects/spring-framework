/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.customizers;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * @author Sam Brannen
 * @since 6.1
 */
class GlobalFruitContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		if (testClass.isAnnotationPresent(CustomizeWithFruit.class)) {
			return new GlobalFruitContextCustomizer();
		}
		return null;
	}

}

class GlobalFruitContextCustomizer implements ContextCustomizer {

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		// Use "global$fruit" as the bean name instead of something simple like "fruit"
		// to avoid bean name clashes with any test that registers a bean named "fruit".
		context.getBeanFactory().registerSingleton("global$fruit", "apple, banana, cherry");
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other != null && getClass() == other.getClass()));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

}
