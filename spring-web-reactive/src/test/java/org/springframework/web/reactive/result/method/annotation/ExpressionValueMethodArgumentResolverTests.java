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
import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.MockServerHttpRequest;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.MockWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link ExpressionValueMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ExpressionValueMethodArgumentResolverTests {

	private ExpressionValueMethodArgumentResolver resolver;

	private ServerWebExchange exchange;

	private MethodParameter paramSystemProperty;
	private MethodParameter paramNotSupported;


	@Before
	public void setUp() throws Exception {
		ConversionService conversionService = new GenericConversionService();
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();
		this.resolver = new ExpressionValueMethodArgumentResolver(conversionService, context.getBeanFactory());

		ServerHttpRequest request = new MockServerHttpRequest(HttpMethod.GET, new URI("/"));
		WebSessionManager sessionManager = new MockWebSessionManager();
		this.exchange = new DefaultServerWebExchange(request, new MockServerHttpResponse(), sessionManager);

		Method method = getClass().getMethod("params", int.class, String.class);
		this.paramSystemProperty = new MethodParameter(method, 0);
		this.paramNotSupported = new MethodParameter(method, 1);
	}


	@Test
	public void supportsParameter() throws Exception {
		assertTrue(this.resolver.supportsParameter(this.paramSystemProperty));
		assertFalse(this.resolver.supportsParameter(this.paramNotSupported));
	}

	@Test
	public void resolveSystemProperty() throws Exception {
		System.setProperty("systemProperty", "22");
		try {
			Mono<Object> mono = this.resolver.resolveArgument(this.paramSystemProperty, null, this.exchange);
			Object value = mono.block();
			assertEquals(22, value);
		}
		finally {
			System.clearProperty("systemProperty");
		}

	}

	// TODO: test with expression for ServerWebExchange


	@SuppressWarnings("unused")
	public void params(@Value("#{systemProperties.systemProperty}") int param1,
					   String notSupported) {
	}

}