/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class XpathRequestMatchersTests {

	private static final String RESPONSE_CONTENT = "<foo><bar>111</bar><bar>true</bar></foo>";

	private MockClientHttpRequest request;


	@BeforeEach
	void setUp() throws IOException {
		this.request = new MockClientHttpRequest();
		this.request.getBody().write(RESPONSE_CONTENT.getBytes());
	}


	@Test
	void nodeMatcher() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).node(Matchers.notNullValue()).match(this.request);
	}

	@Test
	void nodeMatcherNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).node(Matchers.nullValue()).match(this.request));
	}

	@Test
	void exists() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).exists().match(this.request);
	}

	@Test
	void existsNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/Bar", null).exists().match(this.request));
	}

	@Test
	void doesNotExist() throws Exception {
		new XpathRequestMatchers("/foo/Bar", null).doesNotExist().match(this.request);
	}

	@Test
	void doesNotExistNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).doesNotExist().match(this.request));
	}

	@Test
	void nodeCount() throws Exception {
		new XpathRequestMatchers("/foo/bar", null).nodeCount(2).match(this.request);
	}

	@Test
	void nodeCountNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar", null).nodeCount(1).match(this.request));
	}

	@Test
	void string() throws Exception {
		new XpathRequestMatchers("/foo/bar[1]", null).string("111").match(this.request);
	}

	@Test
	void stringNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[1]", null).string("112").match(this.request));
	}

	@Test
	void number() throws Exception {
		new XpathRequestMatchers("/foo/bar[1]", null).number(111.0).match(this.request);
	}

	@Test
	void numberNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[1]", null).number(111.1).match(this.request));
	}

	@Test
	void booleanCase() throws Exception {
		new XpathRequestMatchers("/foo/bar[2]", null).booleanValue(true).match(this.request);
	}

	@Test
	void booleanNoMatch() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				new XpathRequestMatchers("/foo/bar[2]", null).booleanValue(false).match(this.request));
	}

}
