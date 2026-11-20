/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.FailingExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link MockitoBean @MockitoBean} used in combination with scoped-proxy
 * targets.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 6.2
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/5724">gh-5724</a>
 */
@ExtendWith(SpringExtension.class)
public class MockitoBeanAndScopedProxyIntegrationTests {

	@MockitoBean
	// The ExampleService mock should replace the scoped-proxy FailingExampleService
	// created in the @Configuration class.
	ExampleService service;

	@Autowired
	ExampleServiceCaller serviceCaller;


	@BeforeEach
	void configureServiceMock() {
		given(service.greeting()).willReturn("mock");
	}

	@Test
	void mocking() {
		assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock");
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
		ExampleService exampleService() {
			return new FailingExampleService();
		}

		@Bean
		ExampleServiceCaller serviceCaller(ExampleService service) {
			return new ExampleServiceCaller(service);
		}

	}

}
