/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.easymock;

import org.easymock.EasyMockSupport;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.bean.override.example.ExampleService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * Integration tests for {@link EasyMockBean @EasyMockBean}.
 *
 * @author Sam Brannen
 * @since 6.2
 */
@SpringJUnitConfig
@TestExecutionListeners(listeners = EasyMockResetTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
@TestMethodOrder(OrderAnnotation.class)
public class EasyMockBeanIntegrationTests {

	@Autowired
	ApplicationContext ctx;

	@EasyMockBean
	ExampleService service;

	@Test
	@Order(1)
	void test1() {
		assertThat(ctx.getBean("service", ExampleService.class))
				.satisfies(this::assertIsEasyMock)
				.isSameAs(service);

		// Before mock setup
		assertThat(service.greeting()).isNull();
		reset(service);

		// After mock setup
		expect(service.greeting()).andReturn("mocked");
		replay(service);
		assertThat(service.greeting()).isEqualTo("mocked");
	}

	@Test
	@Order(2)
	void test2() {
		assertThat(ctx.getBean("service", ExampleService.class))
				.satisfies(this::assertIsEasyMock)
				.isSameAs(service);

		// Before mock setup
		assertThat(service.greeting()).isNull();
		reset(service);

		// After mock setup
		expect(service.greeting()).andReturn("mocked");
		replay(service);
		assertThat(service.greeting()).isEqualTo("mocked");
	}


	private void assertIsEasyMock(Object obj) {
		assertThat(EasyMockSupport.isAMock(obj)).as("is EasyMock mock").isTrue();
	}


	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		ExampleService service() {
			return () -> "enigma";
		}
	}

}
