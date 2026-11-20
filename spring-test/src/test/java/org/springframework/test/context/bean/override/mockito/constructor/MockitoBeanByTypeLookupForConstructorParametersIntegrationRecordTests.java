/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.constructor;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Integration tests for {@link MockitoBean @MockitoBean} that use by-type lookup
 * on constructor parameters in a Java record.
 *
 * @author Sam Brannen
 * @since 7.1
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/36096">gh-36096</a>
 */
@SpringJUnitConfig
record MockitoBeanByTypeLookupForConstructorParametersIntegrationRecordTests(
		@MockitoBean ExampleService exampleService) {

	@Test
	void test() {
		assertIsMock(this.exampleService);

		when(this.exampleService.greeting()).thenReturn("Mocked greeting");

		assertThat(this.exampleService.greeting()).isEqualTo("Mocked greeting");
		verify(this.exampleService, times(1)).greeting();
		verifyNoMoreInteractions(this.exampleService);
	}

}
