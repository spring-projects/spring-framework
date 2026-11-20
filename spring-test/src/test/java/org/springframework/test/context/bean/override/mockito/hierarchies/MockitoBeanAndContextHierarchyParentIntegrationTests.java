/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests which verify that {@link MockitoBean @MockitoBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockitoSpyBeanAndContextHierarchyChildIntegrationTests
 */
@SpringJUnitConfig
public class MockitoBeanAndContextHierarchyParentIntegrationTests {

	@MockitoBean
	ExampleService service;


	@BeforeEach
	void configureServiceMock() {
		given(service.greeting()).willReturn("mock");
	}

	@Test
	void test(ApplicationContext context) {
		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).isEmpty();

		assertThat(service.greeting()).isEqualTo("mock");
	}

}
