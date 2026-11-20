/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.ExampleServiceCaller;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;
import static org.springframework.test.mockito.MockitoAssertions.assertMockName;

/**
 * Tests for {@link MockitoSpyBean @MockitoSpyBean} where the mocked bean is associated
 * with a custom {@link Qualifier @Qualifier} annotation and the bean to override
 * is selected by name.
 *
 * @author Sam Brannen
 * @since 6.2.6
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34646">gh-34646</a>
 * @see MockitoSpyBeanWithCustomQualifierAnnotationByTypeTests
 */
@ExtendWith(SpringExtension.class)
class MockitoSpyBeanWithCustomQualifierAnnotationByNameTests {

	@MockitoSpyBean(name = "qualifiedService")
	@MyQualifier
	ExampleService service;

	@Autowired
	ExampleServiceCaller caller;


	@Test
	void test(ApplicationContext context) {
		assertIsSpy(service);
		assertMockName(service, "qualifiedService");
		assertThat(service).isInstanceOf(QualifiedService.class);

		assertThat(context.getBeanNamesForType(QualifiedService.class)).hasSize(1);
		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);

		when(service.greeting()).thenReturn("mock!");
		assertThat(caller.sayGreeting()).isEqualTo("I say mock!");
		verify(service).greeting();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		QualifiedService qualifiedService() {
			return new QualifiedService();
		}

		@Bean
		ExampleServiceCaller myServiceCaller(@MyQualifier ExampleService service) {
			return new ExampleServiceCaller(service);
		}
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface MyQualifier {
	}

	@MyQualifier
	static class QualifiedService implements ExampleService {

		@Override
		public String greeting() {
			return "Qualified service";
		}
	}

}
