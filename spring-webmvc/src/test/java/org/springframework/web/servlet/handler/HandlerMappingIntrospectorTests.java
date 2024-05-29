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

package org.springframework.web.servlet.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;

/**
 * Tests for {@link HandlerMappingIntrospector}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
class HandlerMappingIntrospectorTests {

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

	@Test
	void useParsedPatternsOnly() {
		GenericWebApplicationContext context = new GenericWebApplicationContext();
		context.registerBean("A", SimpleUrlHandlerMapping.class);
		context.registerBean("B", SimpleUrlHandlerMapping.class);
		context.registerBean("C", SimpleUrlHandlerMapping.class);
		context.refresh();

		assertThat(initIntrospector(context).allHandlerMappingsUsePathPatternParser()).isTrue();

		context = new GenericWebApplicationContext();
		context.registerBean("A", SimpleUrlHandlerMapping.class);
		context.registerBean("B", SimpleUrlHandlerMapping.class);
		context.registerBean("C", SimpleUrlHandlerMapping.class, () -> {
			SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
			mapping.setPatternParser(null);
			return mapping;
		});
		context.refresh();

		assertThat(initIntrospector(context).allHandlerMappingsUsePathPatternParser()).isFalse();
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
		cxt.registerBean("mapping", HandlerMapping.class, () -> request -> new HandlerExecutionChain(new Object()));
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

	@Test
	void handlePreFlight() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/path");
		request.addHeader("Origin", "http://localhost:9000");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		MockHttpServletResponse response = new MockHttpServletResponse();

		initIntrospector(context).handlePreFlight(request, response);

		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:9000");
		assertThat(response.getHeaders(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).containsExactly("POST");
	}

	@Test
	void handlePreFlightWithNoHandlerFoundException() {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/unknownPath");
		request.addHeader("Origin", "http://localhost:9000");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatThrownBy(() -> initIntrospector(context).handlePreFlight(request, response))
				.isInstanceOf(NoHandlerFoundException.class);

		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"/test", "/resource/1234****"}) // gh-31937
	void cacheFilter(String uri) throws Exception {
		CorsConfiguration corsConfig = new CorsConfiguration();
		TestMatchableHandlerMapping mapping = new TestMatchableHandlerMapping();
		mapping.registerHandler("/*", new TestHandler(corsConfig));

		HandlerMappingIntrospector introspector = initIntrospector(mapping);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
		MockHttpServletResponse response = new MockHttpServletResponse();

		MockFilterChain filterChain = new MockFilterChain(
				new TestServlet(), introspector.createCacheFilter(), new AuthFilter(introspector, corsConfig));

		filterChain.doFilter(request, response);

		assertThat(response.getContentAsString()).isEqualTo("Success");
		assertThat(mapping.getInvocationCount()).isEqualTo(1);
		assertThat(mapping.getMatchCount()).isEqualTo(1);
	}

	@Test
	void cacheFilterWithNestedDispatch() throws Exception {
		CorsConfiguration corsConfig1 = new CorsConfiguration();
		CorsConfiguration corsConfig2 = new CorsConfiguration();

		TestMatchableHandlerMapping mapping1 = new TestMatchableHandlerMapping();
		TestMatchableHandlerMapping mapping2 = new TestMatchableHandlerMapping();

		mapping1.registerHandler("/1", new TestHandler(corsConfig1));
		mapping2.registerHandler("/2", new TestHandler(corsConfig2));

		HandlerMappingIntrospector introspector = initIntrospector(mapping1, mapping2);

		MockFilterChain filterChain = new MockFilterChain(
				new TestServlet(),
				introspector.createCacheFilter(),
				new AuthFilter(introspector, corsConfig1),
				(req, res, chain) -> chain.doFilter(new MockHttpServletRequest("GET", "/2"), res),
				introspector.createCacheFilter(),
				new AuthFilter(introspector, corsConfig2));

		MockHttpServletResponse response = new MockHttpServletResponse();
		filterChain.doFilter(new MockHttpServletRequest("GET", "/1"), response);

		assertThat(response.getContentAsString()).isEqualTo("Success");
		assertThat(mapping1.getInvocationCount()).isEqualTo(2);
		assertThat(mapping2.getInvocationCount()).isEqualTo(1);
		assertThat(mapping1.getMatchCount()).isEqualTo(1);
		assertThat(mapping2.getMatchCount()).isEqualTo(1);
	}

	private HandlerMappingIntrospector initIntrospector(TestMatchableHandlerMapping... mappings) {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		int index = 0;
		for (TestMatchableHandlerMapping mapping : mappings) {
			context.registerBean("mapping" + index++, TestMatchableHandlerMapping.class, () -> mapping);
		}
		context.refresh();
		return initIntrospector(context);
	}

	private static HandlerMappingIntrospector initIntrospector(WebApplicationContext context) {
		HandlerMappingIntrospector introspector = new HandlerMappingIntrospector();
		introspector.setApplicationContext(context);
		introspector.afterPropertiesSet();
		return introspector;
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


	private static class TestMatchableHandlerMapping extends SimpleUrlHandlerMapping {

		private int invocationCount;

		private int matchCount;

		public int getInvocationCount() {
			return this.invocationCount;
		}

		public int getMatchCount() {
			return this.matchCount;
		}

		@Override
		protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
			this.invocationCount++;
			Object handler = super.getHandlerInternal(request);
			if (handler != null) {
				this.matchCount++;
			}
			return handler;
		}
	}


	private static class TestHandler implements CorsConfigurationSource {

		private final CorsConfiguration corsConfig;

		private TestHandler(CorsConfiguration corsConfig) {
			this.corsConfig = corsConfig;
		}

		@Override
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			return this.corsConfig;
		}
	}


	private static class AuthFilter implements Filter {

		private final HandlerMappingIntrospector introspector;

		private final CorsConfiguration corsConfig;

		private AuthFilter(HandlerMappingIntrospector introspector, CorsConfiguration corsConfig) {
			this.introspector = introspector;
			this.corsConfig = corsConfig;
		}

		@Override
		public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
			try {
				for (int i = 0; i < 10; i++) {
					HttpServletRequest httpRequest = (HttpServletRequest) req;
					assertThat(introspector.getMatchableHandlerMapping(httpRequest)).isNotNull();
					assertThat(introspector.getCorsConfiguration(httpRequest)).isSameAs(corsConfig);
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
			chain.doFilter(req, res);
		}
	}


	@SuppressWarnings("serial")
	private static class TestServlet extends HttpServlet {

		@Override
		protected void service(HttpServletRequest req, HttpServletResponse res) {
			try {
				res.getWriter().print("Success");
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
