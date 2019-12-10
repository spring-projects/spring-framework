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
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.ResolvableMethod;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.method.MvcAnnotationPredicates.requestParam;

/**
 * Unit tests for {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestParamMapMethodArgumentResolverTests {

	private final RequestParamMapMethodArgumentResolver resolver =
			new RequestParamMapMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	public void supportsParameter() {
		MethodParameter param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		assertThat(this.resolver.supportsParameter(param)).isTrue();

		param = this.testMethod.annot(requestParam().name("name")).arg(Map.class);
		assertThat(this.resolver.supportsParameter(param)).isFalse();

		param = this.testMethod.annotNotPresent(RequestParam.class).arg(Map.class);
		assertThat(this.resolver.supportsParameter(param)).isFalse();

		assertThatIllegalStateException().isThrownBy(() ->
					this.resolver.supportsParameter(this.testMethod.annot(requestParam()).arg(Mono.class, Map.class)))
			.withMessageStartingWith("RequestParamMapMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void resolveMapArgumentWithQueryString() {
		MethodParameter param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		Object result= resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/path?foo=bar")));
		boolean condition = result instanceof Map;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo(Collections.singletonMap("foo", "bar"));
	}

	@Test
	public void resolveMultiValueMapArgument() {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?foo=bar&foo=baz"));
		Object result= resolve(param, exchange);

		boolean condition = result instanceof MultiValueMap;
		assertThat(condition).isTrue();
		assertThat(result).isEqualTo(Collections.singletonMap("foo", Arrays.asList("bar", "baz")));
	}


	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, null, exchange).block(Duration.ofMillis(0));
	}


	public void handle(
			@RequestParam Map<?, ?> param1,
			@RequestParam MultiValueMap<?, ?> param2,
			@RequestParam("name") Map<?, ?> param3,
			Map<?, ?> param4,
			@RequestParam Mono<Map<?, ?>> paramMono) {
	}

}
