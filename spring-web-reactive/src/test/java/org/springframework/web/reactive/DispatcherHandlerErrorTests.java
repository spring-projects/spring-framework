/*
 * Copyright 2002-2016 the original author or authors.
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

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.tests.TestSubscriber;
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
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;


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

	private MockServerHttpRequest request;

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext appContext = new AnnotationConfigApplicationContext();
		appContext.register(TestConfig.class);
		appContext.refresh();

		this.dispatcherHandler = new DispatcherHandler(appContext);

		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		MockServerHttpResponse response = new MockServerHttpResponse();
		MockWebSessionManager sessionManager = new MockWebSessionManager();
		this.exchange = new DefaultServerWebExchange(this.request, response, sessionManager);
	}


	@Test
	public void noHandler() throws Exception {
		this.request.setUri(new URI("/does-not-exist"));
		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertError(ResponseStatusException.class)
				.assertErrorMessage("Request failure [status: 404, reason: \"No matching handler\"]");
	}

	@Test
	public void unknownMethodArgumentType() throws Exception {
		this.request.setUri(new URI("/unknown-argument-type"));
		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertError(IllegalStateException.class)
				.assertErrorWith(ex -> assertThat(ex.getMessage(), startsWith("No resolver for argument [0]")));
	}

	@Test
	public void controllerReturnsMonoError() throws Exception {
		this.request.setUri(new URI("/error-signal"));
		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertErrorWith(ex -> assertSame(EXCEPTION, ex));
	}

	@Test
	public void controllerThrowsException() throws Exception {
		this.request.setUri(new URI("/raise-exception"));
		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertErrorWith(ex -> assertSame(EXCEPTION, ex));
	}

	@Test
	public void unknownReturnType() throws Exception {
		this.request.setUri(new URI("/unknown-return-type"));
		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertError(IllegalStateException.class)
				.assertErrorWith(ex -> assertThat(ex.getMessage(), startsWith("No HandlerResultHandler")));
	}

	@Test
	public void responseBodyMessageConversionError() throws Exception {
		DataBuffer dataBuffer = new DefaultDataBufferFactory().allocateBuffer();
		this.request.setUri(new URI("/request-body"));
		this.request.getHeaders().add("Accept", MediaType.APPLICATION_JSON_VALUE);
		this.request.writeWith(Mono.just(dataBuffer.write("body".getBytes("UTF-8"))));

		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertError(NotAcceptableStatusException.class);
	}

	@Test
	public void requestBodyError() throws Exception {
		this.request.setUri(new URI("/request-body"));
		this.request.writeWith(Mono.error(EXCEPTION));
		Mono<Void> publisher = this.dispatcherHandler.handle(this.exchange);

		TestSubscriber.subscribe(publisher)
				.assertError(ServerWebInputException.class)
				.assertErrorWith(ex -> assertSame(EXCEPTION, ex.getCause()));
	}

	@Test
	public void webExceptionHandler() throws Exception {
		this.request.setUri(new URI("/unknown-argument-type"));

		WebExceptionHandler exceptionHandler = new ServerError500ExceptionHandler();
		WebHandler webHandler = new ExceptionHandlingWebHandler(this.dispatcherHandler, exceptionHandler);
		webHandler.handle(this.exchange).block(Duration.ofSeconds(5));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.exchange.getResponse().getStatusCode());
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
			return new ResponseBodyResultHandler(
					Collections.singletonList(new EncoderHttpMessageWriter<>(new CharSequenceEncoder())),
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

		@RequestMapping("/unknown-argument-type")
		public void unknownArgumentType(Foo arg) {
		}

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
