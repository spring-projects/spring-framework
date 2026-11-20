/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.testfixture.method.MvcAnnotationPredicates.requestParam;

/**
 * Tests for {@link RequestParamMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class RequestParamMapMethodArgumentResolverTests {

	private final RequestParamMapMethodArgumentResolver resolver =
			new RequestParamMapMethodArgumentResolver(ReactiveAdapterRegistry.getSharedInstance());

	private ResolvableMethod testMethod = ResolvableMethod.on(getClass()).named("handle").build();


	@Test
	void supportsParameter() {
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
	void resolveMapArgumentWithQueryString() {
		MethodParameter param = this.testMethod.annot(requestParam().name("")).arg(Map.class);
		Object result= resolve(param, MockServerWebExchange.from(MockServerHttpRequest.get("/path?foo=bar")));
		assertThat(result).isInstanceOf(Map.class);
		assertThat(result).isEqualTo(Collections.singletonMap("foo", "bar"));
	}

	@Test
	void resolveMultiValueMapArgument() {
		MethodParameter param = this.testMethod.annotPresent(RequestParam.class).arg(MultiValueMap.class);
		ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path?foo=bar&foo=baz"));
		Object result= resolve(param, exchange);

		assertThat(result).isInstanceOf(MultiValueMap.class);
		assertThat(result).isEqualTo(Collections.singletonMap("foo", Arrays.asList("bar", "baz")));
	}


	private Object resolve(MethodParameter parameter, ServerWebExchange exchange) {
		return this.resolver.resolveArgument(parameter, null, exchange).block(Duration.ZERO);
	}


	public void handle(
			@RequestParam Map<?, ?> param1,
			@RequestParam MultiValueMap<?, ?> param2,
			@RequestParam("name") Map<?, ?> param3,
			Map<?, ?> param4,
			@RequestParam Mono<Map<?, ?>> paramMono) {
	}

}
