/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.example.ExampleGenericServiceCaller;
import org.springframework.test.context.bean.override.example.IntegerExampleGenericService;
import org.springframework.test.context.bean.override.example.StringExampleGenericService;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.mockito.MockitoAssertions.assertIsSpy;
import static org.springframework.test.mockito.MockitoAssertions.assertMockName;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to spy on a bean
 * when there are multiple candidates and a {@link Qualifier @Qualifier} is supplied
 * to select one of the candidates.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoSpyBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests
 */
@SpringJUnitConfig
class MockitoSpyBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests {

	@Qualifier("stringService")
	@MockitoSpyBean
	StringExampleGenericService spy;

	@Autowired
	ExampleGenericServiceCaller caller;


	@Test
	void test() {
		assertIsSpy(spy);
		assertMockName(spy, "stringService");

		assertThat(caller.sayGreeting()).isEqualTo("I say two 123");
		then(spy).should().greeting();
	}


	@Configuration(proxyBeanMethods = false)
	@Import({ ExampleGenericServiceCaller.class, IntegerExampleGenericService.class })
	static class Config {

		@Bean
		StringExampleGenericService one() {
			return new StringExampleGenericService("one");
		}

		@Bean
		// "stringService" matches the constructor argument name in ExampleGenericServiceCaller
		StringExampleGenericService stringService() {
			return new StringExampleGenericService("two");
		}
	}

}
