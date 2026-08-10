/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

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
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.servlet.function.support.RouterFunctionMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link DefaultPreFlightRequestHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 7.1
 */
class DefaultPreFlightRequestHandlerTests {

	@Test
	void detectHandlerMappings() {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		context.registerSingleton("A", SimpleUrlHandlerMapping.class);
		context.registerSingleton("B", SimpleUrlHandlerMapping.class);
		context.registerSingleton("C", SimpleUrlHandlerMapping.class);
		context.refresh();

		List<?> expected = Arrays.asList(context.getBean("A"), context.getBean("B"), context.getBean("C"));
		List<HandlerMapping> actual = initHandler(context).getHandlerMappings();

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
		List<HandlerMapping> actual = initHandler(context).getHandlerMappings();

		assertThat(actual).isEqualTo(expected);
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

		initHandler(context).handlePreFlight(request, response);

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

		assertThatThrownBy(() -> initHandler(context).handlePreFlight(request, response))
				.isInstanceOf(NoHandlerFoundException.class);

		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
		assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).isNull();
	}

	private static DefaultPreFlightRequestHandler initHandler(WebApplicationContext context) {
		DefaultPreFlightRequestHandler handler = new DefaultPreFlightRequestHandler();
		handler.setApplicationContext(context);
		handler.afterPropertiesSet();
		return handler;
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

}
