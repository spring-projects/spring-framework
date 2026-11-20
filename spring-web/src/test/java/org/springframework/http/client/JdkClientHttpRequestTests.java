/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JdkClientHttpRequest}.
 */
class JdkClientHttpRequestTests {

	private final HttpClient client = mock(HttpClient.class);

	@AutoClose("shutdownNow")
	private final ExecutorService executor = Executors.newSingleThreadExecutor();


	@Test
	@SuppressWarnings("unchecked")
	void futureCancelledAfterTimeout() {
		CompletableFuture<HttpResponse<InputStream>> future = new CompletableFuture<>();
		when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(future);

		assertThatThrownBy(() -> createRequest(Duration.ofMillis(10)).executeInternal(new HttpHeaders(), null))
				.isExactlyInstanceOf(HttpTimeoutException.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	void futureCancelled() {
		CompletableFuture<HttpResponse<InputStream>> future = new CompletableFuture<>();
		future.cancel(true);
		when(client.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(future);

		assertThatThrownBy(() -> createRequest(null).executeInternal(new HttpHeaders(), null))
				.isExactlyInstanceOf(IOException.class);
	}

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

	private JdkClientHttpRequest createRequest(Duration timeout) {
		return new JdkClientHttpRequest(client, URI.create("https://abc.com"), HttpMethod.GET, executor, timeout, false);
	}

}
