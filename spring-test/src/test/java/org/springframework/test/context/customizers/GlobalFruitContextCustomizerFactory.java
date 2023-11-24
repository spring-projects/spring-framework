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

package org.springframework.test.context.customizers;

import java.util.List;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
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
