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

package org.springframework.web.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletConfig;

import static org.assertj.core.api.Assertions.assertThat;

class DispatcherServletContentLengthTests {


	private AnnotationConfigWebApplicationContext context;

	private DispatcherServlet servlet;


	@BeforeEach
	void setUp() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(WebConfig.class);
		this.servlet = new DispatcherServlet(this.context);
		MockServletConfig config = new MockServletConfig();
		config.addInitParameter("jakarta.servlet.http.legacyDoHead", "true");
		this.servlet.init(config);
	}

	@AfterEach
	void tearDown() {
		this.servlet.destroy();
		this.context.close();
	}

	@ParameterizedTest
	@CsvSource(value = {
			"GET, /empty, 1000, null, ''",
			"HEAD, /empty, 1000, null, ''",
			"GET, /body, 1000, 14, Error occurred",
			"HEAD, /body, 1000, 14, ''",
			"GET, /empty, -1, null, ''",
			"HEAD, /empty, -1, null, ''",
			"GET, /body, -1, 14, Error occurred",
			"HEAD, /body, -1, 14, ''"
	}, nullValues = "null")
	void shouldResetContentLengthIfNotCommitted(String method, String path, int originalLength,
			@Nullable String expectedLength, String expectedBody) throws Exception {

		MockHttpServletRequest request = request(method, path, originalLength);
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.servlet.service(request, response);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getHeader(HttpHeaders.CONTENT_LENGTH)).isEqualTo(expectedLength);
		assertThat(response.getContentAsString()).isEqualTo(expectedBody);
		assertThat(response.getHeader("X-Preserved")).isEqualTo("sentinel");
		assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).isNull();
		assertThat(handledCount(this.context)).isOne();
	}

	@ParameterizedTest
	@CsvSource({"GET, /empty, 6", "HEAD, /empty, 0", "GET, /body, 20", "HEAD, /body, 0"})
	void shouldPreserveContentLengthIfCommitted(String method, String path, int expectedBodyLength) throws Exception {
		MockHttpServletRequest request = request(method, path, 1000);
		request.addParameter("commit", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.servlet.service(request, response);

		assertThat(response.isCommitted()).isTrue();
		assertThat(response.getStatus()).isEqualTo(202);
		assertThat(response.getHeader(HttpHeaders.CONTENT_LENGTH)).isEqualTo("1000");
		assertThat(response.getContentAsByteArray()).hasSize(expectedBodyLength);
		assertThat(handledCount(this.context)).isOne();
	}

	private MockHttpServletRequest request(String method, String path, int length) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		request.addParameter("length", Integer.toString(length));
		return request;
	}

	static int handledCount(AnnotationConfigWebApplicationContext context) {
		return context.getBean(EmptyController.class).handled.get() +
				context.getBean(BodyController.class).handled.get();
	}

	static void fail(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String value = request.getParameter("length");
		int length = (value != null ? Integer.parseInt(value) : 1000);
		response.setHeader("X-Preserved", "sentinel");
		response.setContentType("application/octet-stream");
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=original.bin");
		if (length >= 0) {
			response.setContentLength(length);
		}
		if ("true".equals(request.getParameter("commit"))) {
			response.setStatus(202);
			response.getOutputStream().write("before".getBytes(StandardCharsets.US_ASCII));
			response.flushBuffer();
		}
		throw new IllegalArgumentException("error");
	}


	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class WebConfig {

		@Bean
		EmptyController emptyController() {
			return new EmptyController();
		}

		@Bean
		BodyController bodyController() {
			return new BodyController();
		}
	}


	@Controller
	static class EmptyController {

		final AtomicInteger handled = new AtomicInteger();

		@GetMapping("/empty")
		void test(HttpServletRequest request, HttpServletResponse response) throws IOException {
			fail(request, response);
		}

		@ExceptionHandler(IllegalArgumentException.class)
		ResponseEntity<Void> handle() {
			this.handled.incrementAndGet();
			return ResponseEntity.badRequest().build();
		}
	}


	@Controller
	static class BodyController {

		final AtomicInteger handled = new AtomicInteger();

		@GetMapping("/body")
		void test(HttpServletRequest request, HttpServletResponse response) throws IOException {
			fail(request, response);
		}

		@ExceptionHandler(IllegalArgumentException.class)
		ResponseEntity<String> handle() {
			this.handled.incrementAndGet();
			return ResponseEntity.badRequest().body("Error occurred");
		}
	}

}
