/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client.samples.bind;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.http.HttpStatus.EXPECTATION_FAILED;

/**
 * Tests for a {@link Filter}.
 *
 * @author Rob Worsnop
 */
class FilterTests {

	@Test
	void filter() {

		@SuppressWarnings("serial")
		Filter filter = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException {
				res.getWriter().write("It works!");
			}
		};

		RestTestClient client = RestTestClient.bindToRouterFunction(
				request -> Optional.of(req -> ServerResponse.status(EXPECTATION_FAILED).build()))
				.configureServer(builder -> builder.addFilters(filter))
				.build();

		client.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}
