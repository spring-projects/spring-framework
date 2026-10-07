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

package org.springframework.http.client.reactive;

import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JdkClientHttpRequest}.
 *
 * @author Sam Brannen
 * @since 7.1
 */
class JdkClientHttpRequestTests {

	@Test
	void disallowedHeadersByDefault() {
		assertThat(disallowedHeaders(null))
				.containsExactlyInAnyOrder("connection", "content-length", "expect", "host", "upgrade");
	}

	@Test
	void disallowedHeadersWithSingleHeaderAllowed() {
		assertThat(disallowedHeaders("expect"))
				.containsExactlyInAnyOrder("connection", "content-length", "host", "upgrade");
	}

	@Test
	void disallowedHeadersWithMultipleHeadersAllowed() {
		assertThat(disallowedHeaders("expect,host"))
				.containsExactlyInAnyOrder("connection", "content-length", "upgrade");
	}

	@Test
	void disallowedHeadersAreCaseInsensitive() {
		// AssertJ's contains() uses equals(), so go through Set.contains() instead.
		assertThat(disallowedHeaders(null).contains("EXPECT")).isTrue();
		assertThat(disallowedHeaders("Expect").contains("expect")).isFalse();
	}

	private static Set<String> disallowedHeaders(@Nullable String allowRestrictedHeaders) {
		String key = "jdk.httpclient.allowRestrictedHeaders";
		String original = System.getProperty(key);
		try {
			if (allowRestrictedHeaders != null) {
				System.setProperty(key, allowRestrictedHeaders);
			}
			else {
				System.clearProperty(key);
			}
			return JdkClientHttpRequest.disallowedHeaders();
		}
		finally {
			if (original != null) {
				System.setProperty(key, original);
			}
			else {
				System.clearProperty(key);
			}
		}
	}

}
