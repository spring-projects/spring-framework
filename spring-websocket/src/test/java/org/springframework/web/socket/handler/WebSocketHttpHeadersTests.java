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

package org.springframework.web.socket.handler;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests for {@link WebSocketHttpHeaders}.
 *
 * @author Rossen Stoyanchev
 */
class WebSocketHttpHeadersTests {

	private WebSocketHttpHeaders headers;

	@BeforeEach
	void setUp() {
		headers = new WebSocketHttpHeaders();
	}

	@Test
	void parseWebSocketExtensions() {
		List<String> extensions = new ArrayList<>();
		extensions.add("x-foo-extension, x-bar-extension");
		extensions.add("x-test-extension");
		this.headers.put(WebSocketHttpHeaders.SEC_WEBSOCKET_EXTENSIONS, extensions);

		List<WebSocketExtension> parsedExtensions = this.headers.getSecWebSocketExtensions();
		assertThat(parsedExtensions).hasSize(3);
	}

}
