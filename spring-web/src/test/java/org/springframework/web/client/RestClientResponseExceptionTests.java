/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RestClientResponseExceptionTests {

	@Test // gh-31978
	void caseInsensitiveHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("foo", "bar");
		RestClientResponseException ex = new RestClientResponseException("message", 200, "statusText", headers, null,
				null);

		HttpHeaders responseHeaders = ex.getResponseHeaders();
		assertThat(responseHeaders).isNotNull();
		assertThat(responseHeaders.size()).isOne();
		assertThat(responseHeaders.getFirst("FOO")).isEqualTo("bar");
	}

}
