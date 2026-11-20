/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RequestMethodTests {

	@Test
	void resolveString() {
		String[] methods = new String[]{"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE", "QUERY"};
		for (String httpMethod : methods) {
			RequestMethod requestMethod = RequestMethod.resolve(httpMethod);
			assertThat(requestMethod).isNotNull();
			assertThat(requestMethod.name()).isEqualTo(httpMethod);
		}
		assertThat(RequestMethod.resolve("PROPFIND")).isNull();
	}

	@Test
	void resolveHttpMethod() {
		for (HttpMethod httpMethod : HttpMethod.values()) {
			RequestMethod requestMethod = RequestMethod.resolve(httpMethod);
			assertThat(requestMethod).isNotNull();
			assertThat(requestMethod.name()).isEqualTo(httpMethod.name());
		}
		assertThat(RequestMethod.resolve(HttpMethod.valueOf("PROPFIND"))).isNull();
	}

	@Test
	void asHttpMethod() {
		for (RequestMethod requestMethod : RequestMethod.values()) {
			HttpMethod httpMethod = requestMethod.asHttpMethod();
			assertThat(httpMethod).isNotNull();
			assertThat(httpMethod.name()).isEqualTo(requestMethod.name());
		}
	}
}
