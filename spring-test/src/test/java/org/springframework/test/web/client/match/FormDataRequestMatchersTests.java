/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.client.match;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit tests for {@link FormDataRequestMatchers}.
 *
 * @author Valentin Spac
 */
public class FormDataRequestMatchersTests {

	private MockClientHttpRequest request;

	@BeforeEach
	public void setUp() {
		this.request = new MockClientHttpRequest();
	}

	@Test
	public void testContains() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("foo", "baz");
		payload.add("lorem", "ipsum");

		writeForm(payload);

		MockRestRequestMatchers.formData().value("foo", containsInAnyOrder("bar", "baz")).match(request);
	}

	@Test
	public void testNoContains() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("foo", "baz");
		payload.add("lorem", "ipsum");

		writeForm(payload);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				MockRestRequestMatchers.formData().value("foo", containsInAnyOrder("wrongValue")).match(request));
	}

	@Test
	public void testEqualMatcher() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);
		MockRestRequestMatchers.formData().value("foo", equalTo("bar")).match(request);
	}

	@Test
	public void testNoEqualMatcher() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				MockRestRequestMatchers.formData().value("foo", equalTo("wrongValue")).match(request));
	}

	@Test
	public void testStringMatch() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);
		MockRestRequestMatchers.formData().value("foo", "bar").match(request);
	}

	@Test
	public void testStringNoMatch() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				MockRestRequestMatchers.formData().value("foo", "wrongValue").match(request));
	}


	private void writeForm(MultiValueMap<String, String> payload) throws IOException {
		FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
		formHttpMessageConverter.write(payload, MediaType.APPLICATION_FORM_URLENCODED, new HttpOutputMessage() {
			@Override
			public OutputStream getBody() throws IOException {
				return FormDataRequestMatchersTests.this.request.getBody();
			}

			@Override
			public HttpHeaders getHeaders() {
				return FormDataRequestMatchersTests.this.request.getHeaders();
			}
		});
	}
}
