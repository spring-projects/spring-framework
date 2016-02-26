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
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.rx.Fluxion;
import reactor.rx.Signal;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.ResponseStatusException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.reactive.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.reactive.method.annotation.ResponseBodyResultHandler;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.handler.FilteringWebHandler;
import org.springframework.web.server.session.WebSessionManager;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

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

	private ServerWebExchange exchange;


	@Before
	public void setUp() throws Exception {
		AnnotationConfigApplicationContext appContext = new AnnotationConfigApplicationContext();
		appContext.register(TestConfig.class);
		appContext.refresh();

		this.dispatcherHandler = new DispatcherHandler();
		this.dispatcherHandler.setApplicationContext(appContext);

		WebSessionManager sessionManager = mock(WebSessionManager.class);

		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		this.response = new MockServerHttpResponse();
		this.exchange = new DefaultServerWebExchange(this.request, this.response, sessionManager);
	}


	@Test
	public void noHandler() throws Exception {
		this.request.setUri(new URI("/does-not-exist"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertNotNull(ex.getCause());
		assertEquals(HandlerNotFoundException.class, ex.getCause().getClass());
	}

	@Test
	public void noResolverForArgument() throws Exception {
		this.request.setUri(new URI("/unknown-argument-type"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertThat(ex.getMessage(), startsWith("No resolver for argument [0]"));
	}

	@Test
	public void controllerMethodError() throws Exception {
		this.request.setUri(new URI("/error-signal"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		assertSame(EXCEPTION, ex);
	}

	@Test
	public void controllerMethodWithThrownException() throws Exception {
		this.request.setUri(new URI("/raise-exception"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		assertSame(EXCEPTION, ex);
	}

	@Test
	public void noHandlerResultHandler() throws Exception {
		this.request.setUri(new URI("/unknown-return-type"));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertThat(ex.getMessage(), startsWith("No HandlerResultHandler"));
	}

	@Test
	public void notAcceptable() throws Exception {
		this.request.setUri(new URI("/request-body"));
		this.request.getHeaders().setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		DataBuffer buffer = new DefaultDataBufferAllocator().allocateBuffer()
				.write("body".getBytes("UTF-8"));
		this.request.setBody(Mono.just(buffer));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(ResponseStatusException.class, ex.getClass());
		assertNotNull(ex.getCause());
		assertEquals(HttpMediaTypeNotAcceptableException.class, ex.getCause().getClass());
	}

	@Test
	public void requestBodyError() throws Exception {
		this.request.setUri(new URI("/request-body"));
		this.request.setBody(Mono.error(EXCEPTION));

		Publisher<Void> publisher = this.dispatcherHandler.handle(this.exchange);
		Throwable ex = awaitErrorSignal(publisher);

		ex.printStackTrace();
		assertSame(EXCEPTION, ex);

	}

	@Test
	public void dispatcherHandlerWithHttpExceptionHandler() throws Exception {
		this.request.setUri(new URI("/unknown-argument-type"));

		WebExceptionHandler exceptionHandler = new ServerError500ExceptionHandler();
		WebHandler webHandler = new ExceptionHandlingWebHandler(this.dispatcherHandler, exceptionHandler);
		Publisher<Void> publisher = webHandler.handle(this.exchange);

		Fluxion.from(publisher).toList().get();
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}

	@Test
	public void filterChainWithHttpExceptionHandler() throws Exception {
		this.request.setUri(new URI("/unknown-argument-type"));

		WebHandler webHandler = new FilteringWebHandler(this.dispatcherHandler, new TestWebFilter());
		webHandler = new ExceptionHandlingWebHandler(webHandler, new ServerError500ExceptionHandler());
		Publisher<Void> publisher = webHandler.handle(this.exchange);

		Fluxion.from(publisher).toList().get();
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}


	private Throwable awaitErrorSignal(Publisher<?> publisher) throws Exception {
		Signal<?> signal = Fluxion.from(publisher).materialize().toList().get().get(0);
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
			List<Encoder<?>> encoders = Collections
					.singletonList(new StringEncoder(new DefaultDataBufferAllocator()));
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

	private static class TestWebFilter implements WebFilter {

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			return chain.filter(exchange);
		}
	}

}
