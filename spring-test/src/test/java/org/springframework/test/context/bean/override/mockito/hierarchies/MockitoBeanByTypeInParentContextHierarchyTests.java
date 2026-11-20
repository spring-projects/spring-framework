/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.hierarchies.MockitoBeanByTypeInParentContextHierarchyTests.Config1;
import org.springframework.test.context.bean.override.mockito.hierarchies.MockitoBeanByTypeInParentContextHierarchyTests.Config2;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link MockitoBean @MockitoBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy} with named context levels, when
 * a bean is only mocked "by type" in the parent.
 *
 * @author Sam Brannen
 * @since 6.2.6
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
	@ContextConfiguration(classes = Config1.class, name = "parent"),
	@ContextConfiguration(classes = Config2.class)
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockitoBeanByTypeInParentContextHierarchyTests {

	@MockitoBean(contextName = "parent")
	ExampleService service;

	@Autowired
	ExampleServiceCaller serviceCaller1;

	@Autowired
	ExampleServiceCaller serviceCaller2;


	@Test
	void test() {
		when(service.greeting()).thenReturn("Mock 1");

		assertThat(service.greeting()).isEqualTo("Mock 1");
		assertThat(serviceCaller1.getService()).isSameAs(service);
		assertThat(serviceCaller2.getService()).isSameAs(service);
		assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say Mock 1");
		assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say Mock 1");
	}


	@Configuration
	static class Config1 {

		@Bean
		ExampleService service() {
			return new RealExampleService("Service 1");
		}

		@Bean
		ExampleServiceCaller serviceCaller1(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

	@Configuration
	static class Config2 {

		@Bean
		ExampleServiceCaller serviceCaller2(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

}
