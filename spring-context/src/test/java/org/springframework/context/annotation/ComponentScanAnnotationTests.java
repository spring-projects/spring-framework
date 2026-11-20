/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

