/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;

/**
 * Unit tests for {@link HandlerMappingIntrospector}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class HandlerMappingIntrospectorTests {

	@Test
	void detectHandlerMappings() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("A", SimpleUrlHandlerMapping.class);
		context.registerSingleton("B", SimpleUrlHandlerMapping.class);
		context.registerSingleton("C", SimpleUrlHandlerMapping.class);
		context.refresh();

		List<?> expected = Arrays.asList(context.getBean("A"), context.getBean("B"), context.getBean("C"));
		List<HandlerMapping> actual = initIntrospector(context).getHandlerMappings();

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void detectHandlerMappingsOrdered() {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.registerBean("B", SimpleUrlHandlerMapping.class, () -> {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setOrder(2);
			return mapping;
		});
		context.registerBean("C", SimpleUrlHandlerMapping.class, () -> {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setOrder(3);
			return mapping;
		});
		context.registerBean("A", SimpleUrlHandlerMapping.class, () -> {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setOrder(1);
			return mapping;
		});
		context.refresh();

		List<?> expected = Arrays.asList(context.getBean("A"), context.getBean("B"), context.getBean("C"));
		List<HandlerMapping> actual = initIntrospector(context).getHandlerMappings();

		assertThat(actual).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void getMatchable(boolean usePathPatterns) throws Exception {

		TestPathPatternParser parser = new TestPathPatternParser();

		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.registerBean("mapping", SimpleUrlHandlerMapping.class, () -> {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			if (usePathPatterns) {
				mapping.setPatternParser(parser);
			}
			mapping.setUrlMap(Collections.singletonMap("/path/*", new Object()));
			return mapping;
		});
		context.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/path/123");
		MatchableHandlerMapping mapping = initIntrospector(context).getMatchableHandlerMapping(request);

		assertThat(mapping).isNotNull();
		assertThat(request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE)).as("Attribute changes not ignored").isNull();
		assertThat(request.getAttribute(ServletRequestPathUtils.PATH_ATTRIBUTE)).as("Parsed path not cleaned").isNull();

		assertThat(mapping.match(request, "/p*/*")).isNotNull();
		assertThat(mapping.match(request, "/b*/*")).isNull();

		if (usePathPatterns) {
			assertThat(parser.getParsedPatterns()).containsExactly("/path/*", "/p*/*", "/b*/*");
		}
	}

	@Test
	void getMatchableWhereHandlerMappingDoesNotImplementMatchableInterface() {
		StaticWebApplicationContext cxt = new StaticWebApplicationContext();
		cxt.registerSingleton("mapping", TestHandlerMapping.class);
		cxt.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest();
		assertThatIllegalStateException().isThrownBy(() -> initIntrospector(cxt).getMatchableHandlerMapping(request));
	}

	@Test // gh-26833
	void getMatchablePreservesRequestAttributes() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/path");
		request.setAttribute("name", "value");

		MatchableHandlerMapping matchable = initIntrospector(context).getMatchableHandlerMapping(request);
		assertThat(matchable).isNotNull();

		// RequestPredicates.restoreAttributes clears and re-adds attributes
		assertThat(request.getAttribute("name")).isEqualTo("value");
	}

	@Test
	void getCorsConfigurationPreFlight() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		// PRE-FLIGHT

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/path");
		request.addHeader("Origin", "http://localhost:9000");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		CorsConfiguration corsConfig = initIntrospector(context).getCorsConfiguration(request);

		assertThat(corsConfig).isNotNull();
		assertThat(corsConfig.getAllowedOrigins()).isEqualTo(Collections.singletonList("http://localhost:9000"));
		assertThat(corsConfig.getAllowedMethods()).isEqualTo(Collections.singletonList("POST"));
	}

	@Test
	void getCorsConfigurationActual() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/path");
		request.addHeader("Origin", "http://localhost:9000");
		CorsConfiguration corsConfig = initIntrospector(context).getCorsConfiguration(request);

		assertThat(corsConfig).isNotNull();
		assertThat(corsConfig.getAllowedOrigins()).isEqualTo(Collections.singletonList("http://localhost:9000"));
		assertThat(corsConfig.getAllowedMethods()).isEqualTo(Collections.singletonList("POST"));
	}

	private HandlerMappingIntrospector initIntrospector(WebApplicationContext context) {
		HandlerMappingIntrospector introspector = new HandlerMappingIntrospector();
		introspector.setApplicationContext(context);
		introspector.afterPropertiesSet();
		return introspector;
	}


	private static class TestHandlerMapping implements HandlerMapping {

		@Override
		public HandlerExecutionChain getHandler(HttpServletRequest request) {
			return new HandlerExecutionChain(new Object());
		}
	}


	@Configuration
	static class TestConfig {

		@Bean
		public RouterFunctionMapping routerFunctionMapping() {
			RouterFunctionMapping mapping = new RouterFunctionMapping();
			mapping.setOrder(1);
			return mapping;
		}

		@Bean
		public RequestMappingHandlerMapping handlerMapping() {
			RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
			mapping.setOrder(2);
			return mapping;
		}

		@Bean
		public TestController testController() {
			return new TestController();
		}

		@Bean
		public RouterFunction<?> routerFunction() {
			return RouterFunctions.route().GET("/fn-path", request -> ServerResponse.ok().build()).build();
		}
	}


	@CrossOrigin("http://localhost:9000")
	@Controller
	private static class TestController {

		@PostMapping("/path")
		void handle() {
		}
	}

	private static class TestPathPatternParser extends PathPatternParser {

		private final List<String> parsedPatterns = new ArrayList<>();


		public List<String> getParsedPatterns() {
			return this.parsedPatterns;
		}

		@Override
		public PathPattern parse(String pathPattern) throws PatternParseException {
			this.parsedPatterns.add(pathPattern);
			return super.parse(pathPattern);
		}
	}

}
