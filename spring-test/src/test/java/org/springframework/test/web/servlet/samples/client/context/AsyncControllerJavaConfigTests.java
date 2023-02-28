/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.context.AsyncControllerJavaConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy(@ContextConfiguration(classes = AsyncControllerJavaConfigTests.WebConfig.class))
public class AsyncControllerJavaConfigTests {

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private CallableProcessingInterceptor callableInterceptor;

	private WebTestClient testClient;


	@BeforeEach
	public void setup() {
		this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
	}

	@Test
	public void callableInterceptor() throws Exception {
		testClient.get().uri("/callable")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("{\"key\":\"value\"}");

		Mockito.verify(this.callableInterceptor).beforeConcurrentHandling(any(), any());
		Mockito.verify(this.callableInterceptor).preProcess(any(), any());
		Mockito.verify(this.callableInterceptor).postProcess(any(), any(), any());
		Mockito.verify(this.callableInterceptor).afterCompletion(any(), any());
		Mockito.verifyNoMoreInteractions(this.callableInterceptor);
	}


	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Override
		public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
			configurer.registerCallableInterceptors(callableInterceptor());
		}

		@Bean
		public CallableProcessingInterceptor callableInterceptor() {
			return mock();
		}

		@Bean
		public AsyncController asyncController() {
			return new AsyncController();
		}

	}

	@RestController
	static class AsyncController {

		@GetMapping("/callable")
		public Callable<Map<String, String>> getCallable() {
			return () -> Collections.singletonMap("key", "value");
		}
	}

}
