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
package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.util.SignalKind;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class InvocableHandlerMethodTests {

	private ServerHttpRequest request;

	private ServerWebExchange exchange;

	private ModelMap model;


	@Before
	public void setUp() throws Exception {
		WebSessionManager sessionManager = mock(WebSessionManager.class);
		this.request = mock(ServerHttpRequest.class);
		this.exchange = new DefaultServerWebExchange(request, mock(ServerHttpResponse.class), sessionManager);
		this.model = new ExtendedModelMap();
	}


	@Test
	public void noArgsMethod() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("noArgs");

		Mono<HandlerResult> mono = hm.invokeForRequest(this.exchange, this.model);
		HandlerResult value = mono.get();

		assertNotNull(value);
		assertEquals("success", value.getReturnValue().get());
	}

	@Test
	public void resolveArgToZeroValues() throws Exception {
		when(this.request.getURI()).thenReturn(new URI("http://localhost:8080/path"));
		when(this.request.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		hm.setHandlerMethodArgumentResolvers(Collections.singletonList(
				new RequestParamMethodArgumentResolver(new GenericConversionService(), null, false)));

		Mono<HandlerResult> mono = hm.invokeForRequest(this.exchange, this.model);
		HandlerResult value = mono.get();

		assertNotNull(value);
		assertEquals("success:null", value.getReturnValue().get());
	}

	@Test
	public void resolveArgToOneValue() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Mono.just("value1"));

		Mono<HandlerResult> mono = hm.invokeForRequest(this.exchange, this.model);
		HandlerResult value = mono.get();

		assertNotNull(value);
		assertEquals("success:value1", value.getReturnValue().get());
	}

	@Test
	public void resolveArgToMultipleValues() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Flux.fromIterable(Arrays.asList("value1", "value2", "value3")));

		Mono<HandlerResult> mono = hm.invokeForRequest(this.exchange, this.model);
		HandlerResult value = mono.get();

		assertNotNull(value);
		assertEquals("success:value1", value.getReturnValue().get());
	}

	@Test
	public void noResolverForArg() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.exchange, this.model);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("No resolver for argument [0] of type [java.lang.String] on method " +
				"[" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void resolveArgumentWithThrownException() throws Exception {
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
		when(resolver.supportsParameter(any())).thenReturn(true);
		when(resolver.resolveArgument(any(), any(), any())).thenThrow(new IllegalStateException("boo"));

		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		hm.setHandlerMethodArgumentResolvers(Collections.singletonList(resolver));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.exchange, this.model);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("Exception not wrapped with helpful argument details",
				"Error resolving argument [0] of type [java.lang.String] on method " +
				"[" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void resolveArgumentWithErrorSignal() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Mono.error(new IllegalStateException("boo")));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.exchange, this.model);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("Exception not wrapped with helpful argument details",
				"Error resolving argument [0] of type [java.lang.String] on method " +
				"[" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void illegalArgumentExceptionIsWrappedWithHelpfulDetails() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Mono.just(1));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.exchange, this.model);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("Failed to invoke controller with resolved arguments: " +
				"[0][type=java.lang.Integer][value=1] " +
				"on method [" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void invocationTargetExceptionIsUnwrapped() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("exceptionMethod");

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.exchange, this.model);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("boo", ex.getMessage());
	}


	private InvocableHandlerMethod createHandlerMethod(String methodName, Class<?>... argTypes) throws Exception {
		Object controller = new TestController();
		Method method = controller.getClass().getMethod(methodName, argTypes);
		return new InvocableHandlerMethod(new HandlerMethod(controller, method));
	}

	private void addResolver(InvocableHandlerMethod handlerMethod, Publisher<Object> resolvedValue) {
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
		when(resolver.supportsParameter(any())).thenReturn(true);
		when(resolver.resolveArgument(any(), any(), any())).thenReturn(Mono.from(resolvedValue));
		handlerMethod.setHandlerMethodArgumentResolvers(Collections.singletonList(resolver));
	}

	private Throwable awaitErrorSignal(Publisher<?> publisher) throws Exception {
		Signal<?> signal = Flux.from(publisher).materialize().toList().get().get(0);
		assertEquals("Unexpected signal: " + signal, SignalKind.onError, signal.getType());
		return signal.getThrowable();
	}


	@SuppressWarnings("unused")
	private static class TestController {

		public String noArgs() {
			return "success";
		}

		public String singleArg(@RequestParam(required=false) String q) {
			return "success:" + q;
		}

		public void exceptionMethod() {
			throw new IllegalStateException("boo");
		}
	}


}
