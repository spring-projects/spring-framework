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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ServerResponse.SseBuilder}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 */
class SseServerResponseTests {

	private final MockHttpServletRequest mockRequest = new MockHttpServletRequest("GET", "https://example.com");

	private final MockHttpServletResponse mockResponse = new MockHttpServletResponse();


	@BeforeEach
	void setUp() {
		this.mockRequest.setAsyncSupported(true);
	}


	@Test
	void sendString() throws Exception {
		String body = "foo bar";
		ServerResponse response = ServerResponse.sse(sse -> {
			try {
				sse.send(body);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});

		ServerResponse.Context context = Collections::emptyList;

		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();

		String expected = "data:" + body + "\n\n";
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
	}

	@Test
	void sendObject() throws Exception {
		Person person = new Person("John Doe", 42);
		ServerResponse response = ServerResponse.sse(sse -> {
			try {
				sse.send(person);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});

		ServerResponse.Context context = () -> List.of(new JacksonJsonHttpMessageConverter());

		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();

		String expected = "data:{\"name\":\"John Doe\",\"age\":42}\n\n";
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
	}

	@Test
	void sendObjectWithPrettyPrint() throws Exception {
		Person person = new Person("John Doe", 42);
		ServerResponse response = ServerResponse.sse(sse -> {
			try {
				sse.send(person);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});

		JsonMapper jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
		JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
		ServerResponse.Context context = () -> List.of(converter);

		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();

		String expected = """
				data:{
				data:  "name" : "John Doe",
				data:  "age" : 42
				data:}

				""";
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
	}

	@Test
	void builder() throws Exception {
		ServerResponse response = ServerResponse.sse(sse -> {
			try {
				sse.id("id")
						.event("name")
						.comment("comment line 1\ncomment line 2")
						.retry(Duration.ofSeconds(1))
						.data("data");
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});

		ServerResponse.Context context = Collections::emptyList;

		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();

		String expected = """
				id:id
				event:name
				:comment line 1
				:comment line 2
				retry:1000
				data:data

				""";
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
	}

	@Test
	void sendWithoutData() throws Exception {
		ServerResponse response = ServerResponse.sse(sse -> {
			try {
				sse.event("custom").send();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});

		ServerResponse.Context context = Collections::emptyList;

		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();

		String expected = "event:custom\n\n";
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
	}

	@Test // gh-34608
	void sendHeartbeat() throws Exception {
		ServerResponse response = ServerResponse.sse(sse -> {
			try {
				sse.comment("").send();
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		});

		ServerResponse.Context context = Collections::emptyList;

		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();

		String expected = ":\n\n";
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(expected);
	}


	private static final class Person {

		private final String name;

		private final int age;


		public Person(String name, int age) {
			this.name = name;
			this.age = age;
		}

		@SuppressWarnings("unused")
		public String getName() {
			return this.name;
		}

		@SuppressWarnings("unused")
		public int getAge() {
			return this.age;
		}
	}

}
