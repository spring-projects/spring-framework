/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.example.ExampleGenericService;
import org.springframework.test.context.bean.override.example.ExampleGenericServiceCaller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests that {@link MockitoBean @MockitoBean} on fields with generics can be used
 * to inject new mock instances.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests
 */
@SpringJUnitConfig
class MockitoBeanWithGenericsOnTestFieldForNewBeanIntegrationTests {

	@MockitoBean
	ExampleGenericService<String> stringService;

	@MockitoBean
	ExampleGenericService<Integer> integerService;

	@Autowired
	ExampleGenericServiceCaller caller;


	@Test
	void mocking() {
		given(stringService.greeting()).willReturn("Hello");
		given(integerService.greeting()).willReturn(42);
		assertThat(caller.sayGreeting()).isEqualTo("I say Hello 42");
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ExampleGenericServiceCaller.class)
	static class Config {
	}

}
