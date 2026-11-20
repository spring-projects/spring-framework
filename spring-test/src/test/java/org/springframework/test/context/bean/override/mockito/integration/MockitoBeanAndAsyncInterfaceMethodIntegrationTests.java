/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.mockito.integration;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.mockito.MockitoAssertions.assertIsMock;

/**
 * Tests for {@link MockitoBean @MockitoBean} where the mocked interface has an
 * {@link Async @Async} method.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @since 6.2
 */
@ExtendWith(SpringExtension.class)
public class MockitoBeanAndAsyncInterfaceMethodIntegrationTests {

	@MockitoBean
	Transformer transformer;

	@Autowired
	MyService service;


	@Test
	void mockedMethodsAreNotAsync() throws Exception {
		assertThat(AopUtils.isAopProxy(transformer)).as("is Spring AOP proxy").isFalse();
		assertIsMock(transformer);

		given(transformer.transform("foo")).willReturn(completedFuture("bar"));
		assertThat(service.transform("foo")).isEqualTo("result: bar");
	}


	interface Transformer {

		@Async
		CompletableFuture<String> transform(String input);
	}

	record MyService(Transformer transformer) {

		String transform(String input) throws Exception {
			return "result: " + this.transformer.transform(input).get();
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAsync
	static class Config {

		@Bean
		Transformer transformer() {
			return input -> completedFuture(input.toUpperCase());
		}

		@Bean
		MyService myService(Transformer transformer) {
			return new MyService(transformer);
		}
	}

}
