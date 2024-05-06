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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.json.AbstractJsonContentAssert;
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
 * Tests for {@link AssertableMockMvc}.
 *
 * @author Stephane Nicoll
 */
class AssertableMockMvcTests {

	private static final MappingJackson2HttpMessageConverter jsonHttpMessageConverter =
			new MappingJackson2HttpMessageConverter(new ObjectMapper());


	@Test
	void createShouldRejectNullMockMvc() {
		assertThatIllegalArgumentException().isThrownBy(() -> AssertableMockMvc.create(null));
	}

	@Test
	void createWithExistingWebApplicationContext() {
		try (GenericWebApplicationContext wac = create(WebConfiguration.class)) {
			AssertableMockMvc mockMvc = AssertableMockMvc.from(wac);
			assertThat(mockMvc.perform(post("/increase"))).body().isEqualTo("counter 41");
			assertThat(mockMvc.perform(post("/increase"))).body().isEqualTo("counter 42");
		}
	}

	@Test
	void createWithControllerClassShouldInstantiateControllers() {
		AssertableMockMvc mockMvc = AssertableMockMvc.of(HelloController.class, CounterController.class);
		assertThat(mockMvc.perform(get("/hello"))).body().isEqualTo("Hello World");
		assertThat(mockMvc.perform(post("/increase"))).body().isEqualTo("counter 1");
		assertThat(mockMvc.perform(post("/increase"))).body().isEqualTo("counter 2");
	}

	@Test
	void createWithControllersShouldUseThemAsIs() {
		AssertableMockMvc mockMvc = AssertableMockMvc.of(new HelloController(),
				new CounterController(new AtomicInteger(41)));
		assertThat(mockMvc.perform(get("/hello"))).body().isEqualTo("Hello World");
		assertThat(mockMvc.perform(post("/increase"))).body().isEqualTo("counter 42");
		assertThat(mockMvc.perform(post("/increase"))).body().isEqualTo("counter 43");
	}

	@Test
	void createWithControllerAndCustomizations() {
		AssertableMockMvc mockMvc = AssertableMockMvc.of(List.of(new HelloController()), builder ->
				builder.defaultRequest(get("/hello").accept(MediaType.APPLICATION_JSON)).build());
		assertThat(mockMvc.perform(get("/hello"))).hasStatus(HttpStatus.NOT_ACCEPTABLE);
	}

	@Test
	void createWithControllersHasNoHttpMessageConverter() {
		AssertableMockMvc mockMvc = AssertableMockMvc.of(new HelloController());
		AbstractJsonContentAssert<?> jsonContentAssert = assertThat(mockMvc.perform(get("/json"))).hasStatusOk().body().jsonPath();
		assertThatIllegalStateException()
				.isThrownBy(() -> jsonContentAssert.extractingPath("$").convertTo(Message.class))
				.withMessageContaining("No JSON message converter available");
	}

	@Test
	void createWithControllerCanConfigureHttpMessageConverters() {
		AssertableMockMvc mockMvc = AssertableMockMvc.of(HelloController.class)
				.withHttpMessageConverters(List.of(jsonHttpMessageConverter));
		assertThat(mockMvc.perform(get("/json"))).hasStatusOk().body().jsonPath()
				.extractingPath("$").convertTo(Message.class).satisfies(message -> {
					assertThat(message.message()).isEqualTo("Hello World");
					assertThat(message.counter()).isEqualTo(42);
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	void withHttpMessageConverterDetectsJsonConverter() {
		MappingJackson2HttpMessageConverter converter = spy(jsonHttpMessageConverter);
		AssertableMockMvc mockMvc = AssertableMockMvc.of(HelloController.class)
				.withHttpMessageConverters(List.of(mock(), mock(), converter));
		assertThat(mockMvc.perform(get("/json"))).hasStatusOk().body().jsonPath()
				.extractingPath("$").convertTo(Message.class).satisfies(message -> {
					assertThat(message.message()).isEqualTo("Hello World");
					assertThat(message.counter()).isEqualTo(42);
				});
		verify(converter).canWrite(Map.class, MediaType.APPLICATION_JSON);
	}

	@Test
	void performWithUnresolvedExceptionSetsException() {
		AssertableMockMvc mockMvc = AssertableMockMvc.of(HelloController.class);
		AssertableMvcResult result = mockMvc.perform(get("/error"));
		assertThat(result.getUnresolvedException()).isInstanceOf(ServletException.class)
				.cause().isInstanceOf(IllegalStateException.class).hasMessage("Expected");
		assertThat(result).hasFieldOrPropertyWithValue("target", null);
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
