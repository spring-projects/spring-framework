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

import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT;


/**
 * Tests for a {@link Filter}.
 * @author Rob Worsnop
 */
class FilterTests {

	@Test
	void filter() {

		Filter filter = new HttpFilter() {
			@Override
			protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException {
				res.getWriter().write("It works!");
			}
		};

		RestTestClient client = RestTestClient.bindToRouterFunction(
				request -> Optional.of(req -> ServerResponse.status(I_AM_A_TEAPOT).build()))
				.configureServer(builder -> builder.addFilters(filter))
				.build();

		client.get().uri("/")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo("It works!");
	}

}
