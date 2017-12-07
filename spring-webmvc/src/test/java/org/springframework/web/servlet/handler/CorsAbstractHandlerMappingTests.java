/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.handler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.WebContentGenerator;

/**
 * Unit tests for CORS-related handling in {@link AbstractHandlerMapping}.
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CorsAbstractHandlerMappingTests {

	private MockHttpServletRequest request;

	private AbstractHandlerMapping handlerMapping;


	@Before
	public void setup() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		this.handlerMapping = new TestHandlerMapping();
		this.handlerMapping.setApplicationContext(context);
		this.request = new MockHttpServletRequest();
		this.request.setRemoteHost("domain1.com");
	}

	@Test
	public void actualRequestWithoutCorsConfigurationProvider() throws Exception {
		this.request.setMethod(RequestMethod.GET.name());
		this.request.setRequestURI("/foo");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HandlerExecutionChain chain = handlerMapping.getHandler(this.request);
		assertNotNull(chain);
		assertTrue(chain.getHandler() instanceof SimpleHandler);
	}

	@Test
	public void preflightRequestWithoutCorsConfigurationProvider() throws Exception {
		this.request.setMethod(RequestMethod.OPTIONS.name());
		this.request.setRequestURI("/foo");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HandlerExecutionChain chain = handlerMapping.getHandler(this.request);
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
		assertTrue(chain.getHandler().getClass().getSimpleName().equals("PreFlightHandler"));
	}

	@Test
	public void actualRequestWithCorsConfigurationProvider() throws Exception {
		this.request.setMethod(RequestMethod.GET.name());
		this.request.setRequestURI("/cors");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HandlerExecutionChain chain = handlerMapping.getHandler(this.request);
		assertNotNull(chain);
		assertTrue(chain.getHandler() instanceof CorsAwareHandler);
		CorsConfiguration config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(config.getAllowedOrigins().toArray(), new String[]{"*"});
	}

	@Test
	public void preflightRequestWithCorsConfigurationProvider() throws Exception {
		this.request.setMethod(RequestMethod.OPTIONS.name());
		this.request.setRequestURI("/cors");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HandlerExecutionChain chain = handlerMapping.getHandler(this.request);
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
		assertTrue(chain.getHandler().getClass().getSimpleName().equals("PreFlightHandler"));
		CorsConfiguration config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(config.getAllowedOrigins().toArray(), new String[]{"*"});
	}

	@Test
	public void actualRequestWithMappedCorsConfiguration() throws Exception {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/foo", config));
		this.request.setMethod(RequestMethod.GET.name());
		this.request.setRequestURI("/foo");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HandlerExecutionChain chain = handlerMapping.getHandler(this.request);
		assertNotNull(chain);
		assertTrue(chain.getHandler() instanceof SimpleHandler);
		config = getCorsConfiguration(chain, false);
		assertNotNull(config);
		assertArrayEquals(config.getAllowedOrigins().toArray(), new String[]{"*"});
	}

	@Test
	public void preflightRequestWithMappedCorsConfiguration() throws Exception {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedOrigin("*");
		this.handlerMapping.setCorsConfigurations(Collections.singletonMap("/foo", config));
		this.request.setMethod(RequestMethod.OPTIONS.name());
		this.request.setRequestURI("/foo");
		this.request.addHeader(HttpHeaders.ORIGIN, "http://domain2.com");
		this.request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HandlerExecutionChain chain = handlerMapping.getHandler(this.request);
		assertNotNull(chain);
		assertNotNull(chain.getHandler());
		assertTrue(chain.getHandler().getClass().getSimpleName().equals("PreFlightHandler"));
		config = getCorsConfiguration(chain, true);
		assertNotNull(config);
		assertArrayEquals(config.getAllowedOrigins().toArray(), new String[]{"*"});
	}


	private CorsConfiguration getCorsConfiguration(HandlerExecutionChain chain, boolean isPreFlightRequest) {
		if (isPreFlightRequest) {
			Object handler = chain.getHandler();
			assertTrue(handler.getClass().getSimpleName().equals("PreFlightHandler"));
			DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
			return (CorsConfiguration)accessor.getPropertyValue("config");
		}
		else {
			HandlerInterceptor[] interceptors = chain.getInterceptors();
			if (interceptors != null) {
				for (HandlerInterceptor interceptor : interceptors) {
					if (interceptor.getClass().getSimpleName().equals("CorsInterceptor")) {
						DirectFieldAccessor accessor = new DirectFieldAccessor(interceptor);
						return (CorsConfiguration) accessor.getPropertyValue("config");
					}
				}
			}
		}
		return null;
	}

	public class TestHandlerMapping extends AbstractHandlerMapping {

		@Override
		protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
			if (request.getRequestURI().equals("/cors")) {
				return new CorsAwareHandler();
			}
			return new SimpleHandler();
		}
	}

	public class SimpleHandler extends WebContentGenerator implements HttpRequestHandler {

		public SimpleHandler() {
			super(METHOD_GET);
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

			response.setStatus(HttpStatus.OK.value());
		}

	}

	public class CorsAwareHandler extends SimpleHandler implements CorsConfigurationSource {

		@Override
		public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("*");
			return config;
		}

	}

}
