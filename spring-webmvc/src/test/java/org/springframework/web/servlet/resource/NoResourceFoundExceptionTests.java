/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoResourceFoundException}.
 * @author Brian Clozel
 */
class NoResourceFoundExceptionTests {

	@Test
	void messageShouldContainRequestUriAndResourcePath() {
		var noResourceFoundException = new NoResourceFoundException(HttpMethod.GET, "/context/resource", "/resource");
		assertThat(noResourceFoundException.getMessage()).isEqualTo("No static resource /resource for request '/context/resource'.");
	}

	@Test
	void detailShouldContainResourcePath() {
		var noResourceFoundException = new NoResourceFoundException(HttpMethod.GET, "/context/resource", "/resource");
		assertThat(noResourceFoundException.getBody().getDetail()).isEqualTo("No static resource /resource.");
	}

	@Test
	void messageArgumentsShouldContainResourcePath() {
		var noResourceFoundException = new NoResourceFoundException(HttpMethod.GET, "/context/resource", "/resource");
		assertThat(noResourceFoundException.getDetailMessageArguments()).containsExactly("/resource");
	}
}
