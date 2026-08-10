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

package org.springframework.web.util;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
class WhatWgUrlParserTests {

	private static final WhatWgUrlParser.UrlRecord EMPTY_URL_RECORD = new WhatWgUrlParser.UrlRecord();

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

	@Test
	void parseAsciiHost() {
		// Pure ASCII host is lowercased
		testParse("https://EXAMPLE.com/foo", "https", "example.com", null, "/foo", null, null);
		// ASCII "xn--" (ACE) labels are accepted and lowercased (in any case), not validated or rejected.
		// See https://url.spec.whatwg.org/#concept-domain-to-ascii and web-platform-tests url cases.
		testParse("https://a.b.c.xn--pokxncvks", "https", "a.b.c.xn--pokxncvks", null, "", null, null);
		testParse("https://a.b.c.XN--pokxncvks", "https", "a.b.c.xn--pokxncvks", null, "", null, null);
		testParse("https://a.b.c.Xn--pokxncvks", "https", "a.b.c.xn--pokxncvks", null, "", null, null);
		// A trailing non-numeric "xn--" label keeps a numeric-looking host as a domain, not an IPv4 address.
		testParse("https://10.0.0.xn--pokxncvks", "https", "10.0.0.xn--pokxncvks", null, "", null, null);
		testParse("https://10.0.0.XN--pokxncvks", "https", "10.0.0.xn--pokxncvks", null, "", null, null);
		testParse("https://10.0.0.xN--pokxncvks", "https", "10.0.0.xn--pokxncvks", null, "", null, null);
		// Leading ACE label is handled too, and an empty "xn--" label is accepted (not a failure)
		testParse("https://XN--pokxncvks.example", "https", "xn--pokxncvks.example", null, "", null, null);
		testParse("https://xn--/", "https", "xn--", null, "/", null, null);
		testParse("file://xn--/p", "file", "xn--", null, "/p", null, null);
	}

	private void testParse(String input, String scheme, @Nullable String host, @Nullable String port, String path, @Nullable String query, @Nullable String fragment) {
		WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse(input, EMPTY_URL_RECORD, null, null);
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
		WhatWgUrlParser.UrlRecord result = WhatWgUrlParser.parse("mailto:user@example.com?subject=foo", EMPTY_URL_RECORD, null, null);


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
