/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.result.method;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest.get;

/**
 * Tests for {@link InvocableHandlerMethod}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
class InvocableHandlerMethodTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5);


	private final MockServerWebExchange exchange = MockServerWebExchange.from(get("http://localhost:8080/path"));

	private final List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();


	@Test
	void resolveArg() {
		this.resolvers.add(stubResolver("value1"));
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertHandlerResultValue(mono, "success:value1");
	}

	@Test
	void resolveArgOnSchedulerThread() {
		this.resolvers.add(stubResolver(Mono.<Object>just("success").publishOn(Schedulers.newSingle("wrong"))));
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArgThread(null)).method();
		Mono<HandlerResult> mono = invokeOnScheduler(Schedulers.newSingle("good"), new TestController(), method);

		assertHandlerResultValue(mono, "success on thread: good-", false);
	}

	@Test
	void resolveNoArgValue() {
		this.resolvers.add(stubResolver(Mono.empty()));
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertHandlerResultValue(mono, "success:null");
	}

	@Test
	void resolveNoArgs() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::noArgs).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertHandlerResultValue(mono, "success");
	}

	@Test
	void resolveNoArgsOnSchedulerThread() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::noArgsThread).method();
		Mono<HandlerResult> mono = invokeOnScheduler(Schedulers.newSingle("good"), new TestController(), method);

		assertHandlerResultValue(mono, "on thread: good-", false);
	}

	@Test
	void cannotResolveArg() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertThatIllegalStateException()
			.isThrownBy(mono::block)
			.withMessage("Could not resolve parameter [0] in %s: No suitable resolver", method.toGenericString());
	}

	@Test
	void resolveProvidedArg() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, "value1");

		assertHandlerResultValue(mono, "success:value1");
	}

	@Test
	void resolveProvidedArgFirst() {
		this.resolvers.add(stubResolver("value1"));
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method, "value2");

		assertHandlerResultValue(mono, "success:value2");
	}

	@Test
	void exceptionInResolvingArg() {
		this.resolvers.add(stubResolver(Mono.error(new UnsupportedMediaTypeStatusException("boo"))));
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertThatExceptionOfType(UnsupportedMediaTypeStatusException.class)
			.isThrownBy(mono::block)
			.withMessage("415 UNSUPPORTED_MEDIA_TYPE \"boo\"");
	}

	@Test
	void illegalArgumentException() {
		this.resolvers.add(stubResolver(1));
		Method method = ResolvableMethod.on(TestController.class).mockCall(o -> o.singleArg(null)).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertThatIllegalStateException()
			.isThrownBy(mono::block)
			.withCauseInstanceOf(IllegalArgumentException.class)
			.withMessageContaining("Controller [")
			.withMessageContaining("Method [")
			.withMessageContaining("with argument values:")
			.withMessageContaining("[0] [type=java.lang.Integer] [value=1]");
	}

	@Test
	void invocationTargetException() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::exceptionMethod).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertThatIllegalStateException()
			.isThrownBy(mono::block)
			.withMessage("boo");
	}

	@Test
	void responseStatusAnnotation() {
		Method method = ResolvableMethod.on(TestController.class).mockCall(TestController::created).method();
		Mono<HandlerResult> mono = invoke(new TestController(), method);

		assertHandlerResultValue(mono, "created");
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	@Test
	void voidMethodWithResponseArg() {
		ServerHttpResponse response = this.exchange.getResponse();
		this.resolvers.add(stubResolver(response));
		Method method = ResolvableMethod.on(TestController.class).mockCall(c -> c.response(response)).method();
		HandlerResult result = invokeForResult(new TestController(), method);

		assertThat(result).as("Expected no result (i.e. fully handled)").isNull();
		assertThat(this.exchange.getResponse().getHeaders().getFirst("foo")).isEqualTo("bar");
	}

	@Test
	void voidMonoMethodWithResponseArg() {
		ServerHttpResponse response = this.exchange.getResponse();
		this.resolvers.add(stubResolver(response));
		Method method = ResolvableMethod.on(TestController.class).mockCall(c -> c.responseMonoVoid(response)).method();
		HandlerResult result = invokeForResult(new TestController(), method);

		assertThat(result).as("Expected no result (i.e. fully handled)").isNull();
		assertThat(this.exchange.getResponse().getBodyAsString().block(TIMEOUT)).isEqualTo("body");
	}

	@Test
	void voidMethodWithExchangeArg() {
		this.resolvers.add(stubResolver(this.exchange));
		Method method = ResolvableMethod.on(TestController.class).mockCall(c -> c.exchange(exchange)).method();
		HandlerResult result = invokeForResult(new TestController(), method);

		assertThat(result).as("Expected no result (i.e. fully handled)").isNull();
		assertThat(this.exchange.getResponse().getHeaders().getFirst("foo")).isEqualTo("bar");
	}

	@Test
	void voidMonoMethodWithExchangeArg() {
		this.resolvers.add(stubResolver(this.exchange));
		Method method = ResolvableMethod.on(TestController.class).mockCall(c -> c.exchangeMonoVoid(exchange)).method();
		HandlerResult result = invokeForResult(new TestController(), method);

		assertThat(result).as("Expected no result (i.e. fully handled)").isNull();
		assertThat(this.exchange.getResponse().getBodyAsString().block(TIMEOUT)).isEqualTo("body");
	}

	@Test
	void checkNotModified() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/").ifModifiedSince(10 * 1000 * 1000).build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		this.resolvers.add(stubResolver(exchange));
		Method method = ResolvableMethod.on(TestController.class).mockCall(c -> c.notModified(exchange)).method();
		HandlerResult result = invokeForResult(new TestController(), method);

		assertThat(result).as("Expected no result (i.e. fully handled)").isNull();
	}


	@Nullable
	private HandlerResult invokeForResult(Object handler, Method method, Object... providedArgs) {
		return invoke(handler, method, providedArgs).block(Duration.ofSeconds(5));
	}

	private Mono<HandlerResult> invoke(Object handler, Method method, Object... providedArgs) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handler, method);
		invocable.setArgumentResolvers(this.resolvers);
		return invocable.invoke(this.exchange, new BindingContext(), providedArgs);
	}

	private Mono<HandlerResult> invokeOnScheduler(Scheduler scheduler, Object handler, Method method, Object... providedArgs) {
		InvocableHandlerMethod invocable = new InvocableHandlerMethod(handler, method);
		invocable.setArgumentResolvers(this.resolvers);
		invocable.setInvocationScheduler(scheduler);
		return invocable.invoke(this.exchange, new BindingContext(), providedArgs);
	}

	private HandlerMethodArgumentResolver stubResolver(Object stubValue) {
		return stubResolver(Mono.just(stubValue));
	}

	private HandlerMethodArgumentResolver stubResolver(Mono<Object> stubValue) {
		HandlerMethodArgumentResolver resolver = mock();
		given(resolver.supportsParameter(any())).willReturn(true);
		given(resolver.resolveArgument(any(), any(), any())).willReturn(stubValue);
		return resolver;
	}

	private void assertHandlerResultValue(Mono<HandlerResult> mono, String expected) {
		this.assertHandlerResultValue(mono, expected, true);
	}

	private void assertHandlerResultValue(Mono<HandlerResult> mono, String expected, boolean strict) {
		StepVerifier.create(mono)
				.assertNext(result -> {
					if (strict) {
						assertThat(result.getReturnValue()).isEqualTo(expected);
					}
					else {
						assertThat(String.valueOf(result.getReturnValue())).startsWith(expected);
					}
				})
				.expectComplete()
				.verify();
	}


	@SuppressWarnings({"unused", "UnusedReturnValue", "SameParameterValue"})
	static class TestController {

		String singleArg(String q) {
			return "success:" + q;
		}

		String noArgs() {
			return "success";
		}

		String singleArgThread(String q) {
			return q + " on thread: " + Thread.currentThread().getName();
		}

		String noArgsThread() {
			return "on thread: " + Thread.currentThread().getName();
		}

		void exceptionMethod() {
			throw new IllegalStateException("boo");
		}

		@ResponseStatus(HttpStatus.CREATED)
		String created() {
			return "created";
		}

		void response(ServerHttpResponse response) {
			response.getHeaders().add("foo", "bar");
		}

		Mono<Void> responseMonoVoid(ServerHttpResponse response) {
			return Mono.delay(Duration.ofMillis(100))
					.thenEmpty(Mono.defer(() -> response.writeWith(getBody("body"))));
		}

		void exchange(ServerWebExchange exchange) {
			exchange.getResponse().getHeaders().add("foo", "bar");
		}

		Mono<Void> exchangeMonoVoid(ServerWebExchange exchange) {
			return Mono.delay(Duration.ofMillis(100))
					.thenEmpty(Mono.defer(() -> exchange.getResponse().writeWith(getBody("body"))));
		}

		@Nullable
		String notModified(ServerWebExchange exchange) {
			if (exchange.checkNotModified(Instant.ofEpochMilli(1000 * 1000))) {
				return null;
			}
			return "body";
		}

		private Flux<DataBuffer> getBody(String body) {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
		}
	}

}
