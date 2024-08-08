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

package org.springframework.test.web.servlet.result;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.StubMvcResult;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class ContentResultMatchersTests {

	@Test
	void typeMatches() throws Exception {
		new ContentResultMatchers().contentType(APPLICATION_JSON_VALUE).match(getStubMvcResult(CONTENT));
	}

	@Test
	void typeNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().contentType("text/plain").match(getStubMvcResult(CONTENT)));
	}

	@Test
	void string() throws Exception {
		new ContentResultMatchers().string(new String(CONTENT.getBytes(UTF_8))).match(getStubMvcResult(CONTENT));
	}

	@Test
	void stringNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().encoding("bogus").match(getStubMvcResult(CONTENT)));
	}

	@Test
	void stringMatcher() throws Exception {
		String content = new String(CONTENT.getBytes(UTF_8));
		new ContentResultMatchers().string(Matchers.equalTo(content)).match(getStubMvcResult(CONTENT));
	}

	@Test
	void stringMatcherNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().string(Matchers.equalTo("bogus")).match(getStubMvcResult(CONTENT)));
	}

	@Test
	void bytes() throws Exception {
		new ContentResultMatchers().bytes(CONTENT.getBytes(UTF_8)).match(getStubMvcResult(CONTENT));
	}

	@Test
	void bytesNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().bytes("bogus".getBytes()).match(getStubMvcResult(CONTENT)));
	}

	@Test
	void jsonLenientMatch() throws Exception {
		new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}").match(getStubMvcResult(CONTENT));
		new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}",
				JsonCompareMode.LENIENT).match(getStubMvcResult(CONTENT));
	}

	@Test
	@Deprecated
	void jsonLenientMatchWithDeprecatedBooleanFlag() throws Exception {
		new ContentResultMatchers().json("{\n \"foo\" : \"bar\"  \n}", false).match(getStubMvcResult(CONTENT));
	}

	@Test
	void jsonStrictMatch() throws Exception {
		new ContentResultMatchers().json("{\n \"foo\":\"bar\",   \"foo array\":[\"foo\",\"bar\"] \n}",
				JsonCompareMode.STRICT).match(getStubMvcResult(CONTENT));
		new ContentResultMatchers().json("{\n \"foo array\":[\"foo\",\"bar\"], \"foo\":\"bar\" \n}",
				JsonCompareMode.STRICT).match(getStubMvcResult(CONTENT));
	}

	@Test
	@Deprecated
	void jsonStrictMatchWithDeprecatedBooleanFlag() throws Exception {
		new ContentResultMatchers().json("{\n \"foo\":\"bar\",   \"foo array\":[\"foo\",\"bar\"] \n}", true)
				.match(getStubMvcResult(CONTENT));
		new ContentResultMatchers().json("{\n \"foo array\":[\"foo\",\"bar\"], \"foo\":\"bar\" \n}", true)
				.match(getStubMvcResult(CONTENT));
	}

	@Test
	void jsonLenientNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().json("{\n\"fooo\":\"bar\"\n}").match(getStubMvcResult(CONTENT)));
	}

	@Test
	void jsonStrictNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().json("{\"foo\":\"bar\",   \"foo array\":[\"bar\",\"foo\"]}",
						JsonCompareMode.STRICT).match(getStubMvcResult(CONTENT)));
	}

	@Test
	@Deprecated
	void jsonStrictNoMatchWithDeprecatedBooleanFlag() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new ContentResultMatchers().json("{\"foo\":\"bar\",   \"foo array\":[\"bar\",\"foo\"]}", true)
						.match(getStubMvcResult(CONTENT)));
	}

	@Test  // gh-23622
	void jsonUtf8Match() throws Exception {
		new ContentResultMatchers().json("{\"name\":\"Jürgen\"}").match(getStubMvcResult(UTF8_CONTENT));
	}

	private static final String CONTENT = "{\"foo\":\"bar\",\"foo array\":[\"foo\",\"bar\"]}";

	private static final String UTF8_CONTENT = "{\"name\":\"Jürgen\"}";

	private StubMvcResult getStubMvcResult(String content) throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", APPLICATION_JSON_VALUE);
		response.getOutputStream().write(content.getBytes(UTF_8));
		return new StubMvcResult(null, null, null, null, null, null, response);
	}

}
