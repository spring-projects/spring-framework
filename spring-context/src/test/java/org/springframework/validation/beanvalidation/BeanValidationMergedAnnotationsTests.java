/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation.beanvalidation;

import java.lang.reflect.Method;

import org.hibernate.validator.constraints.URL;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations;

/**
 * Smoke tests specific to the combination of {@link MergedAnnotations}
 * and Bean Validation constraint annotations.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class BeanValidationMergedAnnotationsTests {

	@Disabled("This test should only be run manually to inspect log messages")
	@Test
	@URL
	void constraintAnnotationOnMethod() throws Exception {
		// If the issue raised in gh-29206 had not been addressed, we would see
		// a log message similar to the following.
		// 19:07:33.848 [main] WARN  o.s.c.a.AnnotationTypeMapping - Support for
		// convention-based annotation attribute overrides is deprecated and will
		// be removed in Spring Framework 6.1. Please annotate the following attributes
		// in @org.hibernate.validator.constraints.URL with appropriate @AliasFor
		// declarations: [regexp, payload, flags, groups, message]
		Method method = getClass().getDeclaredMethod("constraintAnnotationOnMethod");
		MergedAnnotations mergedAnnotations = MergedAnnotations.from(method);
		mergedAnnotations.get(URL.class);
	}

}
