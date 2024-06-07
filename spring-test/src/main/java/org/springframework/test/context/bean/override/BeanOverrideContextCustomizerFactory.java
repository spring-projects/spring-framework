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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.TestContextAnnotationUtils;

/**
 * {@link ContextCustomizerFactory} implementation that provides support for
 * Bean Overriding.
 *
 * @author Simon Baslé
 * @author Stephane Nicoll
 * @since 6.2
 * @see BeanOverride
 */
class BeanOverrideContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	@Nullable
	public BeanOverrideContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {

		Set<OverrideMetadata> metadata = new HashSet<>();
		findOverrideMetadata(testClass, metadata);
		if (metadata.isEmpty()) {
			return null;
		}
		return new BeanOverrideContextCustomizer(metadata);
	}

	private void findOverrideMetadata(Class<?> testClass, Set<OverrideMetadata> metadata) {
		List<OverrideMetadata> overrideMetadata = OverrideMetadata.forTestClass(testClass);
		metadata.addAll(overrideMetadata);
		if (TestContextAnnotationUtils.searchEnclosingClass(testClass)) {
			findOverrideMetadata(testClass.getEnclosingClass(), metadata);
		}
	}

}
