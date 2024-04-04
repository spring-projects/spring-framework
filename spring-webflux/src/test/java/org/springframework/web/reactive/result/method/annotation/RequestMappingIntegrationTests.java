/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.config.BlockingExecutionConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests with {@code @RequestMapping} handler methods.
 *
 * <p>Before adding tests here consider if they are a better fit for any of the
 * other {@code RequestMapping*IntegrationTests}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @author Sebastien Deleuze
 * @since 5.0
 */
class RequestMappingIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class, TestRestController.class, LocalConfig.class);
		wac.refresh();
		return wac;
	}


	@ParameterizedHttpServerTest // gh-30293
	void emptyMapping(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String url = "http://localhost:" + this.port;
		assertThat(getRestTemplate().getForObject(url, String.class)).isEqualTo("root");

		url += "/";
		assertThat(getRestTemplate().getForObject(url, String.class)).isEqualTo("root");

		assertThat(getApplicationContext().getBean(TestExecutor.class).invocationCount.get()).isEqualTo(4);
		assertThat(getApplicationContext().getBean(TestPredicate.class).invocationCount.get()).isEqualTo(4);
	}

	@ParameterizedHttpServerTest
	void httpHead(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String url = "http://localhost:" + this.port + "/text";
		HttpHeaders headers = getRestTemplate().headForHeaders(url);
		String contentType = headers.getFirst("Content-Type");
		assertThat(contentType).isNotNull();
		assertThat(headers.getContentLength()).isEqualTo(3);
	}

	@ParameterizedHttpServerTest
	void forwardedHeaders(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		// One integration test to verify triggering of Forwarded header support.
		// More fine-grained tests in ForwardedHeaderTransformerTests.

		RequestEntity<Void> request = RequestEntity
				.get(URI.create("http://localhost:" + this.port + "/uri"))
				.header("Forwarded", "host=84.198.58.199;proto=https")
				.build();
		ResponseEntity<String> entity = getRestTemplate().exchange(request, String.class);
		assertThat(entity.getBody()).isEqualTo("https://84.198.58.199/uri");
	}

	@ParameterizedHttpServerTest
	void stream(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		int[] expected = {0, 1, 2, 3, 4};
		assertThat(performGet("/stream", new HttpHeaders(), int[].class).getBody()).isEqualTo(expected);
	}


	@Configuration
	@EnableWebFlux
	static class WebConfig implements WebFluxConfigurer {

		@Override
		public void configureBlockingExecution(BlockingExecutionConfigurer configurer) {
			configurer.setExecutor(executor());
			configurer.setControllerMethodPredicate(predicate());
		}

		@Bean
		TestExecutor executor() {
			return new TestExecutor();
		}

		@Bean
		TestPredicate predicate() {
			return new TestPredicate();
		}

	}


	@Configuration
	static class LocalConfig {

		@Bean
		public ForwardedHeaderTransformer forwardedHeaderTransformer() {
			return new ForwardedHeaderTransformer();
		}
	}


	@RestController
	@SuppressWarnings("unused")
	private static class TestRestController {

		@GetMapping
		public String get() {
			return "root";
		}

		@GetMapping("/text")
		public String textGet() {
			return "Foo";
		}

		// SPR-17593: explicit HEAD should not clash with implicit mapping via GET
		@RequestMapping(path = "/text", method = RequestMethod.HEAD)
		public String textHead() {
			return textGet();
		}

		@GetMapping("/uri")
		public String uri(ServerHttpRequest request) {
			return request.getURI().toString();
		}

		@GetMapping("/stream")
		public Publisher<Long> stream() {
			return testInterval(Duration.ofMillis(50), 5);
		}
	}


	private static class TestExecutor implements AsyncTaskExecutor {

		private final AtomicInteger invocationCount = new AtomicInteger();

		@Override
		public void execute(Runnable task) {
			this.invocationCount.incrementAndGet();
			task.run();
		}
	}


	private static class TestPredicate implements Predicate<HandlerMethod> {

		private final AtomicInteger invocationCount = new AtomicInteger();

		@Override
		public boolean test(HandlerMethod handlerMethod) {
			this.invocationCount.incrementAndGet();
			Class<?> returnType = handlerMethod.getReturnType().getParameterType();
			return (ReactiveAdapterRegistry.getSharedInstance().getAdapter(returnType) == null);
		}
	}

}
