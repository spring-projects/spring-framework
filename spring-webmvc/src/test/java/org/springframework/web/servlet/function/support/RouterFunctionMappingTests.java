/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.function.support;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RouterFunctionMapping}.
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
class RouterFunctionMappingTests {

	private final List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	@Test
	void normal() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = request -> Optional.of(handlerFunction);

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageConverters(this.messageConverters);

		MockHttpServletRequest request = createTestRequest("/match");
		HandlerExecutionChain result = mapping.getHandler(request);

		assertThat(result).isNotNull();
		assertThat(result.getHandler()).isSameAs(handlerFunction);
	}

	@Test
	void noMatch() throws Exception {
		RouterFunction<ServerResponse> routerFunction = request -> Optional.empty();

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageConverters(this.messageConverters);

		MockHttpServletRequest request = createTestRequest("/match");
		HandlerExecutionChain result = mapping.getHandler(request);

		assertThat(result).isNull();
	}

	@Test
	void empty() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.refresh();

		RouterFunctionMapping mapping = new RouterFunctionMapping();
		mapping.setMessageConverters(this.messageConverters);
		mapping.setApplicationContext(context);
		mapping.afterPropertiesSet();

		MockHttpServletRequest request = createTestRequest("/match");
		HandlerExecutionChain result = mapping.getHandler(request);

		assertThat(result).isNull();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void detectHandlerFunctionsInAncestorContexts(boolean detect) throws Exception {
		HandlerFunction<ServerResponse> function1 = request -> ServerResponse.ok().build();
		HandlerFunction<ServerResponse> function2 = request -> ServerResponse.ok().build();
		HandlerFunction<ServerResponse> function3 = request -> ServerResponse.ok().build();

		AnnotationConfigApplicationContext context1 = new AnnotationConfigApplicationContext();
		context1.registerBean("fn1", RouterFunction.class, () -> RouterFunctions.route().GET("/fn1", function1).build());
		context1.refresh();

		AnnotationConfigApplicationContext context2 = new AnnotationConfigApplicationContext();
		context2.registerBean("fn2", RouterFunction.class, () -> RouterFunctions.route().GET("/fn2", function2).build());
		context2.setParent(context1);
		context2.refresh();

		AnnotationConfigApplicationContext context3 = new AnnotationConfigApplicationContext();
		context3.registerBean("fn3", RouterFunction.class, () -> RouterFunctions.route().GET("/fn3", function3).build());
		context3.setParent(context2);
		context3.refresh();

		RouterFunctionMapping mapping = new RouterFunctionMapping();
		mapping.setDetectHandlerFunctionsInAncestorContexts(detect);
		mapping.setMessageConverters(this.messageConverters);
		mapping.setApplicationContext(context3);
		mapping.afterPropertiesSet();

		HandlerExecutionChain chain1 = mapping.getHandler(createTestRequest("/fn1"));
		HandlerExecutionChain chain2 = mapping.getHandler(createTestRequest("/fn2"));
		if (detect) {
			assertThat(chain1).isNotNull().extracting(HandlerExecutionChain::getHandler).isSameAs(function1);
			assertThat(chain2).isNotNull().extracting(HandlerExecutionChain::getHandler).isSameAs(function2);
		}
		else {
			assertThat(chain1).isNull();
			assertThat(chain2).isNull();
		}

		HandlerExecutionChain chain3 = mapping.getHandler(createTestRequest("/fn3"));
		assertThat(chain3).isNotNull().extracting(HandlerExecutionChain::getHandler).isSameAs(function3);

	}

	@Test
	void changeParser() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/foo", handlerFunction)
				.POST("/bar", handlerFunction)
				.build();

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageConverters(this.messageConverters);
		PathPatternParser patternParser = new PathPatternParser();
		patternParser.setCaseSensitive(false);
		mapping.setPatternParser(patternParser);
		mapping.afterPropertiesSet();

		MockHttpServletRequest request = createTestRequest("/FOO");
		HandlerExecutionChain result = mapping.getHandler(request);

		assertThat(result).isNotNull();
		assertThat(result.getHandler()).isSameAs(handlerFunction);
	}

	@Test
	void mappedRequestShouldHoldAttributes() throws Exception {
		HandlerFunction<ServerResponse> handlerFunction = request -> ServerResponse.ok().build();
		RouterFunction<ServerResponse> routerFunction = RouterFunctions.route()
				.GET("/match", handlerFunction)
				.build();

		RouterFunctionMapping mapping = new RouterFunctionMapping(routerFunction);
		mapping.setMessageConverters(this.messageConverters);

		MockHttpServletRequest request = createTestRequest("/match");
		HandlerExecutionChain result = mapping.getHandler(request);

		assertThat(result).isNotNull();
		assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).isEqualTo("/match");
		assertThat(ServerHttpObservationFilter.findObservationContext(request))
				.hasValueSatisfying(context -> assertThat(context.getPathPattern()).isEqualTo("/match"));
		assertThat(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE)).isEqualTo(handlerFunction);
	}

	private MockHttpServletRequest createTestRequest(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		request.setAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE,
				new ServerRequestObservationContext(request, new MockHttpServletResponse()));
		ServletRequestPathUtils.parseAndCache(request);
		return request;
	}

}
