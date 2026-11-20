/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests which verify that {@link MockitoBean @MockitoBean} and
 * {@link MockitoSpyBean @MockitoSpyBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockitoBeanAndContextHierarchyParentIntegrationTests
 */
@ContextHierarchy(@ContextConfiguration)
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
public class MockitoSpyBeanAndContextHierarchyChildIntegrationTests extends
		MockitoBeanAndContextHierarchyParentIntegrationTests {

	@MockitoSpyBean
	ExampleServiceCaller serviceCaller;


	@Test
	@Override
	void test(ApplicationContext context) {
		ApplicationContext parentContext = context.getParent();
		assertThat(parentContext).as("parent ApplicationContext").isNotNull();
		assertThat(parentContext.getParent()).as("grandparent ApplicationContext").isNull();

		assertThat(parentContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(parentContext.getBeanNamesForType(ExampleServiceCaller.class)).isEmpty();

		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);

		assertThat(service.greeting()).isEqualTo("mock");
		assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock");
	}


	@Configuration(proxyBeanMethods = false)
	static class ChildConfig {

		@Bean
		ExampleServiceCaller serviceCaller(ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

}
