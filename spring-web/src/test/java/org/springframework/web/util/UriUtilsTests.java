/*
 * Copyright 2002-2025 the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Med Belamachi
 */
class UriUtilsTests {

	private static final Charset CHARSET = StandardCharsets.UTF_8;


	@Test
	void encodeScheme() {
		assertThat(UriUtils.encodeScheme("foobar+-.", CHARSET)).as("Invalid encoded result").isEqualTo("foobar+-.");
		assertThat(UriUtils.encodeScheme("foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%20bar");
	}

	@Test
	void encodeUserInfo() {
		assertThat(UriUtils.encodeUserInfo("foobar:", CHARSET)).as("Invalid encoded result").isEqualTo("foobar:");
		assertThat(UriUtils.encodeUserInfo("foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%20bar");
	}

	@Test
	void encodeHost() {
		assertThat(UriUtils.encodeHost("foobar", CHARSET)).as("Invalid encoded result").isEqualTo("foobar");
		assertThat(UriUtils.encodeHost("foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%20bar");
	}

	@Test
	void encodePort() {
		assertThat(UriUtils.encodePort("80", CHARSET)).as("Invalid encoded result").isEqualTo("80");
	}

	@Test
	void encodePath() {
		assertThat(UriUtils.encodePath("/foo/bar", CHARSET)).as("Invalid encoded result").isEqualTo("/foo/bar");
		assertThat(UriUtils.encodePath("/foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("/foo%20bar");
		assertThat(UriUtils.encodePath("/Z\u00fcrich", CHARSET)).as("Invalid encoded result").isEqualTo("/Z%C3%BCrich");
	}

	@Test
	void encodePathSegment() {
		assertThat(UriUtils.encodePathSegment("foobar", CHARSET)).as("Invalid encoded result").isEqualTo("foobar");
		assertThat(UriUtils.encodePathSegment("/foo/bar", CHARSET)).as("Invalid encoded result").isEqualTo("%2Ffoo%2Fbar");
	}

	@Test
	void encodeQuery() {
		assertThat(UriUtils.encodeQuery("foobar", CHARSET)).as("Invalid encoded result").isEqualTo("foobar");
		assertThat(UriUtils.encodeQuery("foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%20bar");
		assertThat(UriUtils.encodeQuery("foobar/+", CHARSET)).as("Invalid encoded result").isEqualTo("foobar/+");
		assertThat(UriUtils.encodeQuery("T\u014dky\u014d", CHARSET)).as("Invalid encoded result").isEqualTo("T%C5%8Dky%C5%8D");
	}

	@Test
	void encodeQueryParam() {
		assertThat(UriUtils.encodeQueryParam("foobar", CHARSET)).as("Invalid encoded result").isEqualTo("foobar");
		assertThat(UriUtils.encodeQueryParam("foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%20bar");
		assertThat(UriUtils.encodeQueryParam("foo&bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%26bar");
	}

	@Test
	void encodeFragment() {
		assertThat(UriUtils.encodeFragment("foobar", CHARSET)).as("Invalid encoded result").isEqualTo("foobar");
		assertThat(UriUtils.encodeFragment("foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("foo%20bar");
		assertThat(UriUtils.encodeFragment("foobar/", CHARSET)).as("Invalid encoded result").isEqualTo("foobar/");
	}

	@Test
	void encode() {
		assertThat(UriUtils.encode("foo", CHARSET)).as("Invalid encoded result").isEqualTo("foo");
		assertThat(UriUtils.encode("https://example.com/foo bar", CHARSET)).as("Invalid encoded result").isEqualTo("https%3A%2F%2Fexample.com%2Ffoo%20bar");
	}

	@Test
	void decode() {
		assertThat(UriUtils.decode("", CHARSET)).as("Invalid encoded URI").isEmpty();
		assertThat(UriUtils.decode("foobar", CHARSET)).as("Invalid encoded URI").isEqualTo("foobar");
		assertThat(UriUtils.decode("foo%20bar", CHARSET)).as("Invalid encoded URI").isEqualTo("foo bar");
		assertThat(UriUtils.decode("foo%2bbar", CHARSET)).as("Invalid encoded URI").isEqualTo("foo+bar");
		assertThat(UriUtils.decode("T%C5%8Dky%C5%8D", CHARSET)).as("Invalid encoded result").isEqualTo("T\u014dky\u014d");
		assertThat(UriUtils.decode("/Z%C3%BCrich", CHARSET)).as("Invalid encoded result").isEqualTo("/Z\u00fcrich");
		assertThat(UriUtils.decode("T\u014dky\u014d", CHARSET)).as("Invalid encoded result").isEqualTo("T\u014dky\u014d");
		assertThat(UriUtils.decode("%20\u2019", CHARSET)).as("Invalid encoded result").isEqualTo(" \u2019");
		assertThat(UriUtils.decode("\u015bp\u0159\u00ec\u0144\u0121", CHARSET)).as("Invalid encoded result").isEqualTo("śpřìńġ");
		assertThat(UriUtils.decode("%20\u015bp\u0159\u00ec\u0144\u0121", CHARSET)).as("Invalid encoded result").isEqualTo(" śpřìńġ");
	}

	@Test
	void decodeInvalidSequence() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriUtils.decode("foo%2", CHARSET));
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriUtils.decode("foo%", CHARSET));
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriUtils.decode("%", CHARSET));
		assertThatIllegalArgumentException().isThrownBy(() ->
				UriUtils.decode("%zz", CHARSET));
	}

	@Test
	void extractFileExtension() {
		assertThat(UriUtils.extractFileExtension("index.html")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/index.html")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html#/a")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html#/path/a")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html#/path/a.do")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html#aaa?bbb")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html#aaa.xml?bbb")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html?param=a")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html?param=/path/a")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html?param=/path/a.do")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html?param=/path/a#/path/a")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/view.html?param=/path/a.do#/path/a.do")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products;q=11/view.html?param=/path/a.do")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products;q=11/view.html;r=22?param=/path/a.do")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products;q=11/view.html;r=22;s=33?param=/path/a.do")).isEqualTo("html");
		assertThat(UriUtils.extractFileExtension("/products/.html")).isEqualTo("html");
	}

}
