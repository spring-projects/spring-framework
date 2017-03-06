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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.web.method.ResolvableMethod.on;

/**
 * Unit tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class InvocableHandlerMethodTests {

	private ServerWebExchange exchange;


	@Before
	public void setup() throws Exception {
		this.exchange = new DefaultServerWebExchange(
				MockServerHttpRequest.get("http://localhost:8080/path").build(),
				new MockServerHttpResponse(),
				new MockWebSessionManager());
	}


	@Test
	public void invokeMethodWithNoArguments() throws Exception {
		Method method = on(TestController.class).mockCall(TestController::noArgs).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);
		assertHandlerResultValue(mono, "success");
	}

	@Test
	public void invokeMethodWithNoValue() throws Exception {

		Mono<Object> resolvedValue = Mono.empty();
		Method method = on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, resolverFor(resolvedValue));

		assertHandlerResultValue(mono, "success:null");
	}

	@Test
	public void invokeMethodWithValue() throws Exception {

		Mono<Object> resolvedValue = Mono.just("value1");
		Method method = on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, resolverFor(resolvedValue));

		assertHandlerResultValue(mono, "success:value1");
	}

	@Test
	public void noMatchingResolver() throws Exception {

		Method method = on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), is("No suitable resolver for argument 0 of type 'java.lang.String' " +
					"on " + method.toGenericString()));
		}
	}

	@Test
	public void resolverThrowsException() throws Exception {

		Mono<Object> resolvedValue = Mono.error(new UnsupportedMediaTypeStatusException("boo"));
		Method method = on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, resolverFor(resolvedValue));

		try {
			mono.block();
			fail("Expected UnsupportedMediaTypeStatusException");
		}
		catch (UnsupportedMediaTypeStatusException ex) {
			assertThat(ex.getMessage(), is("Request failure [status: 415, reason: \"boo\"]"));
		}
	}

	@Test
	public void illegalArgumentExceptionIsWrappedWithInvocationDetails() throws Exception {

		Mono<Object> resolvedValue = Mono.just(1);
		Method method = on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, resolverFor(resolvedValue));

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), is("Failed to invoke handler method with resolved arguments: " +
					"[0][type=java.lang.Integer][value=1] " +
					"on " + method.toGenericString()));
		}
	}

	@Test
	public void invocationTargetExceptionIsUnwrapped() throws Exception {

		Method method = on(TestController.class).mockCall(TestController::exceptionMethod).method();
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
	public void invokeMethodWithResponseStatus() throws Exception {

		Method method = on(TestController.class).annotPresent(ResponseStatus.class).resolveMethod();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertHandlerResultValue(mono, "created");
		assertThat(this.exchange.getResponse().getStatusCode(), is(HttpStatus.CREATED));
	}


	private Mono<HandlerResult> invoke(Object handler, Method method) {
		return this.invoke(handler, method, new HandlerMethodArgumentResolver[0]);
	}

	private Mono<HandlerResult> invoke(Object handler, Method method,
			HandlerMethodArgumentResolver... resolver) {

		InvocableHandlerMethod hm = new InvocableHandlerMethod(handler, method);
		hm.setArgumentResolvers(Arrays.asList(resolver));
		return hm.invoke(this.exchange, new BindingContext());
	}

	private <T> HandlerMethodArgumentResolver resolverFor(Mono<Object> resolvedValue) {
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
		when(resolver.supportsParameter(any())).thenReturn(true);
		when(resolver.resolveArgument(any(), any(), any())).thenReturn(resolvedValue);
		return resolver;
	}

	private void assertHandlerResultValue(Mono<HandlerResult> mono, String expected) {
		StepVerifier.create(mono)
				.consumeNextWith(result -> {
					Optional<?> optional = result.getReturnValue();
					assertTrue(optional.isPresent());
					assertEquals(expected, optional.get());
				})
				.expectComplete()
				.verify();
	}


	@SuppressWarnings("unused")
	private static class TestController {

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

}
