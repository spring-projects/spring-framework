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

package org.springframework.web.socket;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link WebSocketExtension}.
 *
 * @author Brian Clozel
 */
class WebSocketExtensionTests {

	@Test
	void parseHeaderSingle() {
		List<WebSocketExtension> extensions =
				WebSocketExtension.parseExtensions("x-test-extension ; foo=bar ; bar=baz");

		assertThat(extensions).hasSize(1);
		WebSocketExtension extension = extensions.get(0);

		assertThat(extension.getName()).isEqualTo("x-test-extension");
		assertThat(extension.getParameters()).hasSize(2);
		assertThat(extension.getParameters().get("foo")).isEqualTo("bar");
		assertThat(extension.getParameters().get("bar")).isEqualTo("baz");
	}

	@Test
	void parseHeaderMultiple() {
		List<WebSocketExtension> extensions =
				WebSocketExtension.parseExtensions("x-foo-extension, x-bar-extension");

		assertThat(extensions.stream().map(WebSocketExtension::getName))
				.containsExactly("x-foo-extension", "x-bar-extension");
	}

}
