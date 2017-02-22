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

import java.util.Collections;
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
import org.springframework.web.reactive.result.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
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
		InvocableHandlerMethod hm = handlerMethod("noArgs");
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

		assertHandlerResultValue(mono, "success");
	}

	@Test
	public void invokeMethodWithNoValue() throws Exception {
		InvocableHandlerMethod hm = handlerMethod("singleArg");
		addResolver(hm, Mono.empty());
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

		assertHandlerResultValue(mono, "success:null");
	}

	@Test
	public void invokeMethodWithValue() throws Exception {
		InvocableHandlerMethod hm = handlerMethod("singleArg");
		addResolver(hm, Mono.just("value1"));
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

		assertHandlerResultValue(mono, "success:value1");
	}

	@Test
	public void noMatchingResolver() throws Exception {
		InvocableHandlerMethod hm = handlerMethod("singleArg");
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), is("No suitable resolver for argument 0 of type 'java.lang.String' " +
					"on " + hm.getMethod().toGenericString()));
		}
	}

	@Test
	public void resolverThrowsException() throws Exception {
		InvocableHandlerMethod hm = handlerMethod("singleArg");
		addResolver(hm, Mono.error(new UnsupportedMediaTypeStatusException("boo")));
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

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
		InvocableHandlerMethod hm = handlerMethod("singleArg");
		addResolver(hm, Mono.just(1));
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

		try {
			mono.block();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), is("Failed to invoke handler method with resolved arguments: " +
					"[0][type=java.lang.Integer][value=1] " +
					"on " + hm.getMethod().toGenericString()));
		}
	}

	@Test
	public void invocationTargetExceptionIsUnwrapped() throws Exception {
		InvocableHandlerMethod hm = handlerMethod("exceptionMethod");
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

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
		InvocableHandlerMethod hm = handlerMethod("responseStatus");
		Mono<HandlerResult> mono = hm.invoke(this.exchange, new BindingContext());

		assertHandlerResultValue(mono, "created");
		assertThat(this.exchange.getResponse().getStatusCode(), is(HttpStatus.CREATED));
	}


	private InvocableHandlerMethod handlerMethod(String name) throws Exception {
		TestController controller = new TestController();
		return ResolvableMethod.on(controller).name(name).resolveHandlerMethod();
	}

	private void addResolver(InvocableHandlerMethod handlerMethod, Mono<Object> resolvedValue) {
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
		when(resolver.supportsParameter(any())).thenReturn(true);
		when(resolver.resolveArgument(any(), any(), any())).thenReturn(resolvedValue);
		handlerMethod.setArgumentResolvers(Collections.singletonList(resolver));
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
