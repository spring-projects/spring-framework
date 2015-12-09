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
package org.springframework.http.server.reactive;


import java.net.URI;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.rx.Streams;
import reactor.rx.action.Signal;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class ErrorHandlingHttpHandlerTests {

	private MockServerHttpRequest request;

	private MockServerHttpResponse response;


	@Before
	public void setUp() throws Exception {
		this.request = new MockServerHttpRequest(HttpMethod.GET, new URI("http://localhost:8080"));
		this.response = new MockServerHttpResponse();
	}


	@Test
	public void handleErrorSignal() throws Exception {
		HttpExceptionHandler exceptionHandler = new UnresolvedExceptionHandler();
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"));
		HttpHandler handler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandler);

		Publisher<Void> publisher = handler.handle(this.request, this.response);
		Streams.wrap(publisher).toList().await(5, TimeUnit.SECONDS);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}

	@Test
	public void handleErrorSignalWithMultipleHttpErrorHandlers() throws Exception {
		HttpExceptionHandler[] exceptionHandlers = new HttpExceptionHandler[] {
				new UnresolvedExceptionHandler(),
				new UnresolvedExceptionHandler(),
				new HttpStatusExceptionHandler(HttpStatus.INTERNAL_SERVER_ERROR),
				new UnresolvedExceptionHandler()
		};
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"));
		HttpHandler httpHandler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandlers);

		Publisher<Void> publisher = httpHandler.handle(this.request, this.response);
		Streams.wrap(publisher).toList().await(5, TimeUnit.SECONDS);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}

	@Test
	public void unresolvedException() throws Exception {
		HttpExceptionHandler exceptionHandler = new UnresolvedExceptionHandler();
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"));
		HttpHandler httpHandler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandler);

		Publisher<Void> publisher = httpHandler.handle(this.request, this.response);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals("boo", ex.getMessage());
		assertNull(this.response.getStatus());
	}

	@Test
	public void thrownExceptionBecomesErrorSignal() throws Exception {
		HttpExceptionHandler exceptionHandler = new HttpStatusExceptionHandler(HttpStatus.INTERNAL_SERVER_ERROR);
		HttpHandler targetHandler = new TestHttpHandler(new IllegalStateException("boo"), true);
		HttpHandler handler = new ErrorHandlingHttpHandler(targetHandler, exceptionHandler);

		Publisher<Void> publisher = handler.handle(this.request, this.response);
		Streams.wrap(publisher).toList().await(5, TimeUnit.SECONDS);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, this.response.getStatus());
	}


	private Throwable awaitErrorSignal(Publisher<?> publisher) throws Exception {
		Signal<?> signal = Streams.wrap(publisher).materialize().toList().await(5, TimeUnit.SECONDS).get(0);
		assertEquals("Unexpected signal: " + signal, Signal.Type.ERROR, signal.getType());
		return signal.getThrowable();
	}


	private static class TestHttpHandler implements HttpHandler {

		private final Throwable exception;

		private final boolean raise;


		public TestHttpHandler(Throwable exception) {
			this(exception, false);
		}

		public TestHttpHandler(Throwable exception, boolean raise) {
			this.exception = exception;
			this.raise = raise;
			assertTrue(exception instanceof RuntimeException || !this.raise);
		}

		@Override
		public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			if (this.raise) {
				throw (RuntimeException) exception;
			}
			return Publishers.error(this.exception);
		}
	}


	/** Leave the exception unresolved. */
	private static class UnresolvedExceptionHandler implements HttpExceptionHandler {

		@Override
		public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response, Throwable ex) {
			return Publishers.error(ex);
		}
	}

	/** Set the response status to the given HttpStatus. */
	private static class HttpStatusExceptionHandler implements HttpExceptionHandler {

		private final HttpStatus status;

		public HttpStatusExceptionHandler(HttpStatus status) {
			this.status = status;
		}

		@Override
		public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response, Throwable ex) {
			response.setStatusCode(this.status);
			return Publishers.empty();
		}
	}

}
