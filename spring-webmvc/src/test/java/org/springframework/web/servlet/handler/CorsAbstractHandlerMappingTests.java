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

import java.util.Collections;
import java.util.stream.Stream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Named;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.PreFlightRequestHandler;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.mockito.Mockito.mock;

/**
 * Tests for CORS-related handling in {@link AbstractHandlerMapping}.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
class CorsAbstractHandlerMappingTests {

	@SuppressWarnings("unused")
	private static Stream<Named<TestHandlerMapping>> pathPatternsArguments() {
		return Stream.of(
				named("TestHandlerMapping with PathPatternParser", new TestHandlerMapping(new PathPatternParser())),
				named("TestHandlerMapping without PathPatternParser", new TestHandlerMapping())
			);
	}


	@PathPatternsParameterizedTest
	void actualRequestWithoutCorsConfig(TestHandlerMapping mapping) throws Exception {

		HandlerExecutionChain chain = mapping.getHandler(getCorsRequest("/foo"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(SimpleHandler.class);
		assertThat(mapping.hasSavedCorsConfig()).isFalse();
	}

	@PathPatternsParameterizedTest
	void preflightRequestWithoutCorsConfig(TestHandlerMapping mapping) throws Exception {

		HandlerExecutionChain chain = mapping.getHandler(getPreFlightRequest("/foo"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(PreFlightRequestHandler.class);
		assertThat(mapping.hasSavedCorsConfig()).isFalse();
	}

	@PathPatternsParameterizedTest
	void actualRequestWithCorsConfigProvider(TestHandlerMapping mapping) throws Exception {

		HandlerExecutionChain chain = mapping.getHandler(getCorsRequest("/cors"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(CorsAwareHandler.class);
		assertThat(mapping.getRequiredCorsConfig().getAllowedOrigins()).containsExactly("*");
	}

	@PathPatternsParameterizedTest // see gh-23843
	void actualRequestWithCorsConfigProviderForHandlerChain(TestHandlerMapping mapping) throws Exception {

		HandlerExecutionChain chain = mapping.getHandler(getCorsRequest("/chain"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(CorsAwareHandler.class);
		assertThat(mapping.getRequiredCorsConfig().getAllowedOrigins()).containsExactly("*");
	}

	@PathPatternsParameterizedTest
	void preflightRequestWithCorsConfigProvider(TestHandlerMapping mapping) throws Exception {

		HandlerExecutionChain chain = mapping.getHandler(getPreFlightRequest("/cors"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(PreFlightRequestHandler.class);
		assertThat(mapping.getRequiredCorsConfig().getAllowedOrigins()).containsExactly("*");
	}

	@PathPatternsParameterizedTest
	void actualRequestWithMappedCorsConfig(TestHandlerMapping mapping) throws Exception {

		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		mapping.setCorsConfigurations(Collections.singletonMap("/foo", config));

		HandlerExecutionChain chain = mapping.getHandler(getCorsRequest("/foo"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(SimpleHandler.class);
		assertThat(mapping.getRequiredCorsConfig().getAllowedOrigins()).containsExactly("*");
	}

	@PathPatternsParameterizedTest
	void actualRequestWithMappedPatternCorsConfiguration(TestHandlerMapping mapping) throws Exception {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOriginPattern("http://*.domain2.com");
		mapping.setCorsConfigurations(Collections.singletonMap("/foo", config));
		MockHttpServletRequest request = getCorsRequest("/foo");
		HandlerExecutionChain chain = mapping.getHandler(request);

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(SimpleHandler.class);
		assertThat(mapping.getRequiredCorsConfig().getAllowedOriginPatterns()).containsExactly("http://*.domain2.com");
	}

	@PathPatternsParameterizedTest
	void preflightRequestWithMappedCorsConfig(TestHandlerMapping mapping) throws Exception {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		mapping.setCorsConfigurations(Collections.singletonMap("/foo", config));

		HandlerExecutionChain chain = mapping.getHandler(getPreFlightRequest("/foo"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(PreFlightRequestHandler.class);
		assertThat(mapping.getRequiredCorsConfig().getAllowedOrigins()).containsExactly("*");
	}

	@PathPatternsParameterizedTest
	void actualRequestWithCorsConfigSource(TestHandlerMapping mapping) throws Exception {

		mapping.setCorsConfigurationSource(new CustomCorsConfigurationSource());
		HandlerExecutionChain chain = mapping.getHandler(getCorsRequest("/foo"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(SimpleHandler.class);

		CorsConfiguration config = mapping.getRequiredCorsConfig();
		assertThat(config).isNotNull();
		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
		assertThat(config.getAllowCredentials()).isTrue();
	}

	@PathPatternsParameterizedTest
	void preflightRequestWithCorsConfigSource(TestHandlerMapping mapping) throws Exception {

		mapping.setCorsConfigurationSource(new CustomCorsConfigurationSource());
		HandlerExecutionChain chain = mapping.getHandler(getPreFlightRequest("/foo"));

		assertThat(chain).isNotNull();
		assertThat(chain.getHandler()).isNotNull();
		assertThat(chain.getHandler()).isInstanceOf(PreFlightRequestHandler.class);

		CorsConfiguration config = mapping.getRequiredCorsConfig();
		assertThat(config).isNotNull();
		assertThat(config.getAllowedOrigins()).isNull();
		assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
		assertThat(config.getAllowCredentials()).isTrue();
	}


	private MockHttpServletRequest getCorsRequest(String requestURI) {
		return createCorsRequest(HttpMethod.GET, requestURI);
	}

	private MockHttpServletRequest getPreFlightRequest(String requestURI) {
		return createCorsRequest(HttpMethod.OPTIONS, requestURI);
	}

	private MockHttpServletRequest createCorsRequest(HttpMethod method, String requestURI) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.name(), requestURI);
		request.setRemoteHost("domain1.com");
		request.addHeader(HttpHeaders.ORIGIN, "https://domain2.com");
		request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		return request;
	}


	private static class TestHandlerMapping extends AbstractHandlerMapping {

		@Nullable
		private CorsConfiguration savedCorsConfig;


		TestHandlerMapping() {
			this(null);
		}

		TestHandlerMapping(@Nullable PathPatternParser parser) {
			setInterceptors(mock(HandlerInterceptor.class));
			setApplicationContext(new StaticWebApplicationContext());
			if (parser != null) {
				setPatternParser(parser);
			}
		}

		boolean hasSavedCorsConfig() {
			return this.savedCorsConfig != null;
		}

		CorsConfiguration getRequiredCorsConfig() {
			assertThat(this.savedCorsConfig).isNotNull();
			return this.savedCorsConfig;
		}

		@Override
		protected Object getHandlerInternal(HttpServletRequest request) {
			String lookupPath = initLookupPath(request);
			if (lookupPath.equals("/cors")) {
				return new CorsAwareHandler();
			}
			else if (lookupPath.equals("/chain")) {
				return new HandlerExecutionChain(new CorsAwareHandler());
			}
			return new SimpleHandler();
		}

		@Override
		protected String initLookupPath(HttpServletRequest request) {
			// At runtime this is done by the DispatcherServlet
			if (getPatternParser() != null) {
				RequestPath requestPath = ServletRequestPathUtils.parseAndCache(request);
				return requestPath.pathWithinApplication().value();
			}
			return super.initLookupPath(request);
		}

		@Override
		protected HandlerExecutionChain getCorsHandlerExecutionChain(
				HttpServletRequest request, HandlerExecutionChain chain, @Nullable CorsConfiguration config) {

			this.savedCorsConfig = config;
			return super.getCorsHandlerExecutionChain(request, chain, config);
		}
	}

	private static class SimpleHandler extends WebContentGenerator implements HttpRequestHandler {

		SimpleHandler() {
			super(METHOD_GET);
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
			response.setStatus(HttpStatus.OK.value());
		}

	}

	private static class CorsAwareHandler extends SimpleHandler implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("*");
			return config;
		}

	}

	private static class CustomCorsConfigurationSource implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOriginPattern("*");
			config.setAllowCredentials(true);
			return config;
		}
	}

}
