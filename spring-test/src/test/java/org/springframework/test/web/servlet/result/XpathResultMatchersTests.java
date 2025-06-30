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

package org.springframework.test.web.servlet.result;

import java.nio.charset.StandardCharsets;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.StubMvcResult;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link XpathResultMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class XpathResultMatchersTests {

	private static final String RESPONSE_CONTENT = "<foo><bar>111</bar><bar>true</bar></foo>";


	@Test
	public void node() throws Exception {
		new XpathResultMatchers("/foo/bar", null).node(Matchers.notNullValue()).match(getStubMvcResult());
	}

	@Test
	public void nodeNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar", null).node(Matchers.nullValue()).match(getStubMvcResult()));
	}

	@Test
	public void nodeList() throws Exception {
		new XpathResultMatchers("/foo/bar", null).nodeList(Matchers.notNullValue()).match(getStubMvcResult());
	}

	@Test
	public void nodeListNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar", null).nodeList(Matchers.nullValue()).match(getStubMvcResult()));
	}

	@Test
	public void exists() throws Exception {
		new XpathResultMatchers("/foo/bar", null).exists().match(getStubMvcResult());
	}

	@Test
	public void existsNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/Bar", null).exists().match(getStubMvcResult()));
	}

	@Test
	public void doesNotExist() throws Exception {
		new XpathResultMatchers("/foo/Bar", null).doesNotExist().match(getStubMvcResult());
	}

	@Test
	public void doesNotExistNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar", null).doesNotExist().match(getStubMvcResult()));
	}

	@Test
	public void nodeCount() throws Exception {
		new XpathResultMatchers("/foo/bar", null).nodeCount(2).match(getStubMvcResult());
	}

	@Test
	public void nodeCountNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar", null).nodeCount(1).match(getStubMvcResult()));
	}

	@Test
	public void string() throws Exception {
		new XpathResultMatchers("/foo/bar[1]", null).string("111").match(getStubMvcResult());
	}

	@Test
	public void stringNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar[1]", null).string("112").match(getStubMvcResult()));
	}

	@Test
	public void number() throws Exception {
		new XpathResultMatchers("/foo/bar[1]", null).number(111.0).match(getStubMvcResult());
	}

	@Test
	public void numberNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar[1]", null).number(111.1).match(getStubMvcResult()));
	}

	@Test
	public void booleanValue() throws Exception {
		new XpathResultMatchers("/foo/bar[2]", null).booleanValue(true).match(getStubMvcResult());
	}

	@Test
	public void booleanValueNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathResultMatchers("/foo/bar[2]", null).booleanValue(false).match(getStubMvcResult()));
	}

	@Test
	public void stringEncodingDetection() throws Exception {
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
				"<person><name>Jürgen</name></person>";
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/xml");
		StreamUtils.copy(bytes, response.getOutputStream());
		StubMvcResult result = new StubMvcResult(null, null, null, null, null, null, response);

		new XpathResultMatchers("/person/name", null).string("Jürgen").match(result);
	}


	private StubMvcResult getStubMvcResult() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		response.addHeader("Content-Type", "application/xml");
		response.getWriter().print(new String(RESPONSE_CONTENT.getBytes(StandardCharsets.ISO_8859_1)));
		return new StubMvcResult(null, null, null, null, null, null, response);
	}

}
