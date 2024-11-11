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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.testfixture.server.MockServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AbstractHandlerMethodMapping}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HandlerMethodMappingTests {

	private MyHandlerMethodMapping mapping;

	private MyHandler handler;

	private Method method1;

	private Method method2;


	@BeforeEach
	void setup() throws Exception {
		this.mapping = new MyHandlerMethodMapping();
		this.handler = new MyHandler();
		this.method1 = handler.getClass().getMethod("handlerMethod1");
		this.method2 = handler.getClass().getMethod("handlerMethod2");
	}


	@Test
	void registerDuplicates() {
		this.mapping.registerMapping("foo", this.handler, this.method1);
		assertThatIllegalStateException().isThrownBy(() ->
				this.mapping.registerMapping("foo", this.handler, this.method2));
	}

	@Test
	void directMatch() {
		this.mapping.registerMapping("/foo", this.handler, this.method1);
		this.mapping.registerMapping("/fo*", this.handler, this.method2);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));
		Mono<Object> result = this.mapping.getHandler(exchange);

		assertThat(((HandlerMethod) result.block()).getMethod()).isEqualTo(this.method1);
		assertThat(this.mapping.getMatches()).containsExactly("/foo");
	}

	@Test
	void patternMatch() {
		this.mapping.registerMapping("/fo*", this.handler, this.method1);
		this.mapping.registerMapping("/f*", this.handler, this.method2);

		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));
		Mono<Object> result = this.mapping.getHandler(exchange);
		assertThat(((HandlerMethod) result.block()).getMethod()).isEqualTo(this.method1);
	}

	@Test
	void ambiguousMatch() {
		this.mapping.registerMapping("/f?o", this.handler, this.method1);
		this.mapping.registerMapping("/fo?", this.handler, this.method2);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/foo"));
		Mono<Object> result = this.mapping.getHandler(exchange);

		StepVerifier.create(result).expectError(IllegalStateException.class).verify();
	}

	@Test // gh-26490
	public void ambiguousMatchOnPreFlightRequestWithoutCorsConfig() {
		this.mapping.registerMapping("/f?o", this.handler, this.method1);
		this.mapping.registerMapping("/fo?", this.handler, this.method2);

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.options("https://example.org/foo")
						.header(HttpHeaders.ORIGIN, "https://domain.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));

		this.mapping.getHandler(exchange).block();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test // gh-26490
	public void ambiguousMatchOnPreFlightRequestWithCorsConfig() throws Exception {
		this.mapping.registerMapping("/f?o", this.handler, this.method1);
		this.mapping.registerMapping("/fo?", this.handler, this.handler.getClass().getMethod("corsHandlerMethod"));

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.options("https://example.org/foo")
						.header(HttpHeaders.ORIGIN, "https://domain.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));

		this.mapping.getHandler(exchange).block();

		MockServerHttpResponse response = exchange.getResponse();
		assertThat(response.getStatusCode()).isNull();
		assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://domain.com");
		assertThat(response.getHeaders().getAccessControlAllowMethods()).containsExactly(HttpMethod.GET);
	}

	@Test
	void registerMapping() {
		String key1 = "/foo";
		String key2 = "/foo*";
		this.mapping.registerMapping(key1, this.handler, this.method1);
		this.mapping.registerMapping(key2, this.handler, this.method2);

		assertThat(this.mapping.getMappingRegistry().getRegistrations()).containsKeys(key1, key2);
	}

	@Test
	void registerMappingWithSameMethodAndTwoHandlerInstances() {
		String key1 = "foo";
		String key2 = "bar";
		MyHandler handler1 = new MyHandler();
		MyHandler handler2 = new MyHandler();
		this.mapping.registerMapping(key1, handler1, this.method1);
		this.mapping.registerMapping(key2, handler2, this.method1);

		assertThat(this.mapping.getMappingRegistry().getRegistrations()).containsKeys(key1, key2);
	}

	@Test
	void unregisterMapping() {
		String key = "foo";
		this.mapping.registerMapping(key, this.handler, this.method1);
		Mono<Object> result = this.mapping.getHandler(MockServerWebExchange.from(MockServerHttpRequest.get(key)));

		assertThat(result.block()).isNotNull();

		this.mapping.unregisterMapping(key);
		result = this.mapping.getHandler(MockServerWebExchange.from(MockServerHttpRequest.get(key)));

		assertThat(result.block()).isNull();
		assertThat(this.mapping.getMappingRegistry().getRegistrations().keySet()).doesNotContain(key);
	}


	private static class MyHandlerMethodMapping extends AbstractHandlerMethodMapping<String> {

		private PathPatternParser parser = new PathPatternParser();

		private final List<String> matches = new ArrayList<>();


		public List<String> getMatches() {
			return this.matches;
		}

		@Override
		protected boolean isHandler(Class<?> beanType) {
			return true;
		}

		@Override
		protected String getMappingForMethod(Method method, Class<?> handlerType) {
			String methodName = method.getName();
			return methodName.startsWith("handler") ? methodName : null;
		}

		@Override
		protected Set<String> getDirectPaths(String mapping) {
			return (parser.parse(mapping).hasPatternSyntax() ?
					Collections.emptySet() : Collections.singleton(mapping));
		}

		@Override
		protected CorsConfiguration initCorsConfiguration(Object handler, Method method, String mapping) {
			CrossOrigin crossOrigin = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);
			if (crossOrigin != null) {
				CorsConfiguration corsConfig = new CorsConfiguration();
				corsConfig.setAllowedOrigins(Collections.singletonList("https://domain.com"));
				return corsConfig;
			}
			return null;
		}

		@Override
		protected String getMatchingMapping(String pattern, ServerWebExchange exchange) {
			PathContainer lookupPath = exchange.getRequest().getPath().pathWithinApplication();
			PathPattern parsedPattern = this.parser.parse(pattern);
			String match = parsedPattern.matches(lookupPath) ? pattern : null;
			if (match != null) {
				matches.add(match);
			}
			return match;
		}

		@Override
		protected Comparator<String> getMappingComparator(ServerWebExchange exchange) {
			return (o1, o2) -> PathPattern.SPECIFICITY_COMPARATOR.compare(parser.parse(o1), parser.parse(o2));
		}
	}

	@Controller
	private static class MyHandler {

		@RequestMapping
		@SuppressWarnings("unused")
		public void handlerMethod1() {
		}

		@RequestMapping
		@SuppressWarnings("unused")
		public void handlerMethod2() {
		}

		@RequestMapping
		@CrossOrigin(originPatterns = "*")
		public void corsHandlerMethod() {
		}
	}

}
