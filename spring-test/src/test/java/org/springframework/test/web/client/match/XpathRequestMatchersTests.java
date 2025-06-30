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

package org.springframework.test.web.client.match;

import java.io.IOException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link XpathRequestMatchers}.
 *
 * @author Rossen Stoyanchev
 */
public class XpathRequestMatchersTests {

	private static final String RESPONSE_CONTENT = "<foo><bar>111</bar><bar>true</bar></foo>";

	private MockClientHttpRequest request;


	@BeforeEach
	public void setUp() throws IOException {
		this.request = new MockClientHttpRequest();
		this.request.getBody().write(RESPONSE_CONTENT.getBytes());
	}


	@Test
	public void testNodeMatcher() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).node(Matchers.notNullValue()).match(this.request);
	}

	@Test
	public void testNodeMatcherNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).node(Matchers.nullValue()).match(this.request));
	}

	@Test
	public void testExists() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).exists().match(this.request);
	}

	@Test
	public void testExistsNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/Bar", null).exists().match(this.request));
	}

	@Test
	public void testDoesNotExist() throws Exception {
		new XpathRequestMatchers("/foo/Bar", null).doesNotExist().match(this.request);
	}

	@Test
	public void testDoesNotExistNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).doesNotExist().match(this.request));
	}

	@Test
	public void testNodeCount() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).nodeCount(2).match(this.request);
	}

	@Test
	public void testNodeCountNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).nodeCount(1).match(this.request));
	}

	@Test
	public void testString() throws Exception {
		new XpathRequestMatchers("/foo/bar[1]", null).string("111").match(this.request);
	}

	@Test
	public void testStringNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[1]", null).string("112").match(this.request));
	}

	@Test
	public void testNumber() throws Exception {
		new XpathRequestMatchers("/foo/bar[1]", null).number(111.0).match(this.request);
	}

	@Test
	public void testNumberNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[1]", null).number(111.1).match(this.request));
	}

	@Test
	public void testBoolean() throws Exception {
		new XpathRequestMatchers("/foo/bar[2]", null).booleanValue(true).match(this.request);
	}

	@Test
	public void testBooleanNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[2]", null).booleanValue(false).match(this.request));
	}

}
