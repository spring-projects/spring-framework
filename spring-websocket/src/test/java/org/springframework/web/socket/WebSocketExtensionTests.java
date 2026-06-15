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
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Test fixture for {@link WebSocketExtension}.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class WebSocketExtensionTests {

	@Test
	void parseHeaderSingle() {
		List<WebSocketExtension> extensions =
				WebSocketExtension.parseExtensions("x-test-extension ; foo=bar ; bar=baz");

		assertThat(extensions).singleElement().satisfies(extension -> {
			assertThat(extension.getName()).isEqualTo("x-test-extension");
			assertThat(extension.getParameters())
					.containsOnly(entry("foo", "bar"), entry("bar", "baz"));
		});
	}

	@Test
	void parseHeaderMultiple() {
		List<WebSocketExtension> extensions =
				WebSocketExtension.parseExtensions("x-foo-extension, x-bar-extension");

		assertThat(extensions).extracting(WebSocketExtension::getName)
				.containsExactly("x-foo-extension", "x-bar-extension");
	}


	@Nested
	class EqualsTests {

		@Test
		void equalToSelf() {
			WebSocketExtension extension = new WebSocketExtension("x-test");
			assertThat(extension).isEqualTo(extension);
		}

		@Test
		void notEqualToNull() {
			WebSocketExtension extension = new WebSocketExtension("x-test");
			assertThat(extension).isNotEqualTo(null);
		}

		@Test
		void notEqualToDifferentType() {
			WebSocketExtension extension = new WebSocketExtension("x-test");
			assertThat(extension).isNotEqualTo("x-test");
		}

		@Test
		void equalWithSameName() {
			assertThat(new WebSocketExtension("x-test"))
					.isEqualTo(new WebSocketExtension("x-test"));
		}

		@Test
		void equalWithSameNameAndParameter() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "bar")))
					.isEqualTo(new WebSocketExtension("x-test", Map.of("foo", "bar")));
		}

		@Test
		void equalWithSameNameAndMultipleParameters() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "1", "bar", "2")))
					.isEqualTo(new WebSocketExtension("x-test", Map.of("foo", "1", "bar", "2")));
		}

		@Test
		void notEqualWithDifferentName() {
			assertThat(new WebSocketExtension("x-foo"))
					.isNotEqualTo(new WebSocketExtension("x-bar"));
		}

		@Test
		void notEqualWithDifferentNameCase() {
			assertThat(new WebSocketExtension("x-test"))
					.isNotEqualTo(new WebSocketExtension("X-TEST"));
		}

		@Test
		void notEqualWithDifferentParameterValue() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "bar")))
					.isNotEqualTo(new WebSocketExtension("x-test", Map.of("foo", "baz")));
		}

		@Test
		void notEqualWithDifferentParameterKey() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "bar")))
					.isNotEqualTo(new WebSocketExtension("x-test", Map.of("baz", "bar")));
		}

		@Test
		void notEqualWithDifferentParameterCount() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "bar")))
					.isNotEqualTo(new WebSocketExtension("x-test", Map.of("foo", "bar", "baz", "qux")));
		}
	}


	@Nested
	class HashCodeTests {

		@Test
		void sameHashCodeForEqualExtensions() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "bar")).hashCode())
					.isEqualTo(new WebSocketExtension("x-test", Map.of("foo", "bar")).hashCode());
		}

		@Test
		void differentHashCodeForDifferentNames() {
			assertThat(new WebSocketExtension("x-foo").hashCode())
					.isNotEqualTo(new WebSocketExtension("x-bar").hashCode());
		}

		@Test
		void differentHashCodeForDifferentParameterValues() {
			assertThat(new WebSocketExtension("x-test", Map.of("foo", "bar")).hashCode())
					.isNotEqualTo(new WebSocketExtension("x-test", Map.of("foo", "baz")).hashCode());
		}
	}

}
