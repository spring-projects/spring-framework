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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.accept.SemanticApiVersionParser.Version;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.OPTIONAL;

/**
 * Tests for {@link ApiVersionMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class ApiVersionMethodArgumentResolverTests {

	private final ApiVersionMethodArgumentResolver resolver = new ApiVersionMethodArgumentResolver();

	private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private MethodParameter param;
	private MethodParameter nullableParam;
	private MethodParameter optionalParam;
	private MethodParameter intParam;


	@BeforeEach
	void setUp() throws Exception {

		Method method = getClass().getDeclaredMethod(
				"handle", Version.class, Version.class, Optional.class, int.class);

		this.param = new MethodParameter(method, 0);
		this.nullableParam = new MethodParameter(method, 1);
		this.optionalParam = new MethodParameter(method, 2);
		this.intParam = new MethodParameter(method, 3);
	}

	@Test
	void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.param)).isTrue();
		assertThat(this.resolver.supportsParameter(this.nullableParam)).isTrue();
		assertThat(this.resolver.supportsParameter(this.optionalParam)).isTrue();
		assertThat(this.resolver.supportsParameter(this.intParam)).isFalse();
	}

	@Test
	void resolveArgument() throws Exception {
		Version version = new SemanticApiVersionParser().parseVersion("1.2");
		this.exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, version);

		Object actual = this.resolver.resolveArgumentValue(this.param, new BindingContext(), exchange);

		assertThat(actual).isNotNull();
		assertThat(actual.getClass()).isEqualTo(Version.class);
		assertThat(actual).isSameAs(version);
	}

	@Test
	void resolveNullableArgument() {
		Object actual = this.resolver.resolveArgumentValue(this.nullableParam, new BindingContext(), exchange);
		assertThat(actual).isNull();
	}

	@Test
	void resolveOptionalArgument() {
		Version version = new SemanticApiVersionParser().parseVersion("1.2");
		this.exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, version);

		Object actual = this.resolver.resolveArgumentValue(this.optionalParam, new BindingContext(), exchange);
		assertThat(actual).asInstanceOf(OPTIONAL).hasValue(version);
	}

	@Test
	void resolveOptionalArgumentWhenEmpty() {
		Object actual = this.resolver.resolveArgumentValue(this.optionalParam, new BindingContext(), exchange);
		assertThat(actual).asInstanceOf(OPTIONAL).isEmpty();
	}


	@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
	void handle(
			Version version,
			@Nullable Version nullableVersion,
			Optional<Version> optionalVersion,
			int value) {
	}

}
