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

package org.springframework.test.web.servlet.assertj;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.cglib.core.internal.Function;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.json.AbstractJsonContentAssert;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests for {@link MockMvcTester}.
 *
 * @author Stephane Nicoll
 */
class MockMvcTesterTests {

	private static final MappingJackson2HttpMessageConverter jsonHttpMessageConverter =
			new MappingJackson2HttpMessageConverter(new ObjectMapper());

	private final ServletContext servletContext = new MockServletContext();


	@Test
	void createShouldRejectNullMockMvc() {
		assertThatIllegalArgumentException().isThrownBy(() -> MockMvcTester.create(null));
	}

	@Test
	void createWithExistingWebApplicationContext() {
		try (GenericWebApplicationContext wac = create(WebConfiguration.class)) {
			MockMvcTester mockMvc = MockMvcTester.from(wac);
			assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 41");
			assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 42");
		}
	}

	@Test
	void createWithControllerClassShouldInstantiateControllers() {
		MockMvcTester mockMvc = MockMvcTester.of(HelloController.class, CounterController.class);
		assertThat(mockMvc.perform(get("/hello"))).hasBodyTextEqualTo("Hello World");
		assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 1");
		assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 2");
	}

	@Test
	void createWithControllersShouldUseThemAsIs() {
		MockMvcTester mockMvc = MockMvcTester.of(new HelloController(),
				new CounterController(new AtomicInteger(41)));
		assertThat(mockMvc.perform(get("/hello"))).hasBodyTextEqualTo("Hello World");
		assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 42");
		assertThat(mockMvc.perform(post("/increase"))).hasBodyTextEqualTo("counter 43");
	}

	@Test
	void createWithControllerAndCustomizations() {
		MockMvcTester mockMvc = MockMvcTester.of(List.of(new HelloController()), builder ->
				builder.defaultRequest(get("/hello").accept(MediaType.APPLICATION_JSON)).build());
		assertThat(mockMvc.perform(get("/hello"))).hasStatus(HttpStatus.NOT_ACCEPTABLE);
	}

	@Test
	void createWithControllersHasNoHttpMessageConverter() {
		MockMvcTester mockMvc = MockMvcTester.of(new HelloController());
		AbstractJsonContentAssert<?> jsonContentAssert = assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson();
		assertThatIllegalStateException()
				.isThrownBy(() -> jsonContentAssert.extractingPath("$").convertTo(Message.class))
				.withMessageContaining("No JSON message converter available");
	}

	@Test
	void createWithControllerCanConfigureHttpMessageConverters() {
		MockMvcTester mockMvc = MockMvcTester.of(HelloController.class)
				.withHttpMessageConverters(List.of(jsonHttpMessageConverter));
		assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson()
				.extractingPath("$").convertTo(Message.class).satisfies(message -> {
					assertThat(message.message()).isEqualTo("Hello World");
					assertThat(message.counter()).isEqualTo(42);
				});
	}

	@Test
	void withHttpMessageConverterUsesConverter() {
		MappingJackson2HttpMessageConverter converter = spy(jsonHttpMessageConverter);
		MockMvcTester mockMvc = MockMvcTester.of(HelloController.class)
				.withHttpMessageConverters(List.of(mock(), mock(), converter));
		assertThat(mockMvc.perform(get("/json"))).hasStatusOk().bodyJson()
				.extractingPath("$").convertTo(Message.class).satisfies(message -> {
					assertThat(message.message()).isEqualTo("Hello World");
					assertThat(message.counter()).isEqualTo(42);
				});
		verify(converter).canWrite(LinkedHashMap.class, LinkedHashMap.class, MediaType.APPLICATION_JSON);
	}

	@Test
	void performWithUnresolvedExceptionSetsException() {
		MockMvcTester mockMvc = MockMvcTester.of(HelloController.class);
		MvcTestResult result = mockMvc.perform(get("/error"));
		assertThat(result.getUnresolvedException()).isInstanceOf(ServletException.class)
				.cause().isInstanceOf(IllegalStateException.class).hasMessage("Expected");
		assertThat(result).hasFieldOrPropertyWithValue("mvcResult", null);
	}

	@Test
	void getConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.get().uri("/hello")))
				.satisfies(hasSettings(HttpMethod.GET, "/hello"));
	}

	@Test
	void headConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.head().uri("/download")))
				.satisfies(hasSettings(HttpMethod.HEAD, "/download"));
	}

	@Test
	void postConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.post().uri("/save")))
				.satisfies(hasSettings(HttpMethod.POST, "/save"));
	}

	@Test
	void putConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.put().uri("/save")))
				.satisfies(hasSettings(HttpMethod.PUT, "/save"));
	}

	@Test
	void patchConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.patch().uri("/update")))
				.satisfies(hasSettings(HttpMethod.PATCH, "/update"));
	}

	@Test
	void deleteConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.delete().uri("/users/42")))
				.satisfies(hasSettings(HttpMethod.DELETE, "/users/42"));
	}

	@Test
	void optionsConfiguresBuilder() {
		assertThat(createMockHttpServletRequest(tester -> tester.options().uri("/users")))
				.satisfies(hasSettings(HttpMethod.OPTIONS, "/users"));
	}

	@Test
	void methodConfiguresBuilderWithCustomMethod() {
		HttpMethod customMethod = HttpMethod.valueOf("CUSTOM");
		assertThat(createMockHttpServletRequest(tester -> tester.method(customMethod).uri("/hello")))
				.satisfies(hasSettings(customMethod, "/hello"));
	}

	private MockHttpServletRequest createMockHttpServletRequest(Function<MockMvcTester, MockMvcRequestBuilder> builder) {
		MockMvcTester mockMvcTester = MockMvcTester.of(HelloController.class);
		return builder.apply(mockMvcTester).buildRequest(this.servletContext);
	}

	private Consumer<MockHttpServletRequest> hasSettings(HttpMethod method, String uri) {
		return request -> {
			assertThat(request.getMethod()).isEqualTo(method.name());
			assertThat(request.getRequestURI()).isEqualTo(uri);
		};
	}

	private GenericWebApplicationContext create(Class<?>... classes) {
		GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(new MockServletContext());
		AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
		for (Class<?> beanClass : classes) {
			applicationContext.registerBean(beanClass);
		}
		applicationContext.refresh();
		return applicationContext;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class WebConfiguration {

		@Bean
		CounterController counterController() {
			return new CounterController(new AtomicInteger(40));
		}
	}


	@RestController
	private static class HelloController {

		@GetMapping(path = "/hello", produces = "text/plain")
		public String hello() {
			return "Hello World";
		}

		@GetMapping("/error")
		public String error() {
			throw new IllegalStateException("Expected");
		}

		@GetMapping(path = "/json", produces = "application/json")
		public String json() {
			return """
					{
						"message": "Hello World",
						"counter": 42
					}""";
		}
	}

	private record Message(String message, int counter) {}

	@RestController
	static class CounterController {

		private final AtomicInteger counter;

		CounterController() {
			this(new AtomicInteger());
		}

		CounterController(AtomicInteger counter) {
			this.counter = counter;
		}

		@PostMapping("/increase")
		String increase() {
			int value = this.counter.incrementAndGet();
			return "counter " + value;
		}
	}

}
