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

package org.springframework.web.reactive;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.resource.ResourceWebHandler;
import org.springframework.web.reactive.result.SimpleHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Test the effect of exceptions at different stages of request processing by
 * checking the error signals on the completion publisher.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ThrowableInstanceNeverThrown"})
public class DispatcherHandlerErrorTests {

	private static final IllegalStateException EXCEPTION = new IllegalStateException("boo");

	private DispatcherHandler dispatcherHandler;


	@BeforeEach
	void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		this.dispatcherHandler = new DispatcherHandler(context);
	}


	@Test
	void noHandler() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/does-not-exist"));
		Mono<Void> mono = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(mono)
				.consumeErrorWith(ex -> {
					assertThat(ex).isInstanceOf(ResponseStatusException.class);
					assertThat(ex.getMessage()).isEqualTo("404 NOT_FOUND");
				})
				.verify();

		// SPR-17475
		AtomicReference<Throwable> exceptionRef = new AtomicReference<>();
		StepVerifier.create(mono).consumeErrorWith(exceptionRef::set).verify();
		StepVerifier.create(mono).consumeErrorWith(ex -> assertThat(ex).isNotSameAs(exceptionRef.get())).verify();
	}

	@Test
	void noStaticResource() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(StaticResourceConfig.class);
		context.refresh();

		MockServerHttpRequest request = MockServerHttpRequest.get("/resources/non-existing").build();
		MockServerWebExchange exchange = MockServerWebExchange.from(request);
		new DispatcherHandler(context).handle(exchange).block();

		MockServerHttpResponse response = exchange.getResponse();
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(response.getBodyAsString().block()).isEqualTo("""
				{\
				"detail":"No static resource non-existing.",\
				"instance":"\\/resources\\/non-existing",\
				"status":404,\
				"title":"Not Found",\
				"type":"about:blank"}\
				""");
	}

	@Test
	void controllerReturnsMonoError() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/error-signal"));
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertThat(error).isSameAs(EXCEPTION))
				.verify();
	}

	@Test
	void controllerThrowsException() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/raise-exception"));
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertThat(error).isSameAs(EXCEPTION))
				.verify();
	}

	@Test
	void unknownReturnType() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown-return-type"));
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error ->
					assertThat(error)
							.isInstanceOf(IllegalStateException.class)
							.hasMessageStartingWith("No HandlerResultHandler"))
				.verify();
	}

	@Test
	void responseBodyMessageConversionError() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/request-body").accept(APPLICATION_JSON).body("body"));

		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertThat(error).isInstanceOf(NotAcceptableStatusException.class))
				.verify();
	}

	@Test
	void requestBodyError() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/request-body").body(Mono.error(EXCEPTION)));

		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertThat(error).isSameAs(EXCEPTION))
				.verify();
	}

	@Test
	void webExceptionHandler() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/unknown-argument-type"));

		List<WebExceptionHandler> handlers = Collections.singletonList(new ServerError500ExceptionHandler());
		WebHandler webHandler = new ExceptionHandlingWebHandler(this.dispatcherHandler, handlers);
		webHandler.handle(exchange).block(Duration.ofSeconds(5));

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void asyncRequestNotUsableFromExceptionHandler() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/request-not-usable-on-exception-handling"));

		StepVerifier.create(this.dispatcherHandler.handle(exchange)).verifyComplete();
	}


	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class TestConfig {

		@Bean
		public RequestMappingHandlerMapping handlerMapping() {
			return new RequestMappingHandlerMapping();
		}

		@Bean
		public RequestMappingHandlerAdapter handlerAdapter() {
			return new RequestMappingHandlerAdapter();
		}

		@Bean
		public ResponseBodyResultHandler resultHandler() {
			return new ResponseBodyResultHandler(Collections.singletonList(
					new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly())),
					new HeaderContentTypeResolver());
		}

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}


	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@RequestMapping("/error-signal")
		@ResponseBody
		public Publisher<String> errorSignal() {
			return Mono.error(EXCEPTION);
		}

		@RequestMapping("/raise-exception")
		public void raiseException() {
			throw EXCEPTION;
		}

		@RequestMapping("/unknown-return-type")
		public Foo unknownReturnType() {
			return new Foo();
		}

		@RequestMapping("/request-body")
		@ResponseBody
		public Publisher<String> requestBody(@RequestBody Publisher<String> body) {
			return Mono.from(body).map(s -> "hello " + s);
		}

		@RequestMapping("/request-not-usable-on-exception-handling")
		public void handle() throws Exception {
			throw new IllegalAccessException();
		}

		@ExceptionHandler
		public void handleException(IllegalAccessException ex) throws AsyncRequestNotUsableException {
			throw new AsyncRequestNotUsableException("Simulated response failure");
		}
	}


	private static class Foo {
	}


	@Configuration
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class StaticResourceConfig {

		@Bean
		public SimpleUrlHandlerMapping resourceMapping(ResourceWebHandler resourceWebHandler) {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setUrlMap(Map.of("/resources/**", resourceWebHandler));
			return mapping;
		}

		@Bean
		public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
			return new RequestMappingHandlerAdapter();
		}

		@Bean
		public SimpleHandlerAdapter simpleHandlerAdapter() {
			return new SimpleHandlerAdapter();
		}

		@Bean
		public ResourceWebHandler resourceWebHandler() {
			return new ResourceWebHandler();
		}

		@Bean
		public ResponseEntityResultHandler responseEntityResultHandler() {
			ServerCodecConfigurer configurer = ServerCodecConfigurer.create();
			return new ResponseEntityResultHandler(configurer.getWriters(), new HeaderContentTypeResolver());
		}

		@Bean
		GlobalExceptionHandler globalExceptionHandler() {
			return new GlobalExceptionHandler();
		}
	}


	@ControllerAdvice
	private static class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
	}


	private static class ServerError500ExceptionHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return Mono.empty();
		}
	}

}
