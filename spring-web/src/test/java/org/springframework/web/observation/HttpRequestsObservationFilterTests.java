/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.observation;

import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link HttpRequestsObservationFilter}.
 * @author Brian Clozel
 */
class HttpRequestsObservationFilterTests {

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final HttpRequestsObservationFilter filter = new HttpRequestsObservationFilter(this.observationRegistry);

	private final MockFilterChain mockFilterChain = new MockFilterChain();

	private final MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/resource/test");

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	void filterShouldFillObservationContext() throws Exception {
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);

		HttpRequestsObservationContext context = (HttpRequestsObservationContext) this.request
				.getAttribute(HttpRequestsObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context).isNotNull();
		assertThat(context.getCarrier()).isEqualTo(this.request);
		assertThat(context.getResponse()).isEqualTo(this.response);
		assertThat(context.getPathPattern()).isNull();
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS");
	}

	@Test
	void filterShouldUseThrownException() throws Exception {
		IllegalArgumentException customError = new IllegalArgumentException("custom error");
		this.request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, customError);
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);

		HttpRequestsObservationContext context = (HttpRequestsObservationContext) this.request
				.getAttribute(HttpRequestsObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context.getError()).isEqualTo(customError);
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SERVER_ERROR");
	}

	@Test
	void filterShouldUnwrapServletException() {
		IllegalArgumentException customError = new IllegalArgumentException("custom error");

		assertThatThrownBy(() -> {
			this.filter.doFilter(this.request, this.response, (request, response) -> {
				throw new ServletException(customError);
			});
		}).isInstanceOf(ServletException.class);
		HttpRequestsObservationContext context = (HttpRequestsObservationContext) this.request
				.getAttribute(HttpRequestsObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context.getError()).isEqualTo(customError);
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS");
	}

	private TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatHttpObservation() {
		return TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.server.requests").that();
	}

}
