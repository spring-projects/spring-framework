/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RestTestClient.Builder}.
 */
class DefaultRestTestClientBuilderTests {

	@Test
	void mutateOriginalBuilderHasNoSideEffects() {
		RestTestClient.Builder<?> builder = new DefaultRestTestClientBuilder<>().baseUrl("http://localhost");
		RestTestClient client = builder.build();
		builder.defaultHeader("foo", "bar");
		client.mutate().defaultHeaders(headers -> assertThat(headers.containsHeader("foo")).isFalse());
	}

	@Test
	void mutateSameClientTwiceHasNoSideEffects() {
		RestTestClient client = new DefaultRestTestClientBuilder<>().baseUrl("http://localhost").build();
		client.mutate().defaultHeader("foo", "bar").build();
		client.mutate().defaultHeaders(headers -> assertThat(headers.containsHeader("foo")).isFalse());
	}

}
