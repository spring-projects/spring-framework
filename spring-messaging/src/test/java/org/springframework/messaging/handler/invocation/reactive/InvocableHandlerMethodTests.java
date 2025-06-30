/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.messaging.handler.invocation.ResolvableMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class InvocableHandlerMethodTests {

	private final Message<?> message = mock();

	private final List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();


	@Test
	void resolveArg() {
		this.resolvers.add(new StubArgumentResolver(99));
		this.resolvers.add(new StubArgumentResolver("value"));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method);

		assertThat(getStubResolver(0).getResolvedParameters()).hasSize(1);
		assertThat(getStubResolver(1).getResolvedParameters()).hasSize(1);
		assertThat(value).isEqualTo("99-value");
		assertThat(getStubResolver(0).getResolvedParameters().get(0).getParameterName()).isEqualTo("intArg");
		assertThat(getStubResolver(1).getResolvedParameters().get(0).getParameterName()).isEqualTo("stringArg");
	}

	@Test
	void resolveNoArgValue() {
		this.resolvers.add(new StubArgumentResolver(Integer.class));
		this.resolvers.add(new StubArgumentResolver(String.class));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method);

		assertThat(getStubResolver(0).getResolvedParameters()).hasSize(1);
		assertThat(getStubResolver(1).getResolvedParameters()).hasSize(1);
		assertThat(value).isEqualTo("null-null");
	}

	@Test
	void cannotResolveArg() {
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		assertThatExceptionOfType(MethodArgumentResolutionException.class).isThrownBy(() ->
				invokeAndBlock(new Handler(), method))
			.withMessageContaining("Could not resolve parameter [0]");
	}

	@Test
	void resolveProvidedArg() {
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method, 99, "value");

		assertThat(value).isNotNull();
		assertThat(value.getClass()).isEqualTo(String.class);
		assertThat(value).isEqualTo("99-value");
	}

	@Test
	void resolveProvidedArgFirst() {
		this.resolvers.add(new StubArgumentResolver(1));
		this.resolvers.add(new StubArgumentResolver("value1"));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		Object value = invokeAndBlock(new Handler(), method, 2, "value2");

		assertThat(value).isEqualTo("2-value2");
	}

	@Test
	void exceptionInResolvingArg() {
		this.resolvers.add(new InvocableHandlerMethodTests.ExceptionRaisingArgumentResolver());
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		assertThatIllegalArgumentException().isThrownBy(() ->
				invokeAndBlock(new Handler(), method));
	}

	@Test
	void illegalArgumentException() {
		this.resolvers.add(new StubArgumentResolver(Integer.class, "__not_an_int__"));
		this.resolvers.add(new StubArgumentResolver("value"));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0, "")).method();
		assertThatIllegalStateException().isThrownBy(() ->
				invokeAndBlock(new Handler(), method))
			.withCauseInstanceOf(IllegalArgumentException.class)
			.withMessageContaining("Endpoint [")
			.withMessageContaining("Method [")
			.withMessageContaining("with argument values:")
			.withMessageContaining("[0] [type=java.lang.String] [value=__not_an_int__]")
			.withMessageContaining("[1] [type=java.lang.String] [value=value");
	}

	@Test
	void invocationTargetException() {
		Method method = ResolvableMethod.on(Handler.class).argTypes(Throwable.class).resolveMethod();

		Throwable expected = new Throwable("error");
		Mono<Object> result = invoke(new Handler(), method, expected);
		StepVerifier.create(result).expectErrorSatisfies(actual -> assertThat(actual).isSameAs(expected)).verify();
	}

	@Test
	void voidMethod() {
		this.resolvers.add(new StubArgumentResolver(double.class, 5.25));
		Method method = ResolvableMethod.on(Handler.class).mockCall(c -> c.handle(0.0d)).method();
		Handler handler = new Handler();
		Object value = invokeAndBlock(handler, method);

		assertThat(value).isNull();
		assertThat(getStubResolver(0).getResolvedParameters()).hasSize(1);
		assertThat(handler.getResult()).isEqualTo("5.25");
		assertThat(getStubResolver(0).getResolvedParameters().get(0).getParameterName()).isEqualTo("amount");
	}

	@Test
	void voidMonoMethod() {
		Method method = ResolvableMethod.on(Handler.class).mockCall(Handler::handleAsync).method();
		Handler handler = new Handler();
		Object value = invokeAndBlock(handler, method);

		assertThat(value).isNull();
		assertThat(handler.getResult()).isEqualTo("success");
	}


	private @Nullable Object invokeAndBlock(Object handler, Method method, Object... providedArgs) {
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

		public Handler() {
		}

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
