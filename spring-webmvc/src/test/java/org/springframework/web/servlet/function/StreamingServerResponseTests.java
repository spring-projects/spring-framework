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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StreamingServerResponse}.
 * @author Brian Clozel
 */
class StreamingServerResponseTests {

	private MockHttpServletRequest mockRequest;

	private MockHttpServletResponse mockResponse;

	@BeforeEach
	void setUp() {
		this.mockRequest = new MockHttpServletRequest("GET", "https://example.com");
		this.mockRequest.setAsyncSupported(true);
		this.mockResponse = new MockHttpServletResponse();
	}

	@Test
	void writeSingleString() throws Exception {
		String body = "data: foo bar\n\n";
		ServerResponse response = ServerResponse.ok()
				.contentType(MediaType.TEXT_EVENT_STREAM)
				.stream(stream -> {
					try {
						stream.write(body).complete();
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});

		ServerResponse.Context context = Collections::emptyList;
		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();
		assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM.toString());
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(body);
	}

	@Test
	void writeBytes() throws Exception {
		String body = "data: foo bar\n\n";
		ServerResponse response = ServerResponse
				.ok()
				.contentType(MediaType.TEXT_EVENT_STREAM)
				.cacheControl(CacheControl.noCache())
				.stream(stream -> {
					try {
						stream.write(body.getBytes(StandardCharsets.UTF_8)).complete();
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});
		ServerResponse.Context context = Collections::emptyList;
		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();
		assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.TEXT_EVENT_STREAM.toString());
		assertThat(this.mockResponse.getContentAsString()).isEqualTo(body);
	}

	@Test
	void writeWithConverters() throws Exception {
		ServerResponse response = ServerResponse
				.ok()
				.contentType(MediaType.APPLICATION_NDJSON)
				.cacheControl(CacheControl.noCache())
				.stream(stream -> {
					try {
						stream.write(new Person("John", 51), MediaType.APPLICATION_JSON)
								.write(new byte[]{'\n'})
								.flush();
						stream.write(new Person("Jane", 42), MediaType.APPLICATION_JSON)
								.write(new byte[]{'\n'})
								.complete();
					}
					catch (IOException ex) {
						throw new UncheckedIOException(ex);
					}
				});

		ServerResponse.Context context = () -> Collections.singletonList(new MappingJackson2HttpMessageConverter());
		ModelAndView mav = response.writeTo(this.mockRequest, this.mockResponse, context);
		assertThat(mav).isNull();
		assertThat(this.mockResponse.getContentType()).isEqualTo(MediaType.APPLICATION_NDJSON.toString());
		assertThat(this.mockResponse.getContentAsString()).isEqualTo("""
				{"name":"John","age":51}
				{"name":"Jane","age":42}
				""");
	}


	record Person(String name, int age) {

	}

}
