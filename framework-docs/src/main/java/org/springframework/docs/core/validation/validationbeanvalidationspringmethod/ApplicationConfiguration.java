/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.core.validation.validationbeanvalidationspringmethod;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

// tag::snippet[]
@Configuration
public class ApplicationConfiguration {

	@Bean
	public static MethodValidationPostProcessor validationPostProcessor() {
		return new MethodValidationPostProcessor();
	}
}
// end::snippet[]
