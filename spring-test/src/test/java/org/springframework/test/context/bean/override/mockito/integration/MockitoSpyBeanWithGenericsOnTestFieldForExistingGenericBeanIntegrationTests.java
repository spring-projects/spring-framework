/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.example.ExampleGenericService;
import org.springframework.test.context.bean.override.example.ExampleGenericServiceCaller;
import org.springframework.test.context.bean.override.example.IntegerExampleGenericService;
import org.springframework.test.context.bean.override.example.StringExampleGenericService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} on a field with generics can
 * be used to replace an existing bean with matching generics.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoBeanWithGenericsOnTestFieldForNewBeanIntegrationTests
 * @see MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests {

	@MockitoSpyBean
	ExampleGenericService<String> service;

	@Autowired
	ExampleGenericServiceCaller caller;


	@Test
	void spying() {
		assertThat(caller.sayGreeting()).isEqualTo("I say Enigma 123");
		then(service).should().greeting();
	}


	@Configuration(proxyBeanMethods = false)
	@Import({ ExampleGenericServiceCaller.class, IntegerExampleGenericService.class })
	static class Config {

		@Bean
		ExampleGenericService<String> simpleExampleStringGenericService() {
			// In order to trigger the issue, we need a method signature that returns the
			// generic type instead of the actual implementation class.
			return new StringExampleGenericService("Enigma");
		}
	}

}
