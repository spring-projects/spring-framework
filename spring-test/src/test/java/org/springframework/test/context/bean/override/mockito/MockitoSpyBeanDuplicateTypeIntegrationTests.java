/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;

/**
 * Integration tests for duplicate {@link MockitoSpyBean @MockitoSpyBean}
 * declarations for the same target bean, selected by-type.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34056">gh-34056</a>
 * @see MockitoBeanDuplicateTypeCreationIntegrationTests
 * @see MockitoSpyBeanDuplicateTypeAndNameIntegrationTests
 */
@SpringJUnitConfig
public class MockitoSpyBeanDuplicateTypeIntegrationTests {

	@MockitoSpyBean
	ExampleService spy1;

	@MockitoSpyBean
	ExampleService spy2;

	@Autowired
	List<ExampleService> services;


	@Test
	void onlyOneSpyShouldHaveBeenCreated() {
		// Currently logs something similar to the following.
		//
		// WARN - Bean with name 'exampleService' was overridden by multiple handlers:
		// [MockitoSpyBeanOverrideHandler@1d269ed7 ..., MockitoSpyBeanOverrideHandler@437ebf59 ...]

		assertThat(services).containsExactly(spy2);
		assertThat(spy1).isSameAs(spy2);

		assertIsSpy(spy2);
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService exampleService() {
			return new RealExampleService("@Bean");
		}
	}

}
