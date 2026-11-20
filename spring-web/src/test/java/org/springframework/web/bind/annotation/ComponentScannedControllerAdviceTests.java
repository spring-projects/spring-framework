/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for component scanning
 * {@link ControllerAdvice} and {@link RestControllerAdvice} beans.
 *
 * @author Sam Brannen
 * @since 6.1
 */
class ComponentScannedControllerAdviceTests {

	@Test
	void scannedAdviceHasCustomName() {
		String basePackage = getClass().getPackageName() + ".scanned";
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(basePackage)) {
			assertThat(context.getBean("myControllerAdvice")).isNotNull();
			assertThat(context.getBean("myRestControllerAdvice")).isNotNull();
		}
	}

}
