/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.mock.http.server.reactive.test.MockServerHttpRequest.*;

/**
 * Unit tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class InvocableHandlerMethodTests {

	private final MockServerWebExchange exchange = MockServerWebExchange.from(get("http://localhost:8080/path"));


	@Test
	public void invokeAndHandle_VoidWithResponseStatus() {
		Method method = ResolvableMethod.on(VoidController.class).mockCall(VoidController::responseStatus).method();
		HandlerResult result = invokeForResult(new VoidController(), method);

		assertNull("Expected no result (i.e. fully handled)", result);
		assertEquals(HttpStatus.BAD_REQUEST, this.exchange.getResponse().getStatusCode());
	}

	@Test
	public void invokeAndHandle_withResponse() {
		ServerHttpResponse response = this.exchange.getResponse();
		Method method = ResolvableMethod.on(VoidController.class).mockCall(c -> c.response(response)).method();
		HandlerResult result = invokeForResult(new VoidController(), method, stubResolver(response));

		assertNull("Expected no result (i.e. fully handled)", result);
		assertEquals("bar", this.exchange.getResponse().getHeaders().getFirst("foo"));
	}

	@Test
	public void invokeAndHandle_withResponseAndMonoVoid() {
		ServerHttpResponse response = this.exchange.getResponse();
		Method method = ResolvableMethod.on(VoidController.class).mockCall(c -> c.responseMonoVoid(response)).method();
		HandlerResult result = invokeForResult(new VoidController(), method, stubResolver(response));

		assertNull("Expected no result (i.e. fully handled)", result);
		assertEquals("body", this.exchange.getResponse().getBodyAsString().block(Duration.ZERO));
	}

	@Test
	public void invokeAndHandle_withExchange() {
		Method method = ResolvableMethod.on(VoidController.class).mockCall(c -> c.exchange(exchange)).method();
		HandlerResult result = invokeForResult(new VoidController(), method, stubResolver(this.exchange));

		assertNull("Expected no result (i.e. fully handled)", result);
		assertEquals("bar", this.exchange.getResponse().getHeaders().getFirst("foo"));
	}

	@Test
	public void invokeAndHandle_withExchangeAndMonoVoid() {
		Method method = ResolvableMethod.on(VoidController.class).mockCall(c -> c.exchangeMonoVoid(exchange)).method();
		HandlerResult result = invokeForResult(new VoidController(), method, stubResolver(this.exchange));

		assertNull("Expected no result (i.e. fully handled)", result);
		assertEquals("body", this.exchange.getResponse().getBodyAsString().block(Duration.ZERO));
	}

	@Test
	public void invokeAndHandle_withNotModified() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/").ifModifiedSince(10 * 1000 * 1000));

		Method method = ResolvableMethod.on(VoidController.class).mockCall(c -> c.notModified(exchange)).method();
		HandlerResult result = invokeForResult(new VoidController(), method, stubResolver(exchange));

		assertNull("Expected no result (i.e. fully handled)", result);
	}

	@Test
	public void invokeMethodWithNoArguments() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::noArgs).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);
		assertHandlerResultValue(mono, "success");
	}

	@Test
	public void invokeMethodWithNoValue() {
		Method method = resolveOn().mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, stubResolver(Mono.empty()));

		assertHandlerResultValue(mono, "success:null");
	}

	private ResolvableMethod.Builder<TestController> resolveOn() {
		return ResolvableMethod.on(TestController.class);
	}

	@Test
	public void invokeMethodWithValue() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, stubResolver("value1"));

		assertHandlerResultValue(mono, "success:value1");
	}

	@Test
	public void noMatchingResolver() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), is("Could not resolve parameter [0] in " +
					method.toGenericString() + ": No suitable resolver"));
		}
	}

	@Test
	public void resolverThrowsException() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method,
				stubResolver(Mono.error(new UnsupportedMediaTypeStatusException("boo"))));

		try {
			mono.block();
			fail("Expected UnsupportedMediaTypeStatusException");
		}
		catch (UnsupportedMediaTypeStatusException ex) {
			assertThat(ex.getMessage(), is("415 UNSUPPORTED_MEDIA_TYPE \"boo\""));
		}
	}

	@Test
	public void illegalArgumentException() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, stubResolver(1));

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertNotNull("Exception not wrapped", ex.getCause());
			assertTrue(ex.getCause() instanceof IllegalArgumentException);
			assertTrue(ex.getMessage().contains("Controller ["));
			assertTrue(ex.getMessage().contains("Method ["));
			assertTrue(ex.getMessage().contains("with argument values:"));
			assertTrue(ex.getMessage().contains("[0] [type=java.lang.Integer] [value=1]"));
		}
	}

	@Test
	public void invocationTargetExceptionIsUnwrapped() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::exceptionMethod).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), is("boo"));
		}
	}

	@Test
	public void invokeMethodWithResponseStatus() {
		Method method = ResolvableMethod.on(TestController.class).annotPresent(ResponseStatus.class).resolveMethod();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertHandlerResultValue(mono, "created");
		assertThat(this.exchange.getResponse().getStatusCode(), is(HttpStatus.CREATED));
	}


	@Nullable
	private HandlerResult invokeForResult(Object handler, Method method, HandlerMethodArgumentResolver... resolvers) {
		return invoke(handler, method, resolvers).block(Duration.ZERO);
	}

	private Mono<HandlerResult> invoke(Object handler, Method method, HandlerMethodArgumentResolver... resolvers) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handler, method);
		invocable.setArgumentResolvers(Arrays.asList(resolvers));
		return invocable.invoke(this.exchange, new BindingContext());
	}

	private <T> HandlerMethodArgumentResolver stubResolver(Object stubValue) {
		return stubResolver(Mono.just(stubValue));
	}

	private <T> HandlerMethodArgumentResolver stubResolver(Mono<Object> stubValue) {
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
		when(resolver.supportsParameter(any())).thenReturn(true);
		when(resolver.resolveArgument(any(), any(), any())).thenReturn(stubValue);
		return resolver;
	}

	private void assertHandlerResultValue(Mono<HandlerResult> mono, String expected) {
		StepVerifier.create(mono)
				.consumeNextWith(result -> assertEquals(expected, result.getReturnValue()))
				.expectComplete()
				.verify();
	}


	@SuppressWarnings("unused")
	static class TestController {

		public String noArgs() {
			return "success";
		}

		public String singleArg(String q) {
			return "success:" + q;
		}

		public void exceptionMethod() {
			throw new IllegalStateException("boo");
		}

		@ResponseStatus(HttpStatus.CREATED)
		public String responseStatus() {
			return "created";
		}
	}


	@SuppressWarnings("unused")
	static class VoidController {

		@ResponseStatus(HttpStatus.BAD_REQUEST)
		public void responseStatus() {
		}

		public void response(ServerHttpResponse response) {
			response.getHeaders().add("foo", "bar");
		}

		public Mono<Void> responseMonoVoid(ServerHttpResponse response) {
			return response.writeWith(getBody("body"));
		}

		public void exchange(ServerWebExchange exchange) {
			exchange.getResponse().getHeaders().add("foo", "bar");
		}

		public Mono<Void> exchangeMonoVoid(ServerWebExchange exchange) {
			return exchange.getResponse().writeWith(getBody("body"));
		}

		@Nullable
		public String notModified(ServerWebExchange exchange) {
			if (exchange.checkNotModified(Instant.ofEpochMilli(1000 * 1000))) {
				return null;
			}
			return "body";
		}

		private Flux<DataBuffer> getBody(String body) {
			try {
				return Flux.just(new DefaultDataBufferFactory().wrap(body.getBytes("UTF-8")));
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
