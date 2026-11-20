/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.bean.override.example.FailingExampleService;
import org.springframework.test.context.bean.override.example.RealExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.context.bean.override.mockito.MockReset.BEFORE;

/**
 * Integration tests for {@link MockitoBean} that validate automatic reset
 * of stubbing.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@SpringJUnitConfig
@TestMethodOrder(OrderAnnotation.class)
public class MockitoBeanWithResetIntegrationTests {

	@MockitoBean(reset = BEFORE)
	ExampleService service;

	@MockitoBean(reset = BEFORE)
	FailingExampleService failingService;

	@Order(1)
	@Test
	void beanFirstEstablishingMock(ApplicationContext ctx) {
		ExampleService mock = ctx.getBean("service", ExampleService.class);
		doReturn("Mocked hello").when(mock).greeting();

		assertThat(this.service.greeting()).isEqualTo("Mocked hello");
	}

	@Order(2)
	@Test
	void beanSecondEnsuringMockReset(ApplicationContext ctx) {
		assertThat(ctx.getBean("service")).isNotNull().isSameAs(this.service);

		assertThat(this.service.greeting()).as("not stubbed").isNull();
	}

	@Order(3)
	@Test
	void factoryBeanFirstEstablishingMock(ApplicationContext ctx) {
		FailingExampleService mock = ctx.getBean(FailingExampleService.class);
		doReturn("Mocked hello").when(mock).greeting();

		assertThat(this.failingService.greeting()).isEqualTo("Mocked hello");
	}

	@Order(4)
	@Test
	void factoryBeanSecondEnsuringMockReset(ApplicationContext ctx) {
		assertThat(ctx.getBean("factory")).isNotNull().isSameAs(this.failingService);

		assertThat(this.failingService.greeting()).as("not stubbed")
				.isNull();
	}

	static class FailingExampleServiceFactory implements FactoryBean<FailingExampleService> {
		@Override
		public @Nullable FailingExampleService getObject() {
			return new FailingExampleService();
		}

		@Override
		public @Nullable Class<?> getObjectType() {
			return FailingExampleService.class;
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean("service")
		ExampleService bean1() {
			return new RealExampleService("Production hello");
		}

		@Bean("factory")
		FailingExampleServiceFactory factory() {
			return new FailingExampleServiceFactory();
		}
	}

}
