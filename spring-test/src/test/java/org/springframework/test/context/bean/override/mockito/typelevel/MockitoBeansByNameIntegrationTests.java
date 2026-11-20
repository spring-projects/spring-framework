/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.typelevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;
import static org.springframework.test.mockito.MockitoAssertions.assertIsNotMock;

/**
 * Integration tests for {@link MockitoBeans @MockitoBeans} and
 * {@link MockitoBean @MockitoBean} declared "by name" at the class level as a
 * repeatable annotation.
 *
 * @author Sam Brannen
 * @since 6.2.2
 * @see <a href="https://github.com/spring-projects/spring-framework/issues/33925">gh-33925</a>
 * @see MockitoBeansByTypeIntegrationTests
 */
@SpringJUnitConfig
@MockitoBean(name = "s1", types = ExampleService.class)
@MockitoBean(name = "s2", types = ExampleService.class)
class MockitoBeansByNameIntegrationTests {

	final ExampleService service0A;
	final ExampleService service0B;
	final ExampleService service0C;

	@Autowired
	ExampleService s1;

	@Autowired
	ExampleService s2;

	@MockitoBean(name = "s3")
	ExampleService service3;

	@Autowired
	@Qualifier("s4")
	ExampleService service4;


	MockitoBeansByNameIntegrationTests(@MockitoBean ExampleService s0A,
			@MockitoBean(name = "s0B") ExampleService service0B,
			@MockitoBean @Qualifier("s0C") ExampleService service0C) {

		this.service0A = s0A;
		this.service0B = service0B;
		this.service0C = service0C;
	}


	@BeforeEach
	void configureMocks() {
		assertIsMock(s1, "s1");
		assertIsMock(s2, "s2");
		assertIsMock(service3, "service3");
		assertIsNotMock(service4, "service4");

		given(s1.greeting()).willReturn("mock 1");
		given(s2.greeting()).willReturn("mock 2");
		given(service3.greeting()).willReturn("mock 3");
	}

	@Test
	void checkMocksAndStandardBean() {
		assertThat(s1.greeting()).isEqualTo("mock 1");
		assertThat(s2.greeting()).isEqualTo("mock 2");
		assertThat(service3.greeting()).isEqualTo("mock 3");
		assertThat(service4.greeting()).isEqualTo("prod 4");
	}


	@Configuration
	static class Config {

		@Bean
		ExampleService s0A() {
			return () -> "prod 0A";
		}

		@Bean
		ExampleService s0B() {
			return () -> "prod 0B";
		}

		@Bean
		ExampleService s0C() {
			return () -> "prod 0C";
		}

		@Bean
		ExampleService s1() {
			return () -> "prod 1";
		}

		@Bean
		ExampleService s2() {
			return () -> "prod 2";
		}

		@Bean
		ExampleService s3() {
			return () -> "prod 3";
		}

		@Bean
		ExampleService s4() {
			return () -> "prod 4";
		}
	}

}
