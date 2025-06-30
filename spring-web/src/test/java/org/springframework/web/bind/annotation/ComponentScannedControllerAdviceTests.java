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
