/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.accept.ApiVersionHolder;
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

	private MockServerWebExchange exchange;

	private MethodParameter param;
	private MethodParameter nullableParam;
	private MethodParameter optionalParam;
	private MethodParameter intParam;


	@BeforeEach
	void setUp() throws Exception {

		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"));
		this.exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, ApiVersionHolder.EMPTY);

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
		this.exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, ApiVersionHolder.fromVersion(version));

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
		this.exchange.getAttributes().put(HandlerMapping.API_VERSION_ATTRIBUTE, ApiVersionHolder.fromVersion(version));

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
