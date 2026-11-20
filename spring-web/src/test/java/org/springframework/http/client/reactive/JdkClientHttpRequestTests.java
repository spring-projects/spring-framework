/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	@Test  // gh-37422
	void disallowedHeadersWithManyMixedCaseHeadersAllowed() {
		assertThat(disallowedHeaders("Expect,Host,Connection,Upgrade,Content-Length")).isEmpty();
	}

	@Test  // gh-37422
	void disallowedHeadersWithWhitespaceAroundPropertyValue() {
		assertThat(disallowedHeaders(" expect,host "))
				.containsExactlyInAnyOrder("connection", "content-length", "upgrade");
	}

	@Test  // gh-37422
	void disallowedHeadersWithWhitespaceAroundIndividualHeaders() {
		// Consistent with the JDK, whitespace around individual headers is not ignored.
		assertThat(disallowedHeaders("expect, host"))
				.containsExactlyInAnyOrder("connection", "content-length", "host", "upgrade");
		assertThat(disallowedHeaders("expect ,host"))
				.containsExactlyInAnyOrder("connection", "content-length", "expect", "upgrade");
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
