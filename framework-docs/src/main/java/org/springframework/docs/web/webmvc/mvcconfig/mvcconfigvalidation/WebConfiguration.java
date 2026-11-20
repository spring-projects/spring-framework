/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webmvc.mvcconfig.mvcconfigvalidation;

import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// tag::snippet[]
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

	@Override
	public Validator getValidator() {
		Validator validator = new OptionalValidatorFactoryBean();
		// ...
		return validator;
	}
}
// end::snippet[]
