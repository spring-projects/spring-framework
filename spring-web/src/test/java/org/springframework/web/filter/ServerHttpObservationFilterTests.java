/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.Assert;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockFilterChain;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ServerHttpObservationFilter}.
 *
 * @author Brian Clozel
 */
class ServerHttpObservationFilterTests {

	private final TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	private final MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.GET.name(), "/resource/test");

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private MockFilterChain mockFilterChain = new MockFilterChain();

	private ServerHttpObservationFilter filter = new ServerHttpObservationFilter(this.observationRegistry);


	@Test
	void filterShouldProcessAsyncDispatch() {
		assertThat(this.filter.shouldNotFilterAsyncDispatch()).isFalse();
	}

	@Test
	void filterShouldFillObservationContext() throws Exception {
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);

		ServerRequestObservationContext context = (ServerRequestObservationContext) this.request
				.getAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context).isNotNull();
		assertThat(context.getCarrier()).isEqualTo(this.request);
		assertThat(context.getResponse()).isEqualTo(this.response);
		assertThat(context.getPathPattern()).isNull();
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS").hasBeenStopped();
	}

	@Test
	void filterShouldOpenScope() throws Exception {
		this.mockFilterChain = new MockFilterChain(new ScopeCheckingServlet(this.observationRegistry));
		filter.doFilter(this.request, this.response, this.mockFilterChain);
	}

	@Test
	void filterShouldAcceptNoOpObservationContext() throws Exception {
		this.filter = new ServerHttpObservationFilter(ObservationRegistry.NOOP);
		filter.doFilter(this.request, this.response, this.mockFilterChain);

		ServerRequestObservationContext context = (ServerRequestObservationContext) this.request
				.getAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context).isNull();
	}

	@Test
	void filterShouldUseThrownException() throws Exception {
		IllegalArgumentException customError = new IllegalArgumentException("custom error");
		this.request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, customError);
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);

		ServerRequestObservationContext context = (ServerRequestObservationContext) this.request
				.getAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context.getError()).isEqualTo(customError);
		assertThatHttpObservation().hasLowCardinalityKeyValue("exception", "IllegalArgumentException");
	}

	@Test
	void filterShouldUnwrapServletException() {
		IllegalArgumentException customError = new IllegalArgumentException("custom error");

		assertThatThrownBy(() ->
				this.filter.doFilter(this.request, this.response, (request, response) -> {
			throw new ServletException(customError);
		})).isInstanceOf(ServletException.class);
		ServerRequestObservationContext context = (ServerRequestObservationContext) this.request
				.getAttribute(ServerHttpObservationFilter.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
		assertThat(context.getError()).isEqualTo(customError);
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SERVER_ERROR");
	}

	@Test
	void filterShouldSetDefaultErrorStatusForBubblingExceptions() {
		assertThatThrownBy(() ->
				this.filter.doFilter(this.request, this.response, (request, response) -> {
			throw new ServletException(new IllegalArgumentException("custom error"));
		})).isInstanceOf(ServletException.class);
		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SERVER_ERROR")
				.hasLowCardinalityKeyValue("status", "500");
	}

	@Test
	void customFilterShouldCallScopeOpened() throws Exception {
		this.filter = new CustomObservationFilter(this.observationRegistry);
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);

		assertThat(this.response.getHeader("X-Trace-Id")).isEqualTo("badc0ff33");
	}

	@Test
	void shouldCloseObservationAfterAsyncCompletion() throws Exception {
		this.request.setAsyncSupported(true);
		this.request.startAsync();
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);
		this.request.getAsyncContext().complete();

		assertThatHttpObservation().hasLowCardinalityKeyValue("outcome", "SUCCESS").hasBeenStopped();
	}

	@Test
	void shouldCloseObservationAfterAsyncError() throws Exception {
		this.request.setAsyncSupported(true);
		this.request.startAsync();
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);
		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();
		for (AsyncListener listener : asyncContext.getListeners()) {
			listener.onError(new AsyncEvent(this.request.getAsyncContext(), new IllegalStateException("test error")));
		}
		asyncContext.complete();
		assertThatHttpObservation().hasLowCardinalityKeyValue("exception", "IllegalStateException").hasBeenStopped();
	}

	@Test
	void shouldNotCloseObservationDuringAsyncDispatch() throws Exception {
		this.mockFilterChain = new MockFilterChain(new ScopeCheckingServlet(this.observationRegistry));
		this.request.setDispatcherType(DispatcherType.ASYNC);
		this.filter.doFilter(this.request, this.response, this.mockFilterChain);
		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.server.requests")
				.that().isNotStopped();
	}

	private TestObservationRegistryAssert.TestObservationRegistryAssertReturningObservationContextAssert assertThatHttpObservation() {
		TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasNumberOfObservationsWithNameEqualTo("http.server.requests", 1);

		return TestObservationRegistryAssert.assertThat(this.observationRegistry)
				.hasObservationWithNameEqualTo("http.server.requests")
				.that()
				.hasBeenStopped();
	}

	@SuppressWarnings("serial")
	static class ScopeCheckingServlet extends HttpServlet {

		private final ObservationRegistry observationRegistry;

		public ScopeCheckingServlet(ObservationRegistry observationRegistry) {
			this.observationRegistry = observationRegistry;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			assertThat(this.observationRegistry.getCurrentObservation()).isNotNull();
		}
	}

	static class CustomObservationFilter extends ServerHttpObservationFilter {

		public CustomObservationFilter(ObservationRegistry observationRegistry) {
			super(observationRegistry);
		}

		@Override
		protected void onScopeOpened(Observation.Scope scope, HttpServletRequest request, HttpServletResponse response) {
			Assert.notNull(scope, "scope must not be null");
			Assert.notNull(request, "request must not be null");
			Assert.notNull(response, "response must not be null");
			response.setHeader("X-Trace-Id", "badc0ff33");
		}
	}

}
