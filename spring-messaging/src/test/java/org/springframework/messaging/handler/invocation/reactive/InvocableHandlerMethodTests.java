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

package org.springframework.messaging.handler.invocation.reactive;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.handler.invocation.ResolvableMethod;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class InvocableHandlerMethodTests {

	private final Message<?> message = mock(Message.class);

	private final List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();


	@Test
	public void resolveArg() {
		this.resolvers.add(new StubArgumentResolver(99));
		this.resolvers.add(new StubArgumentResolver("value"));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method);

		assertEquals(1, getStubResolver(0).getResolvedParameters().size());
		assertEquals(1, getStubResolver(1).getResolvedParameters().size());
		assertEquals("99-value", value);
		assertEquals("intArg", getStubResolver(0).getResolvedParameters().get(0).getParameterName());
		assertEquals("stringArg", getStubResolver(1).getResolvedParameters().get(0).getParameterName());
	}

	@Test
	public void resolveNoArgValue() {
		this.resolvers.add(new StubArgumentResolver(Integer.class));
		this.resolvers.add(new StubArgumentResolver(String.class));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method);

		assertEquals(1, getStubResolver(0).getResolvedParameters().size());
		assertEquals(1, getStubResolver(1).getResolvedParameters().size());
		assertEquals("null-null", value);
	}

	@Test
	public void cannotResolveArg() {
		try {
			Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
			invokeAndBlock(new Handler(), method);
			fail("Expected exception");
		}
		catch (MethodArgumentResolutionException ex) {
			assertNotNull(ex.getMessage());
			assertTrue(ex.getMessage().contains("Could not resolve parameter [0]"));
		}
	}

	@Test
	public void resolveProvidedArg() {
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method, 99, "value");

		assertNotNull(value);
		assertEquals(String.class, value.getClass());
		assertEquals("99-value", value);
	}

	@Test
	public void resolveProvidedArgFirst() {
		this.resolvers.add(new StubArgumentResolver(1));
		this.resolvers.add(new StubArgumentResolver("value1"));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method, 2, "value2");

		assertEquals("2-value2", value);
	}

	@Test
	public void exceptionInResolvingArg() {
		this.resolvers.add(new InvocableHandlerMethodTests.ExceptionRaisingArgumentResolver());
		try {
			Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
			invokeAndBlock(new Handler(), method);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			// expected -  allow HandlerMethodArgumentResolver exceptions to propagate
		}
	}

	@Test
	public void illegalArgumentException() {
		this.resolvers.add(new StubArgumentResolver(Integer.class, "__not_an_int__"));
		this.resolvers.add(new StubArgumentResolver("value"));
		try {
			Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
			invokeAndBlock(new Handler(), method);
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertNotNull("Exception not wrapped", ex.getCause());
			assertTrue(ex.getCause() instanceof IllegalArgumentException);
			assertTrue(ex.getMessage().contains("Endpoint ["));
			assertTrue(ex.getMessage().contains("Method ["));
			assertTrue(ex.getMessage().contains("with argument values:"));
			assertTrue(ex.getMessage().contains("[0] [type=java.lang.String] [value=__not_an_int__]"));
			assertTrue(ex.getMessage().contains("[1] [type=java.lang.String] [value=value"));
		}
	}

	@Test
	public void invocationTargetException() {
		Method method = ResolvableMethod.on(Handler.class).argTypes(Throwable.class).resolveMethod();

		Throwable expected = new Throwable("error");
		Mono<Object> result = invoke(new Handler(), method, expected);
		StepVerifier.create(result).expectErrorSatisfies(actual -> assertSame(expected, actual)).verify();
	}

	@Test
	public void voidMethod() {
		this.resolvers.add(new StubArgumentResolver(double.class, 5.25));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0.0d)).method();
		Handler handler = new Handler();
		Object value = invokeAndBlock(handler, method);

		assertNull(value);
		assertEquals(1, getStubResolver(0).getResolvedParameters().size());
		assertEquals("5.25", handler.getResult());
		assertEquals("amount", getStubResolver(0).getResolvedParameters().get(0).getParameterName());
	}

	@Test
	public void voidMonoMethod() {
		Method method = ResolvableMethod.on(Handler.class).mockCall(Handler::handleAsync).method();
		Handler handler = new Handler();
		Object value = invokeAndBlock(handler, method);

		assertNull(value);
		assertEquals("success", handler.getResult());
	}


	@Nullable
	private Object invokeAndBlock(Object handler, Method method, Object... providedArgs) {
		return invoke(handler, method, providedArgs).block(Duration.ofSeconds(5));
	}

	private Mono<Object> invoke(Object handler, Method method, Object... providedArgs) {
		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(handler, method);
		handlerMethod.setArgumentResolvers(this.resolvers);
		return handlerMethod.invoke(this.message, providedArgs);
	}

	private StubArgumentResolver getStubResolver(int index) {
		return (StubArgumentResolver) this.resolvers.get(index);
	}



	@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue"})
	private static class Handler {

		private AtomicReference<String> result = new AtomicReference<>();


		public String getResult() {
			return this.result.get();
		}

		String handle(Integer intArg, String stringArg) {
			return intArg + "-" + stringArg;
		}

		void handle(double amount) {
			this.result.set(String.valueOf(amount));
		}

		void handleWithException(Throwable ex) throws Throwable {
			throw ex;
		}

		Mono<Void> handleAsync() {
			return Mono.delay(Duration.ofMillis(100)).thenEmpty(Mono.defer(() -> {
				this.result.set("success");
				return Mono.empty();
			}));
		}
	}


	private static class ExceptionRaisingArgumentResolver implements HandlerMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return true;
		}

		@Override
		public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
			return Mono.error(new IllegalArgumentException("oops, can't read"));
		}
	}

}
