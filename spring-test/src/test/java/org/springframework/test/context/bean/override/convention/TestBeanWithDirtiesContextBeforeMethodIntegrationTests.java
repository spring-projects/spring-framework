/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.convention;

import org.junit.jupiter.api.RepeatedTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

/**
 * Integration tests for using {@link TestBean @TestBean} with
 * {@link DirtiesContext @DirtiesContext} and {@link MethodMode#BEFORE_METHOD}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
class TestBeanWithDirtiesContextBeforeMethodIntegrationTests {

	@Autowired
	ExampleServiceCaller caller;

	@TestBean
	ExampleService service;

	@Autowired
	ExampleService autowiredService;


	static ExampleService service() {
		return mock();
	}


	@RepeatedTest(2)
	@DirtiesContext(methodMode = BEFORE_METHOD)
	void override() {
		assertThat(service).isSameAs(autowiredService);

		given(service.greeting()).willReturn("Spring");
		assertThat(caller.sayGreeting()).isEqualTo("I say Spring");
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ExampleServiceCaller.class)
	static class Config {
	}

}
