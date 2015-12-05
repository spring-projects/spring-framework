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
package org.springframework.web.reactive.method;


import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.Publishers;
import reactor.rx.Streams;
import reactor.rx.action.Signal;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.method.annotation.RequestParamArgumentResolver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public class InvocableHandlerMethodTests {

	private static Log logger = LogFactory.getLog(InvocableHandlerMethodTests.class);


	private ServerHttpRequest request;


	@Before
	public void setUp() throws Exception {
		this.request = mock(ServerHttpRequest.class);
	}


	@Test
	public void noArgsMethod() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("noArgs");

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Object value = awaitValue(publisher);

		assertEquals("success", value);
	}

	@Test
	public void resolveArgToZeroValues() throws Exception {
		when(this.request.getURI()).thenReturn(new URI("http://localhost:8080/path"));
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		hm.setHandlerMethodArgumentResolvers(Collections.singletonList(new RequestParamArgumentResolver()));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Object value = awaitValue(publisher);

		assertEquals("success:null", value);
	}

	@Test
	public void resolveArgToOneValue() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Publishers.just("value1"));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Object value = awaitValue(publisher);

		assertEquals("success:value1", value);
	}

	@Test
	public void resolveArgToMultipleValues() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Publishers.from(Arrays.asList("value1", "value2", "value3")));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		List<Signal<HandlerResult>> signals = awaitSignals(publisher);

		assertEquals("Expected only one value: " + signals.toString(), 2, signals.size());
		assertEquals(Signal.Type.NEXT, signals.get(0).getType());
		assertEquals(Signal.Type.COMPLETE, signals.get(1).getType());
		assertEquals("success:value1", signals.get(0).get().getValue());
	}

	@Test
	public void noResolverForArg() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("No resolver for argument [0] of type [java.lang.String] on method " +
				"[" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void resolveArgumentWithThrownException() throws Exception {
		HandlerMethodArgumentResolver resolver = mock(HandlerMethodArgumentResolver.class);
		when(resolver.supportsParameter(any())).thenReturn(true);
		when(resolver.resolveArgument(any(), any())).thenThrow(new IllegalStateException("boo"));

		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		hm.setHandlerMethodArgumentResolvers(Collections.singletonList(resolver));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("Exception not wrapped with helpful argument details",
				"Error resolving argument [0] of type [java.lang.String] on method " +
				"[" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void resolveArgumentWithErrorSignal() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Publishers.error(new IllegalStateException("boo")));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("Exception not wrapped with helpful argument details",
				"Error resolving argument [0] of type [java.lang.String] on method " +
				"[" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void illegalArgumentExceptionIsWrappedWithHelpfulDetails() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("singleArg", String.class);
		addResolver(hm, Publishers.just(1));

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
		Throwable ex = awaitErrorSignal(publisher);

		assertEquals(IllegalStateException.class, ex.getClass());
		assertEquals("Failed to invoke controller with resolved arguments: " +
				"[0][type=java.lang.Integer][value=1] " +
				"on method [" + hm.getMethod().toGenericString() + "]", ex.getMessage());
	}

	@Test
	public void invocationTargetExceptionIsUnwrapped() throws Exception {
		InvocableHandlerMethod hm = createHandlerMethod("exceptionMethod");

		Publisher<HandlerResult> publisher = hm.invokeForRequest(this.request);
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
		when(resolver.resolveArgument(any(), any())).thenReturn(resolvedValue);
		handlerMethod.setHandlerMethodArgumentResolvers(Collections.singletonList(resolver));
	}

	private Object awaitValue(Publisher<HandlerResult> publisher) throws Exception {
		Object object = awaitSignal(publisher, Signal.Type.NEXT).get();
		assertEquals(HandlerResult.class, object.getClass());
		return ((HandlerResult) object).getValue();
	}

	private Throwable awaitErrorSignal(Publisher<HandlerResult> publisher) throws Exception {
		return awaitSignal(publisher, Signal.Type.ERROR).getThrowable();
	}

	@SuppressWarnings("unchecked")
	private Signal<HandlerResult> awaitSignal(Publisher<HandlerResult> publisher, Signal.Type type) throws Exception {
		Signal<HandlerResult> signal = awaitSignals(publisher).get(0);
		if (!type.equals(signal.getType()) && signal.isOnError()) {
			logger.error("Unexpected error: ", signal.getThrowable());
		}
		assertEquals("Unexpected signal: " + signal, type, signal.getType());
		return signal;
	}

	private List<Signal<HandlerResult>> awaitSignals(Publisher<HandlerResult> publisher) throws InterruptedException {
		return Streams.wrap(publisher).materialize().toList().await(5, TimeUnit.SECONDS);
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
