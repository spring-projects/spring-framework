/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.annotation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Tests for the {@code @ComponentScan} annotation.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ComponentScanAnnotationIntegrationTests
 */
@Disabled("Compilation of this test class is sufficient to serve its purpose")
class ComponentScanAnnotationTests {

	@Test
	void noop() {
		// no-op; the @ComponentScan-annotated classes below simply exercise
		// available attributes of the annotation.
	}

	@interface MyAnnotation {
	}

	@Configuration
	@ComponentScan(
			basePackageClasses = TestBean.class,
			nameGenerator = DefaultBeanNameGenerator.class,
			scopedProxy = ScopedProxyMode.NO,
			scopeResolver = AnnotationScopeMetadataResolver.class,
			resourcePattern = "**/*custom.class",
			useDefaultFilters = false,
			includeFilters = {
					@Filter(type = FilterType.ANNOTATION, value = MyAnnotation.class)
			},
			excludeFilters = {
					@Filter(type = FilterType.CUSTOM, value = TypeFilter.class)
			},
			lazyInit = true
			)
	static class MyConfig {
	}

	@ComponentScan(basePackageClasses = example.scannable.NamedComponent.class)
	static class SimpleConfig {
	}

}

