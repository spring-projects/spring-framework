/*
 * Copyright 2002-2015 the original author or authors.
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
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.rx.Streams;
import reactor.rx.action.Signal;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.ResponseStatusException;
import org.springframework.http.server.reactive.ErrorHandlingHttpHandler;
import org.springframework.http.server.reactive.FilterChainHttpHandler;
import org.springframework.http.server.reactive.HttpExceptionHandler;
import org.springframework.http.server.reactive.HttpFilter;
import org.springframework.http.server.reactive.HttpFilterChain;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.method.annotation.ResponseBodyResultHandler;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

	public static final IllegalStateException EXCEPTION = new IllegalStateException("boo");


	private DispatcherHandler dispatcherHandler;

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;


	@Before
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext appContext = new AnnotationConfigApplicationContext();
		appContext.register(TestConfig.class);
		appContext.refresh();

		this.dispatcherHandler = new DispatcherHandler();
		this.dispatcherHandler.setApplicationContext(appContext);

		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void noHandler() throws Exception {
		this.request.setUri(new URI("/does-not-exist"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertNotNull(ex.getCause());
		assertEquals(HandlerNotFoundException.class, ex.getCause().getClass());
	}

	@Test
	public void noResolverForArgument() throws Exception {
		this.request.setUri(new URI("/uknown-argument-type"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertThat(ex.getMessage(), startsWith("No resolver for argument [0]"));
	}

	@Test
	public void controllerMethodError() throws Exception {
		this.request.setUri(new URI("/error-signal"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertSame(EXCEPTION, ex);
	}

	@Test
	public void controllerMethodWithThrownException() throws Exception {
		this.request.setUri(new URI("/raise-exception"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertSame(EXCEPTION, ex);
	}

	@Test
	public void noHandlerResultHandler() throws Exception {
		this.request.setUri(new URI("/unknown-return-type"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertThat(ex.getMessage(), startsWith("No HandlerResultHandler"));
	}

	@Test
	public void notAcceptable() throws Exception {
		this.request.setUri(new URI("/request-body"));
		this.request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		this.request.setBody(Publishers.just(ByteBuffer.wrap("body".getBytes("UTF-8"))));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertNotNull(ex.getCause());
		assertEquals(HttpMediaTypeNotAcceptableException.class, ex.getCause().getClass());
	}

	@Test
	public void requestBodyError() throws Exception {
		this.request.setUri(new URI("/request-body"));
		this.request.setBody(Publishers.error(EXCEPTION));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertSame(EXCEPTION, ex);
	}

	@Test
	public void dispatcherHandlerWithHttpExceptionHandler() throws Exception {
		this.request.setUri(new URI("/uknown-argument-type"));

		HttpExceptionHandler exceptionHandler = new ServerError500ExceptionHandler();
		HttpHandler httpHandler = new ErrorHandlingHttpHandler(this.dispatcherHandler, exceptionHandler);
		Publisher<Void> publisher = httpHandler.handle(this.request, this.response);

		Streams.wrap(publisher).toList().await(5, TimeUnit.SECONDS);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}

	@Test
	public void filterChainWithHttpExceptionHandler() throws Exception {
		this.request.setUri(new URI("/uknown-argument-type"));

		HttpHandler httpHandler;
		httpHandler = new FilterChainHttpHandler(this.dispatcherHandler, new TestHttpFilter());
		httpHandler = new ErrorHandlingHttpHandler(httpHandler, new ServerError500ExceptionHandler());
		Publisher<Void> publisher = httpHandler.handle(this.request, this.response);

		Streams.wrap(publisher).toList().await(5, TimeUnit.SECONDS);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}


	private Throwable awaitErrorSignal(Publisher<?> publisher) throws Exception {
		Signal<?> signal = Streams.wrap(publisher).materialize().toList().await(5, TimeUnit.SECONDS).get(0);
		assertEquals("Unexpected signal: " + signal, Signal.Type.ERROR, signal.getType());
		return signal.getThrowable();
	}


	@Configuration
	@SuppressWarnings("unused")
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
			List<Encoder<?>> encoders = Collections.singletonList(new StringEncoder());
			return new ResponseBodyResultHandler(encoders, new DefaultConversionService());
		}

		@Bean
		public TestController testController() {
			return new TestController();
		}
	}

	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@RequestMapping("/uknown-argument-type")
		public void uknownArgumentType(Foo arg) {
		}

		@RequestMapping("/error-signal")
		@ResponseBody
		public Publisher<String> errorSignal() {
			return Publishers.error(EXCEPTION);
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
			return Publishers.map(body, s -> "hello " + s);
		}
	}

	private static class Foo {
	}

	private static class ServerError500ExceptionHandler implements HttpExceptionHandler {

		@Override
		public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response, Throwable ex) {
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return Publishers.empty();
		}
	}

	private static class TestHttpFilter implements HttpFilter {

		@Override
		public Publisher<Void> filter(ServerHttpRequest req, ServerHttpResponse res, HttpFilterChain chain) {
			return chain.filter(req, res);
		}
	}

}
