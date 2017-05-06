/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.result.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.springframework.http.MediaType.*;

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


	@Before
	public void setup() throws Exception {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestConfig.class);
		ctx.refresh();
		this.dispatcherHandler = new DispatcherHandler(ctx);
	}


	@Test
	public void noHandler() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/does-not-exist").toExchange();
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> {
					assertThat(error, instanceOf(ResponseStatusException.class));
					assertThat(error.getMessage(), is("Response status 404 with reason \"No matching handler\""));
				})
				.verify();
	}

	@Test
	public void controllerReturnsMonoError() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/error-signal").toExchange();
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertSame(EXCEPTION, error))
				.verify();
	}

	@Test
	public void controllerThrowsException() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/raise-exception").toExchange();
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertSame(EXCEPTION, error))
				.verify();
	}

	@Test
	public void unknownReturnType() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/unknown-return-type").toExchange();
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> {
					assertThat(error, instanceOf(IllegalStateException.class));
					assertThat(error.getMessage(), startsWith("No HandlerResultHandler"));
				})
				.verify();
	}

	@Test
	public void responseBodyMessageConversionError() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.post("/request-body")
				.accept(APPLICATION_JSON).body("body")
				.toExchange();

		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertThat(error, instanceOf(NotAcceptableStatusException.class)))
				.verify();
	}

	@Test
	public void requestBodyError() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.post("/request-body")
				.body(Mono.error(EXCEPTION))
				.toExchange();
		
		Mono<Void> publisher = this.dispatcherHandler.handle(exchange);

		StepVerifier.create(publisher)
				.consumeErrorWith(error -> assertSame(EXCEPTION, error))
				.verify();
	}

	@Test
	public void webExceptionHandler() throws Exception {
		ServerWebExchange exchange = MockServerHttpRequest.get("/unknown-argument-type").toExchange();

		List<WebExceptionHandler> handlers = Collections.singletonList(new ServerError500ExceptionHandler());
		WebHandler webHandler = new ExceptionHandlingWebHandler(this.dispatcherHandler, handlers);
		webHandler.handle(exchange).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exchange.getResponse().getStatusCode());
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
		public void raiseException() throws Exception {
			throw EXCEPTION;
		}

		@RequestMapping("/unknown-return-type")
		public Foo unknownReturnType() throws Exception {
			return new Foo();
		}

		@RequestMapping("/request-body")
		@ResponseBody
		public Publisher<String> requestBody(@RequestBody Publisher<String> body) {
			return Mono.from(body).map(s -> "hello " + s);
		}
	}


	private static class Foo {
	}


	private static class ServerError500ExceptionHandler implements WebExceptionHandler {

		@Override
		public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
			exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return Mono.empty();
		}
	}

}
