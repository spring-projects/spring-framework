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
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Integration tests for Bean Overrides where a {@link MockitoBean @MockitoBean}
 * overrides a {@link TestBean @TestBean} when trying to replace the same existing
 * bean, selected by-type.
 *
 * <p>In other words, this test class demonstrates how one Bean Override can
 * silently override another Bean Override.
 *
 * @author Sam Brannen
 * @since 6.2.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34056">gh-34056</a>
 * @see MockitoBeanDuplicateTypeCreationIntegrationTests
 * @see MockitoBeanDuplicateTypeReplacementIntegrationTests
 */
@SpringJUnitConfig
public class MockitoBeanOverridesTestBeanIntegrationTests {

	@TestBean
	ExampleService testService;

	@MockitoBean
	ExampleService mockService;

	@Autowired
	List<ExampleService> services;


	static ExampleService testService() {
		return new RealExampleService("@TestBean");
	}


	/**
	 * One could argue that we would ideally expect an exception to be thrown when
	 * two competing overrides are created to replace the same existing bean; however,
	 * we currently only log a warning in such cases.
	 * <p>This method therefore asserts the status quo in terms of behavior.
	 * <p>And the log can be manually checked to verify that an appropriate
	 * warning was logged.
	 */
	@Test
	void mockitoBeanShouldOverrideTestBean() {
		// Currently logs something similar to the following.
		//
		// WARN - Bean with name 'exampleService' was overridden by multiple handlers:
		// [TestBeanOverrideHandler@770beef5 ..., MockitoBeanOverrideHandler@6dd1f638 ...]

		// Last override wins...
		assertThat(services).containsExactly(mockService);
		assertThat(testService).isSameAs(mockService);

		assertIsMock(mockService);

		assertThat(mockService.greeting()).isNull();
		given(mockService.greeting()).willReturn("mocked");
		assertThat(mockService.greeting()).isEqualTo("mocked");
	}


	@Configuration
	static class Config {

		@Bean
		ExampleService exampleService() {
			return () -> "@Bean";
		}
	}

}
