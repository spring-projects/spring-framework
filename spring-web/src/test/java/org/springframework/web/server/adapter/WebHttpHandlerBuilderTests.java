/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.server.adapter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.micrometer.observation.tck.TestObservationRegistry;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.HttpHandlerDecoratorFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link WebHttpHandlerBuilder}.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 */
class WebHttpHandlerBuilderTests {

	@Test  // SPR-15074
	void orderedWebFilterBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(OrderedWebFilterBeanConfig.class);
		context.refresh();

		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(context).build();
		boolean condition = httpHandler instanceof HttpWebHandlerAdapter;
		assertThat(condition).isTrue();
		assertThat(((HttpWebHandlerAdapter) httpHandler).getApplicationContext()).isSameAs(context);

		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		httpHandler.handle(request, response).block(ofMillis(5000));

		assertThat(response.getBodyAsString().block(ofMillis(5000))).isEqualTo("FilterB::FilterA");
	}

	@Test
	void forwardedHeaderTransformer() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ForwardedHeaderTransformerConfig.class);
		context.refresh();

		WebHttpHandlerBuilder builder = WebHttpHandlerBuilder.applicationContext(context);
		builder.filters(filters -> assertThat(filters).isEqualTo(Collections.emptyList()));
		assertThat(builder.hasForwardedHeaderTransformer()).isTrue();
	}

	@Test  // SPR-15074
	void orderedWebExceptionHandlerBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(OrderedExceptionHandlerBeanConfig.class);
		context.refresh();

		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(context).build();
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		httpHandler.handle(request, response).block(ofMillis(5000));

		assertThat(response.getBodyAsString().block(ofMillis(5000))).isEqualTo("ExceptionHandlerB");
	}

	@Test
	void configWithoutFilters() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(NoFilterConfig.class);
		context.refresh();

		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(context).build();
		MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
		MockServerHttpResponse response = new MockServerHttpResponse();
		httpHandler.handle(request, response).block(ofMillis(5000));

		assertThat(response.getBodyAsString().block(ofMillis(5000))).isEqualTo("handled");
	}

	@Test  // SPR-16972
	void cloneWithApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(NoFilterConfig.class);
		context.refresh();

		WebHttpHandlerBuilder builder = WebHttpHandlerBuilder.applicationContext(context);
		assertThat(((HttpWebHandlerAdapter) builder.build()).getApplicationContext()).isSameAs(context);
		assertThat(((HttpWebHandlerAdapter) builder.clone().build()).getApplicationContext()).isSameAs(context);
	}

	@Test
	void httpHandlerDecorator() {
		BiFunction<ServerHttpRequest, String, ServerHttpRequest> mutator =
				(req, value) -> req.mutate().headers(headers -> headers.add("My-Header", value)).build();

		AtomicBoolean success = new AtomicBoolean();
		HttpHandler httpHandler = WebHttpHandlerBuilder
				.webHandler(exchange -> {
					HttpHeaders headers = exchange.getRequest().getHeaders();
					assertThat(headers.get("My-Header")).containsExactlyInAnyOrder("1", "2", "3");
					success.set(true);
					return Mono.empty();
				})
				.httpHandlerDecorator(handler -> (req, res) -> handler.handle(mutator.apply(req, "1"), res))
				.httpHandlerDecorator(handler -> (req, res) -> handler.handle(mutator.apply(req, "2"), res))
				.httpHandlerDecorator(handler -> (req, res) -> handler.handle(mutator.apply(req, "3"), res)).build();

		httpHandler.handle(MockServerHttpRequest.get("/").build(), new MockServerHttpResponse()).block();
		assertThat(success.get()).isTrue();
	}

	@Test
	void httpHandlerDecoratorFactoryBeans() {
		HttpHandler handler = WebHttpHandlerBuilder.applicationContext(
				new AnnotationConfigApplicationContext(HttpHandlerDecoratorFactoryBeansConfig.class)).build();

		MockServerHttpResponse response = new MockServerHttpResponse();
		handler.handle(MockServerHttpRequest.get("/").build(), response).block();

		Function<String, Long> headerValue = name -> Long.valueOf(response.getHeaders().getFirst(name));
		assertThat(headerValue.apply("decoratorA")).isLessThan(headerValue.apply("decoratorB"));
		assertThat(headerValue.apply("decoratorC")).isLessThan(headerValue.apply("decoratorB"));
	}

	@Test
	void observationRegistry() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ObservationConfig.class);
		HttpHandler handler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();

		MockServerHttpResponse response = new MockServerHttpResponse();
		handler.handle(MockServerHttpRequest.get("/").build(), response).block();

		TestObservationRegistry observationRegistry = applicationContext.getBean(TestObservationRegistry.class);
		assertThat(observationRegistry).hasObservationWithNameEqualTo("http.server.requests").that()
				.hasLowCardinalityKeyValue("uri", "UNKNOWN");
	}

	@Test
	void shouldRejectDuplicateObservationConvention() {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ObservationConfig.class,
				DuplicateConventionObservationConfig.class);
		assertThatThrownBy(() -> WebHttpHandlerBuilder.applicationContext(applicationContext).build())
				.isInstanceOf(NoUniqueBeanDefinitionException.class);
	}

	private static Mono<Void> writeToResponse(ServerWebExchange exchange, String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
		return exchange.getResponse().writeWith(Flux.just(buffer));
	}


	@Configuration
	static class HttpHandlerDecoratorFactoryBeansConfig {

		@Bean
		@Order(1)
		public HttpHandlerDecoratorFactory decoratorFactoryA() {
			return delegate -> (HttpHandler) (request, response) -> {
				response.getHeaders().set("decoratorA", String.valueOf(System.nanoTime()));
				return delegate.handle(request, response);
			};
		}

		@Bean
		@Order(3)
		public HttpHandlerDecoratorFactory decoratorFactoryB() {
			return delegate -> (HttpHandler) (request, response) -> {
				response.getHeaders().set("decoratorB", String.valueOf(System.nanoTime()));
				return delegate.handle(request, response);
			};
		}

		@Bean
		@Order(2)
		public HttpHandlerDecoratorFactory decoratorFactoryC() {
			return delegate -> (HttpHandler) (request, response) -> {
				response.getHeaders().set("decoratorC", String.valueOf(System.nanoTime()));
				return delegate.handle(request, response);
			};
		}

		@Bean
		public WebHandler webHandler() {
			return exchange -> Mono.empty();
		}
	}


	@Configuration
	static class OrderedWebFilterBeanConfig {

		private static final String ATTRIBUTE = "attr";

		@Bean @Order(2)
		public WebFilter filterA() {
			return createFilter("FilterA");
		}

		@Bean @Order(1)
		public WebFilter filterB() {
			return createFilter("FilterB");
		}

		private WebFilter createFilter(String name) {
			return (exchange, chain) -> {
				String value = exchange.getAttribute(ATTRIBUTE);
				value = (value != null ? value + "::" + name : name);
				exchange.getAttributes().put(ATTRIBUTE, value);
				return chain.filter(exchange);
			};
		}

		@Bean
		public WebHandler webHandler() {
			return exchange -> {
				String value = exchange.getAttributeOrDefault(ATTRIBUTE, "none");
				return writeToResponse(exchange, value);
			};
		}
	}


	@Configuration
	static class OrderedExceptionHandlerBeanConfig {

		@Bean
		@Order(2)
		public WebExceptionHandler exceptionHandlerA() {
			return (exchange, ex) -> writeToResponse(exchange, "ExceptionHandlerA");
		}

		@Bean
		@Order(1)
		public WebExceptionHandler exceptionHandlerB() {
			return (exchange, ex) -> writeToResponse(exchange, "ExceptionHandlerB");
		}

		@Bean
		public WebHandler webHandler() {
			return exchange -> Mono.error(new Exception());
		}
	}

	@Configuration
	static class ForwardedHeaderTransformerConfig {

		@Bean
		public ForwardedHeaderTransformer forwardedHeaderTransformer() {
			return new ForwardedHeaderTransformer();
		}

		@Bean
		public WebHandler webHandler() {
			return exchange -> Mono.error(new Exception());
		}
	}

	@Configuration
	static class NoFilterConfig {

		@Bean
		public WebHandler webHandler() {
			return exchange -> writeToResponse(exchange, "handled");
		}
	}

	@Configuration
	static class ObservationConfig {

		@Bean
		public TestObservationRegistry testObservationRegistry() {
			return TestObservationRegistry.create();
		}

		@Bean
		public ServerRequestObservationConvention requestObservationConvention() {
			return new DefaultServerRequestObservationConvention();
		}

		@Bean
		public WebHandler webHandler() {
			return exchange -> exchange.getResponse().setComplete();
		}

	}

	@Configuration
	static class DuplicateConventionObservationConfig {

		@Bean
		public ServerRequestObservationConvention duplicateRequestObservationConvention() {
			return new DefaultServerRequestObservationConvention();
		}

	}

}
