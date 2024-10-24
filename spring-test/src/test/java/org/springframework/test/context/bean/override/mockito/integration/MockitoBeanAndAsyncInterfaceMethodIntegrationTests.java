/*
 * Copyright 2012-2024 the original author or authors.
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

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

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
		assertThat(Mockito.mockingDetails(transformer).isMock()).as("is Mockito mock").isTrue();

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
