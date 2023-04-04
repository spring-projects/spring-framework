/*
 * Copyright 2002-2023 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link PathVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class PathVariableMethodArgumentResolverTests {

	private PathVariableMethodArgumentResolver resolver;

	private final MockServerWebExchange exchange= MockServerWebExchange.from(MockServerHttpRequest.get("/"));

	private MethodParameter paramNamedString;
	private MethodParameter paramString;
	private MethodParameter paramNotRequired;
	private MethodParameter paramOptional;
	private MethodParameter paramMono;


	@BeforeEach
	public void setup() throws Exception {
		this.resolver = new PathVariableMethodArgumentResolver(null, ReactiveAdapterRegistry.getSharedInstance());

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		paramNamedString = new SynthesizingMethodParameter(method, 0);
		paramString = new SynthesizingMethodParameter(method, 1);
		paramNotRequired = new SynthesizingMethodParameter(method, 2);
		paramOptional = new SynthesizingMethodParameter(method, 3);
		paramMono = new SynthesizingMethodParameter(method, 4);
	}


	@Test
	public void supportsParameter() {
		assertThat(this.resolver.supportsParameter(this.paramNamedString)).isTrue();
		assertThat(this.resolver.supportsParameter(this.paramString)).isFalse();
		assertThatIllegalStateException().isThrownBy(() ->
				this.resolver.supportsParameter(this.paramMono))
			.withMessageStartingWith("PathVariableMethodArgumentResolver does not support reactive type wrapper");
	}

	@Test
	public void resolveArgument() {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNamedString, bindingContext, this.exchange);
		Object result = mono.block();
		assertThat(result).isEqualTo("value");
	}

	@Test
	public void resolveArgumentNotRequired() {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNotRequired, bindingContext, this.exchange);
		Object result = mono.block();
		assertThat(result).isEqualTo("value");
	}

	@Test
	public void resolveArgumentWrappedAsOptional() {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramOptional, bindingContext, this.exchange);
		Object result = mono.block();
		assertThat(result).isEqualTo(Optional.of("value"));
	}

	@Test
	public void handleMissingValue() {
		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNamedString, bindingContext, this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerErrorException.class)
				.verify();
	}

	@Test
	public void nullIfNotRequired() {
		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNotRequired, bindingContext, this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void wrapEmptyWithOptional() {
		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramOptional, bindingContext, this.exchange);

		StepVerifier.create(mono)
				.consumeNextWith(value -> {
					boolean condition = value instanceof Optional;
					assertThat(condition).isTrue();
					assertThat(((Optional<?>) value)).isNotPresent();
				})
				.expectComplete()
				.verify();
	}


	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	public void handle(
			@PathVariable(value = "name") String param1,
			String param2,
			@PathVariable(name = "name", required = false) String param3,
			@PathVariable("name") Optional<String> param4,
			@PathVariable Mono<String> param5) {
	}

}
