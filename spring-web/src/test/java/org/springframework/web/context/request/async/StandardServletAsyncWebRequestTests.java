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

package org.springframework.web.context.request.async;

import java.util.function.Consumer;

import jakarta.servlet.AsyncEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link StandardServletAsyncWebRequest}.
 *
 * @author Rossen Stoyanchev
 */
class StandardServletAsyncWebRequestTests {

	private StandardServletAsyncWebRequest asyncRequest;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();

		this.asyncRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		this.asyncRequest.setTimeout(44 * 1000L);
	}


	@Nested
	class StartAsync {

		@Test
		void isAsyncStarted() {
			assertThat(asyncRequest.isAsyncStarted()).isFalse();

			asyncRequest.startAsync();

			assertThat(asyncRequest.isAsyncStarted()).isTrue();
		}

		@Test
		void startAsync() {
			asyncRequest.startAsync();

			MockAsyncContext context = (MockAsyncContext) request.getAsyncContext();
			assertThat(context).isNotNull();
			assertThat(context.getTimeout()).as("Timeout value not set").isEqualTo((44 * 1000));
			assertThat(context.getListeners()).containsExactly(asyncRequest);
		}

		@Test
		void startAsyncMultipleTimes() {
			asyncRequest.startAsync();
			asyncRequest.startAsync();
			asyncRequest.startAsync();
			asyncRequest.startAsync();

			MockAsyncContext context = (MockAsyncContext) request.getAsyncContext();
			assertThat(context).isNotNull();
			assertThat(context.getListeners()).hasSize(1);
		}

		@Test
		void startAsyncNotSupported() {
			request.setAsyncSupported(false);
			assertThatIllegalStateException()
					.isThrownBy(asyncRequest::startAsync)
					.withMessageContaining("Async support must be enabled");
		}

		@Test
		void startAsyncAfterCompleted() throws Exception {
			asyncRequest.startAsync();
			asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(request, response)));

			assertThatIllegalStateException()
					.isThrownBy(asyncRequest::startAsync)
					.withMessage("Cannot start async: [COMPLETED]");
		}

		@Test
		void startAsyncAndSetTimeout() {
			asyncRequest.startAsync();
			assertThatIllegalStateException().isThrownBy(() -> asyncRequest.setTimeout(25L));
		}
	}


	@Nested
	class AsyncListenerHandling {

		@Test
		void onTimeoutHandler() throws Exception {
			Runnable handler = mock();
			asyncRequest.addTimeoutHandler(handler);

			asyncRequest.startAsync();
			asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(request, response)));

			verify(handler).run();
		}

		@Test
		void onErrorHandler() throws Exception {
			Exception ex = new Exception();
			Consumer<Throwable> handler = mock();
			asyncRequest.addErrorHandler(handler);

			asyncRequest.startAsync();
			asyncRequest.onError(new AsyncEvent(new MockAsyncContext(request, response), ex));

			verify(handler).accept(ex);
		}

		@Test
		void onCompletionHandler() throws Exception {
			Runnable handler = mock();
			asyncRequest.addCompletionHandler(handler);

			asyncRequest.startAsync();
			asyncRequest.onComplete(new AsyncEvent(request.getAsyncContext()));

			verify(handler).run();
			assertThat(asyncRequest.isAsyncComplete()).isTrue();
		}
	}

}
