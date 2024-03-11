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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} which provides support for Bean Overriding
 * in tests.
 *
 * @author Simon Baslé
 * @since 6.2
 */
public class BeanOverrideContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		BeanOverrideParser parser = new BeanOverrideParser();
		parseMetadata(testClass, parser);
		if (parser.getOverrideMetadata().isEmpty()) {
			return null;
		}

		return new BeanOverrideContextCustomizer(parser.getOverrideMetadata());
	}

	private void parseMetadata(Class<?> testClass, BeanOverrideParser parser) {
		parser.parse(testClass);
		if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
			parseMetadata(testClass.getEnclosingClass(), parser);
		}
	}

	/**
	 * {@link ContextCustomizer} for Bean Overriding in tests.
	 */
	private static final class BeanOverrideContextCustomizer implements ContextCustomizer {

		private final Set<OverrideMetadata> metadata;

		/**
		 * Construct a context customizer given some pre-existing override
		 * metadata.
		 * @param metadata a set of concrete {@link OverrideMetadata} provided
		 * by the underlying {@link BeanOverrideParser}
		 */
		BeanOverrideContextCustomizer(Set<OverrideMetadata> metadata) {
			this.metadata = metadata;
		}

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			if (context instanceof BeanDefinitionRegistry registry) {
				BeanOverrideBeanPostProcessor.register(registry, this.metadata);
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
			return this.metadata.equals(other.metadata);
		}

		@Override
		public int hashCode() {
			return this.metadata.hashCode();
		}
	}

}
