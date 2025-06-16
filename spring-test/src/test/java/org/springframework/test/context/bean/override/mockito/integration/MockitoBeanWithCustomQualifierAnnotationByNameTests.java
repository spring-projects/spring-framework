/*
 * Copyright 2002-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;
import static org.springframework.test.mockito.MockitoAssertions.assertMockName;

/**
 * Tests for {@link MockitoBean @MockitoBean} where the mocked bean is associated
 * with a custom {@link Qualifier @Qualifier} annotation and the bean to override
 * is selected by name.
 *
 * @author Sam Brannen
 * @since 6.2.6
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/34646">gh-34646</a>
 * @see MockitoBeanWithCustomQualifierAnnotationByTypeTests
 */
@ExtendWith(SpringExtension.class)
class MockitoBeanWithCustomQualifierAnnotationByNameTests {

	@MockitoBean(name = "qualifiedService", enforceOverride = true)
	@MyQualifier
	ExampleService service;

	@Autowired
	ExampleServiceCaller caller;


	@Test
	void test(ApplicationContext context) {
		assertIsMock(service);
		assertMockName(service, "qualifiedService");
		assertThat(service).isNotInstanceOf(QualifiedService.class);

		// Since the 'service' field's type is ExampleService, the QualifiedService
		// bean in the @Configuration class effectively gets removed from the context,
		// or rather it never gets created because we register an ExampleService as
		// a manual singleton in its place.
		assertThat(context.getBeanNamesForType(QualifiedService.class)).isEmpty();
		assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
		assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);

		when(service.greeting()).thenReturn("mock!");
		assertThat(caller.sayGreeting()).isEqualTo("I say mock!");
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
