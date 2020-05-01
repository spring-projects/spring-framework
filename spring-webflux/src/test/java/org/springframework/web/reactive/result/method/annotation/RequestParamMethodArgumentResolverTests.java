/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestParam;

/**
 * Unit tests for {@link RequestParamMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamMethodArgumentResolverTests {

	private RequestParamMethodArgumentResolver resolver;

	private BindingContext bindContext;

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@BeforeEach
	public void setup() throws Exception {

		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestParamMethodArgumentResolver(null, adapterRegistry, true);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		this.bindContext = new BindingContext(initializer);
	}


	@Test
	public void supportsParameter() {

		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		assertThat(this.resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam()).arg(String.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().notRequired()).arg(String.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

	}

	@Test
	public void doesNotSupportParameterWithDefaultResolutionTurnedOff() {
		ReactiveAdapterRegistry adapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		this.resolver = new RequestParamMethodArgumentResolver(null, adapterRegistry, false);

		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertThat(this.resolver.supportsParameter(param)).isFalse();
	}

	@Test
	public void doesNotSupportReactiveWrapper() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.testMethod.annot(requestParam()).arg(Mono.class, String.class)))
			.withMessageStartingWith("RequestParamMethodArgumentResolver does not support reactive type wrapper");
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.testMethod.annotNotPresent(RequestParam.class).arg(Mono.class, String.class)))
			.withMessageStartingWith("RequestParamMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void resolveWithQueryString() {
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=foo"));
		assertThat(resolve(param, exchange)).isEqualTo("foo");
	}

	@Test
	public void resolveStringArray() {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?name=foo&name=bar").build();
		Object result = resolve(param, MockServerWebExchange.from(request));
		boolean condition = result instanceof String[];
		assertThat(condition).isTrue();
		assertThat((String[]) result).isEqualTo(new String[] {"foo", "bar"});
	}

	@Test
	public void resolveDefaultValue() {
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		assertThat(resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/")))).isEqualTo("bar");
	}

	@Test // SPR-17050
	public void resolveAndConvertNullValue() {
		MethodParameter param = this.testMethod
				.annot(requestParam().notRequired())
				.arg(Integer.class);
		assertThat(resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/?nullParam=")))).isNull();
	}

	@Test
	public void missingRequestParam() {

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(String[].class);
		Mono<Object> mono = this.resolver.resolveArgument(param, this.bindContext, exchange);

		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerWebInputException.class)
				.verify();
	}

	@Test
	public void resolveSimpleTypeParam() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?stringNotAnnot=plainValue").build();
		ServerWebExchange exchange = MockServerWebExchange.from(request);
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		Object result = resolve(param, exchange);
		assertThat(result).isEqualTo("plainValue");
	}

	@Test  // SPR-8561
	public void resolveSimpleTypeParamToNull() {
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		assertThat(resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/")))).isNull();
	}

	@Test  // SPR-10180
	public void resolveEmptyValueToDefault() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name="));
		MethodParameter param = this.testMethod.annot(requestParam().notRequired("bar")).arg(String.class);
		Object result = resolve(param, exchange);
		assertThat(result).isEqualTo("bar");
	}

	@Test
	public void resolveEmptyValueWithoutDefault() {
		MethodParameter param = this.testMethod.annotNotPresent(RequestParam.class).arg(String.class);
		MockServerHttpRequest request = MockServerHttpRequest.get("/path?stringNotAnnot=").build();
		assertThat(resolve(param, MockServerWebExchange.from(request))).isEqualTo("");
	}

	@Test
	public void resolveEmptyValueRequiredWithoutDefault() {
		MethodParameter param = this.testMethod.annot(requestParam()).arg(String.class);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name="));
		assertThat(resolve(param, exchange)).isEqualTo("");
	}

	@Test
	public void resolveOptionalParamValue() {
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		MethodParameter param = this.testMethod.arg(forClassWithGenerics(Optional.class, Integer.class));
		Object result = resolve(param, exchange);
		assertThat(result).isEqualTo(Optional.empty());

		exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?name=123"));
		result = resolve(param, exchange);

		assertThat(result.getClass()).isEqualTo(Optional.class);
		Optional<?> value = (Optional<?>) result;
		assertThat(value.isPresent()).isTrue();
		assertThat(value.get()).isEqualTo(123);
	}


	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, this.bindContext, exchange).block(Duration.ZERO);
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	public void handle(
			@RequestParam(name = "name", defaultValue = "bar") String param1,
			@RequestParam("name") String[] param2,
			@RequestParam("name") Map<?, ?> param3,
			@RequestParam Map<?, ?> param4,
			String stringNotAnnot,
			Mono<String> monoStringNotAnnot,
			@RequestParam("name") String paramRequired,
			@RequestParam(name = "name", required = false) String paramNotRequired,
			@RequestParam("name") Optional<Integer> paramOptional,
			@RequestParam Mono<String> paramMono,
			@RequestParam(required = false) Integer nullParam) {
	}

}
