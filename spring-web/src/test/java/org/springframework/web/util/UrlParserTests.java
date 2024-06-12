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

package org.springframework.web.util;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class UrlParserTests {

	private static final UrlParser.UrlRecord EMPTY_URL_RECORD = new UrlParser.UrlRecord();

	@Test
	void parse() {
		testParse("https://example.com", "https", "example.com", null, "", null, null);
		testParse("https://example.com/", "https", "example.com", null, "/", null, null);
		testParse("https://example.com/foo", "https", "example.com", null, "/foo", null, null);
		testParse("https://example.com/foo/", "https", "example.com", null, "/foo/", null, null);
		testParse("https://example.com:81/foo", "https", "example.com", "81", "/foo", null, null);
		testParse("/foo", "", null, null, "/foo", null, null);
		testParse("/foo/", "", null, null, "/foo/", null, null);
		testParse("/foo/../bar", "", null, null, "/bar", null, null);
		testParse("/foo/../bar/", "", null, null, "/bar/", null, null);
		testParse("//other.info/foo/bar", "", "other.info", null, "/foo/bar", null, null);
		testParse("//other.info/parent/../foo/bar", "", "other.info", null, "/foo/bar", null, null);
	}

	private void testParse(String input, String scheme, @Nullable String host, @Nullable String port, String path, @Nullable String query, @Nullable String fragment) {
		UrlParser.UrlRecord result = UrlParser.parse(input, EMPTY_URL_RECORD, null, null);
		assertThat(result.scheme()).as("Invalid scheme").isEqualTo(scheme);
		if (host != null) {
			assertThat(result.host()).as("Host is null").isNotNull();
			assertThat(result.host().toString()).as("Invalid host").isEqualTo(host);
		}
		else {
			assertThat(result.host()).as("Host is not null").isNull();
		}
		if (port != null) {
			assertThat(result.port()).as("Port is null").isNotNull();
			assertThat(result.port().toString()).as("Invalid port").isEqualTo(port);
		}
		else {
			assertThat(result.port()).as("Port is not null").isNull();
		}
		assertThat(result.hasOpaquePath()).as("Result has opaque path").isFalse();
		assertThat(result.path().toString()).as("Invalid path").isEqualTo(path);
		assertThat(result.query()).as("Invalid query").isEqualTo(query);
		assertThat(result.fragment()).as("Invalid fragment").isEqualTo(fragment);
	}

	@Test
	void parseOpaque() {
		testParseOpaque("mailto:user@example.com?subject=foo", "user@example.com", "subject=foo");

	}

	void testParseOpaque(String input, String path, @Nullable String query) {
		UrlParser.UrlRecord result = UrlParser.parse("mailto:user@example.com?subject=foo", EMPTY_URL_RECORD, null, null);


		assertThat(result.scheme()).as("Invalid scheme").isEqualTo("mailto");
		assertThat(result.hasOpaquePath()).as("Result has no opaque path").isTrue();
		assertThat(result.path().toString()).as("Invalid path").isEqualTo(path);
		if (query != null) {
			assertThat(result.query()).as("Query is null").isNotNull();
			assertThat(result.query()).as("Invalid query").isEqualTo(query);
		}
		else {
			assertThat(result.query()).as("Query is not null").isNull();
		}
	}
}
