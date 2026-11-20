/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.cache;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * @author Sam Brannen
 * @since 7.0
 */
class DisplayNameCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		return new DisplayNameCustomizer(testClass.getSimpleName());
	}


	private static class DisplayNameCustomizer implements ContextCustomizer {

		private final String displayName;

		DisplayNameCustomizer(String displayName) {
			this.displayName = displayName;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			((AbstractApplicationContext) context).setDisplayName(this.displayName);
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

}
