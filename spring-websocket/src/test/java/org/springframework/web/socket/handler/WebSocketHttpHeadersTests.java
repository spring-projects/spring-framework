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

package org.springframework.web.socket.handler;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.WebSocketHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.socket.WebSocketHttpHeaders.SEC_WEBSOCKET_EXTENSIONS;

/**
 * Tests for {@link WebSocketHttpHeaders}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class WebSocketHttpHeadersTests {

	private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();


	@Test  // gh-35792
	void constructorCopiesHeaders() {
		headers.setContentType(APPLICATION_JSON);
		assertThat(headers.getContentType()).isEqualTo(APPLICATION_JSON);

		var copy = new WebSocketHttpHeaders(headers);
		assertThat(copy.getContentType()).isEqualTo(APPLICATION_JSON);

		copy.setContentType(TEXT_PLAIN);

		assertThat(copy.getContentType()).isEqualTo(TEXT_PLAIN);
		// The WebSocketHttpHeaders "copy constructor" creates an WebSocketHttpHeaders
		// instance that mutates the state of the original WebSocketHttpHeaders instance.
		assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN);
	}

	@Test  // gh-35792
	void constructorUnwrapsReadonly() {
		headers.setContentType(APPLICATION_JSON);

		var readOnly = HttpHeaders.readOnlyHttpHeaders(headers);
		assertThat(readOnly.getContentType()).isEqualTo(APPLICATION_JSON);

		var writable = new WebSocketHttpHeaders(readOnly);
		writable.setContentType(TEXT_PLAIN);

		// content-type value is cached by ReadOnlyHttpHeaders
		assertThat(readOnly.getContentType()).isEqualTo(APPLICATION_JSON);
		assertThat(writable.getContentType()).isEqualTo(TEXT_PLAIN);
		assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN);
	}

	@Test  // gh-35792
	void copyOf() {
		headers.setContentType(APPLICATION_JSON);
		headers.set("X-Project", "Spring Framework");

		assertThat(headers.getContentType()).isEqualTo(APPLICATION_JSON);
		assertThat(headers.toSingleValueMap()).containsOnly(
			entry(CONTENT_TYPE, APPLICATION_JSON_VALUE),
			entry("X-Project", "Spring Framework")
		);

		var copy = HttpHeaders.copyOf(headers);

		assertThat(copy.getContentType()).isEqualTo(APPLICATION_JSON);
		assertThat(copy.toSingleValueMap()).containsOnly(
			entry(CONTENT_TYPE, APPLICATION_JSON_VALUE),
			entry("X-Project", "Spring Framework")
		);

		copy.setContentType(TEXT_PLAIN);
		copy.set("X-Project", "Project X");

		assertThat(headers.getContentType()).isEqualTo(APPLICATION_JSON);
		assertThat(headers.toSingleValueMap()).containsOnly(
			entry(CONTENT_TYPE, APPLICATION_JSON_VALUE),
			entry("X-Project", "Spring Framework")
		);
		assertThat(copy.getContentType()).isEqualTo(TEXT_PLAIN);
		assertThat(copy.toSingleValueMap()).containsOnly(
			entry(CONTENT_TYPE, TEXT_PLAIN_VALUE),
			entry("X-Project", "Project X")
		);
	}

	@Test  // gh-35792
	void readOnlyHttpHeaders() {
		headers.setContentType(APPLICATION_JSON);
		headers.add("X-Project", "Spring Framework");

		assertThat(headers.getContentType()).isEqualTo(APPLICATION_JSON);
		assertThat(headers.toSingleValueMap()).containsOnly(
			entry(CONTENT_TYPE, APPLICATION_JSON_VALUE),
			entry("X-Project", "Spring Framework")
		);

		var readOnly = HttpHeaders.readOnlyHttpHeaders(headers);
		assertThat(readOnly.getContentType()).isEqualTo(APPLICATION_JSON);
		assertThat(readOnly.toSingleValueMap()).containsOnly(
			entry(CONTENT_TYPE, APPLICATION_JSON_VALUE),
			entry("X-Project", "Spring Framework")
		);

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> readOnly.setContentType(TEXT_PLAIN));
	}

	@Test
	void parseWebSocketExtensions() {
		var extensions = List.of("x-foo-extension, x-bar-extension", "x-test-extension");
		this.headers.put(SEC_WEBSOCKET_EXTENSIONS, extensions);

		var parsedExtensions = this.headers.getSecWebSocketExtensions();
		assertThat(parsedExtensions).hasSize(3);
	}

	@Test  // gh-35792
	void addAllViaWebSocketHttpHeadersApi() {
		headers.add("green", "grape");

		var otherHeaders = new HttpHeaders();
		otherHeaders.add("yellow", "banana");
		otherHeaders.add("red", "apple");

		headers.addAll(otherHeaders);

		assertThat(headers.toSingleValueMap()).containsOnly(
			entry("green", "grape"),
			entry("yellow", "banana"),
			entry("red", "apple")
		);
	}

	@Test  // gh-35792
	void addAllViaHttpHeadersApi() {
		headers.add("yellow", "banana");
		headers.add("red", "apple");

		var otherHeaders = new HttpHeaders();
		otherHeaders.add("green", "grape");

		otherHeaders.addAll(headers);

		assertThat(otherHeaders.toSingleValueMap()).containsOnly(
			entry("green", "grape"),
			entry("yellow", "banana"),
			entry("red", "apple")
		);
	}

	@Test  // gh-35792
	void putAllFromHttpHeadersViaWebSocketHttpHeadersApi() {
		var otherHeaders = new HttpHeaders();
		otherHeaders.add("yellow", "banana");
		otherHeaders.add("red", "apple");

		headers.putAll(otherHeaders);

		assertThat(headers.toSingleValueMap()).containsOnly(
			entry("yellow", "banana"),
			entry("red", "apple")
		);
	}

	@Test  // gh-35792
	void putAllFromHttpHeadersViaHttpHeadersApi() {
		headers.add("yellow", "banana");
		headers.add("red", "apple");

		var otherHeaders = new HttpHeaders();
		otherHeaders.putAll(headers);

		assertThat(otherHeaders.toSingleValueMap()).containsOnly(
			entry("yellow", "banana"),
			entry("red", "apple")
		);
	}

	@Test  // gh-35792
	void putAllFromMap() {
		headers.putAll(Map.of("yellow", List.of("banana"), "red", List.of("apple")));

		assertThat(headers.toSingleValueMap()).containsOnly(
			entry("yellow", "banana"),
			entry("red", "apple")
		);
	}

	@Test  // gh-35792
	void setAllFromMap() {
		headers.add("yellow", "lemon");
		assertThat(headers.toSingleValueMap()).containsOnly(
			entry("yellow", "lemon")
		);

		headers.setAll(Map.of("yellow", "banana", "red", "apple"));

		assertThat(headers.toSingleValueMap()).containsOnly(
			entry("yellow", "banana"), // not lemon
			entry("red", "apple")
		);
	}

}
