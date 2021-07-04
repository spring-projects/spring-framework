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

package org.springframework.test.web.servlet.result;

import java.nio.charset.StandardCharsets;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class ContentResultMatchersTests {

	@Test
	public void typeMatches() throws Exception {
		new ContentResultMatchers().contentType(MediaType.APPLICATION_JSON_VALUE).match(getStubMvcResult(CONTENT));
	}

	@Test
	public void typeNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().contentType("text/plain").match(getStubMvcResult(CONTENT)));
	}

	@Test
	public void string() throws Exception {
		new ContentResultMatchers().string(new String(CONTENT.getBytes("UTF-8"))).match(getStubMvcResult(CONTENT));
	}

	@Test
	public void stringNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().encoding("bogus").match(getStubMvcResult(CONTENT)));
	}

	@Test
	public void stringMatcher() throws Exception {
		String content = new String(CONTENT.getBytes("UTF-8"));
		new ContentResultMatchers().string(Matchers.equalTo(content)).match(getStubMvcResult(CONTENT));
	}

	@Test
	public void stringMatcherNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().string(Matchers.equalTo("bogus")).match(getStubMvcResult(CONTENT)));
	}

	@Test
	public void bytes() throws Exception {
		new ContentResultMatchers().bytes(CONTENT.getBytes("UTF-8")).match(getStubMvcResult(CONTENT));
	}

	@Test
	public void bytesNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().bytes("bogus".getBytes()).match(getStubMvcResult(CONTENT)));
	}

	@Test
	public void jsonLenientMatch() throws Exception {
		new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}").match(getStubMvcResult(CONTENT));
		new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}", false).match(getStubMvcResult(CONTENT));
	}

	@Test
	public void jsonStrictMatch() throws Exception {
		new ContentResultMatchers().json("{\n \"foo\":\"bar\",   \"foo array\":[\"foo\",\"bar\"] \n}", true).match(getStubMvcResult(CONTENT));
		new ContentResultMatchers().json("{\n \"foo array\":[\"foo\",\"bar\"], \"foo\":\"bar\" \n}", true).match(getStubMvcResult(CONTENT));
	}

	@Test
	public void jsonLenientNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().json("{\n\"fooo\":\"bar\"\n}").match(getStubMvcResult(CONTENT)));
	}

	@Test
	public void jsonStrictNoMatch() throws Exception {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().json("{\"foo\":\"bar\",   \"foo array\":[\"bar\",\"foo\"]}", true).match(getStubMvcResult(CONTENT)));
	}

	@Test  // gh-23622
	public void jsonUtf8Match() throws Exception {
		new ContentResultMatchers().json("{\"name\":\"Jürgen\"}").match(getStubMvcResult(UTF8_CONTENT));
	}

	private static final String CONTENT = "{\"foo\":\"bar\",\"foo array\":[\"foo\",\"bar\"]}";

	private static final String UTF8_CONTENT = "{\"name\":\"Jürgen\"}";

	private StubMvcResult getStubMvcResult(String content) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
		return new StubMvcResult(null, null, null, null, null, null, response);
	}

}
