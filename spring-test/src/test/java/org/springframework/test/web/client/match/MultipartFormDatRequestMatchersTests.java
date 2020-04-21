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
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit tests for {@link MultipartFormDataRequestMatchers}.
 *
 * @author Valentin Spac
 */
public class MultipartFormDatRequestMatchersTests {

	private MockClientHttpRequest request = new MockClientHttpRequest();
	private MultipartFormDataRequestMatchers multipartRequestMatchers = MockRestRequestMatchers.content().multipart();

	@BeforeEach
	public void setUp() {
		this.request.getHeaders().setContentType(MediaType.MULTIPART_FORM_DATA);
	}

	@Test
	public void testContains() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("foo", "baz");
		payload.add("lorem", "ipsum");

		writeForm(payload);

		multipartRequestMatchers.param("foo", containsInAnyOrder("bar", "baz")).match(request);
	}

	@Test
	public void testNoContains() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("foo", "baz");
		payload.add("lorem", "ipsum");

		writeForm(payload);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.param("foo", containsInAnyOrder("wrongValue")).match(request));
	}

	@Test
	public void testEqualMatcher() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);
		multipartRequestMatchers.param("foo", equalTo("bar")).match(request);
	}

	@Test
	public void testNoEqualMatcher() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);

		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.param("foo", equalTo("wrongValue")).match(request));
	}

	@Test
	public void testParamMatch() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);
		multipartRequestMatchers.param("foo", "bar").match(request);
	}

	@Test
	public void testParamNoMatch() throws Exception {
		MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
		payload.add("foo", "bar");
		payload.add("baz", "foobar");

		writeForm(payload);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.param("foo", "wrongValue").match(request));
	}

	@Test
	public void testParamsMultimapMatch() throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("foo", "value 1");
		map.add("bar", "value A");
		map.add("baz", "value B");

		writeForm(map);

		multipartRequestMatchers.params(map).match(this.request);
	}

	@Test
	public void testParamsMultimapNoMatch() throws Exception {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("foo", "foo value");
		map.add("bar", "bar value");
		map.add("baz", "baz value");
		map.add("baz", "second baz value");

		writeForm(map);

		map.set("baz", "wrong baz value");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.params(map).match(this.request));
	}


	@Test
	public void testResourceMatch() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("fooFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());
		MockMultipartFile foobar = new MockMultipartFile("foobarFile", "foobar.txt", "text/plain", "Foobar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());
		map.add(foobar.getName(), foobar.getResource());

		writeForm(map);

		multipartRequestMatchers.file(foo.getName(), foo.getResource(), bar.getResource()).match(this.request);
	}

	@Test
	public void testResourceNoMatch() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("barFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());

		writeForm(map);


		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.file(foo.getName(), foo.getResource(), bar.getResource()).match(this.request));
	}

	@Test
	public void testByteArrayMatch() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("fooFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());
		MockMultipartFile foobar = new MockMultipartFile("foobarFile", "foobar.txt", "text/plain", "Foobar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());
		map.add(foobar.getName(), foobar.getResource());

		writeForm(map);

		multipartRequestMatchers.file(foo.getName(), foo.getBytes(), bar.getBytes()).match(this.request);
	}

	@Test
	public void testByteArrayNoMatch() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("barFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());

		writeForm(map);


		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.file(foo.getName(), bar.getBytes()).match(this.request));
	}

	@Test
	public void testResourceMatcher() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("barFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());

		writeForm(map);
		multipartRequestMatchers.file(foo.getName(), resourceMatcher(foo.getResource())).match(this.request);
	}

	@Test
	public void testResourceMatcherNoMatch() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("barFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());

		writeForm(map);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				multipartRequestMatchers.file(foo.getName(), resourceMatcher(bar.getResource())).match(this.request));
	}

	@NotNull
	private Matcher<Resource> resourceMatcher(Resource expectedResource) {
		return new TypeSafeMatcher<Resource>() {

			@Override
			public void describeTo(Description description) {
				description.appendValue(expectedResource.getDescription());
			}

			@Override
			protected boolean matchesSafely(Resource resource) {
				try {
					byte[] actual = IOUtils.toByteArray(resource.getInputStream());
					byte[] expected = IOUtils.toByteArray(expectedResource.getInputStream());

					return StringUtils.equals(expectedResource.getFilename(), resource.getFilename())
							&& Arrays.equals(expected, actual);
				}
				catch (IOException e) {
					throw new RuntimeException("Could not read resource content");
				}
			}
		};
	}

	@Test
	public void testResourceMultimapMatch() throws Exception {
		MockMultipartFile foo = new MockMultipartFile("fooFile", "foo.txt", "text/plain", "Foo Lorem ipsum".getBytes());
		MockMultipartFile bar = new MockMultipartFile("barFile", "bar.txt", "text/plain", "Bar Lorem ipsum".getBytes());

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("fooParam", "foo value");
		map.add("barParam", "bar value");
		map.add(foo.getName(), foo.getResource());
		map.add(bar.getName(), bar.getResource());

		writeForm(map);

		MultiValueMap<String, Resource> files = new LinkedMultiValueMap<>();
		files.add(foo.getName(), foo.getResource());
		files.add(bar.getName(), bar.getResource());

		multipartRequestMatchers.files(files).match(this.request);
	}


	private void writeForm(MultiValueMap<String, ?> payload) throws IOException {
		FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
		formHttpMessageConverter.write(payload, MediaType.MULTIPART_FORM_DATA, new HttpOutputMessage() {
			@Override
			public OutputStream getBody() throws IOException {
				return MultipartFormDatRequestMatchersTests.this.request.getBody();
			}

			@Override
			public HttpHeaders getHeaders() {
				return MultipartFormDatRequestMatchersTests.this.request.getHeaders();
			}
		});
	}
}
