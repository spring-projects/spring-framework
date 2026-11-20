/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.RepeatedTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.SimpleExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

/**
 * Integration tests for using {@link MockitoSpyBean @MockitoSpyBean} with
 * {@link DirtiesContext @DirtiesContext} and {@link MethodMode#BEFORE_METHOD}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoBeanWithDirtiesContextBeforeMethodIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanWithDirtiesContextBeforeMethodIntegrationTests {

	@Autowired
	ExampleServiceCaller caller;

	@MockitoSpyBean
	SimpleExampleService service;

	@Autowired
	ExampleService autowiredService;


	@RepeatedTest(2)
	@DirtiesContext(methodMode = BEFORE_METHOD)
	void spying() {
		assertThat(service).isSameAs(autowiredService);

		assertThat(caller.sayGreeting()).isEqualTo("I say simple");
		then(service).should().greeting();
	}


	@Configuration(proxyBeanMethods = false)
	@Import(SimpleExampleService.class)
	static class Config {

		@Bean
		ExampleServiceCaller serviceCaller(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

}
