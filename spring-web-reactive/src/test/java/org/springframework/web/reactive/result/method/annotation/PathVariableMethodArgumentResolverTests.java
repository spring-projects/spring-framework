/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.BindingContext;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link PathVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class PathVariableMethodArgumentResolverTests {

	private PathVariableMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MethodParameter paramNamedString;

	private MethodParameter paramString;

	private MethodParameter paramNotRequired;

	private MethodParameter paramOptional;


	@Before
	public void setUp() throws Exception {
		this.resolver = new PathVariableMethodArgumentResolver(null);

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, "/");
		WebSessionManager sessionManager = new MockWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		paramNamedString = new SynthesizingMethodParameter(method, 0);
		paramString = new SynthesizingMethodParameter(method, 1);
		paramNotRequired = new SynthesizingMethodParameter(method, 2);
		paramOptional = new SynthesizingMethodParameter(method, 3);
	}


	@Test
	public void supportsParameter() {
		assertTrue(this.resolver.supportsParameter(this.paramNamedString));
		assertFalse(this.resolver.supportsParameter(this.paramString));
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNamedString, bindingContext, this.exchange);
		Object result = mono.block();
		assertEquals("value", result);
	}

	@Test
	public void resolveArgumentNotRequired() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNotRequired, bindingContext, this.exchange);
		Object result = mono.block();
		assertEquals("value", result);
	}

	@Test
	public void resolveArgumentWrappedAsOptional() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultFormattingConversionService());
		BindingContext bindingContext = new BindingContext(initializer);

		Mono<Object> mono = this.resolver.resolveArgument(this.paramOptional, bindingContext, this.exchange);
		Object result = mono.block();
		assertEquals(Optional.of("value"), result);
	}

	@Test
	public void handleMissingValue() throws Exception {
		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNamedString, bindingContext, this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectError(ServerErrorException.class)
				.verify();
	}

	@Test
	public void nullIfNotRequired() throws Exception {
		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramNotRequired, bindingContext, this.exchange);
		StepVerifier.create(mono)
				.expectNextCount(0)
				.expectComplete()
				.verify();
	}

	@Test
	public void wrapEmptyWithOptional() throws Exception {
		BindingContext bindingContext = new BindingContext();
		Mono<Object> mono = this.resolver.resolveArgument(this.paramOptional, bindingContext, this.exchange);

		StepVerifier.create(mono)
				.consumeNextWith(value -> {
					assertTrue(value instanceof Optional);
					assertFalse(((Optional) value).isPresent());
				})
				.expectComplete()
				.verify();
	}


	@SuppressWarnings("unused")
	public void handle(@PathVariable(value = "name") String param1, String param2,
			@PathVariable(name = "name", required = false) String param3,
			@PathVariable("name") Optional<String> param4) {
	}

}
