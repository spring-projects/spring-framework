package com.lxcecho.validator.two;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
@Configuration
@ComponentScan("com.lxcecho.validator.two")
public class ValidationConfig {

	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}

}
