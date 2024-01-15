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
import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A test fixture with a {@link StandardServletAsyncWebRequest}.
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
		this.asyncRequest.setTimeout(44*1000L);
	}


	@Test
	void isAsyncStarted() {
		assertThat(this.asyncRequest.isAsyncStarted()).isFalse();
		this.asyncRequest.startAsync();
		assertThat(this.asyncRequest.isAsyncStarted()).isTrue();
	}

	@Test
	void startAsync() {
		this.asyncRequest.startAsync();

		MockAsyncContext context = (MockAsyncContext) this.request.getAsyncContext();
		assertThat(context).isNotNull();
		assertThat(context.getTimeout()).as("Timeout value not set").isEqualTo((44 * 1000));
		assertThat(context.getListeners()).containsExactly(this.asyncRequest);
	}

	@Test
	void startAsyncMultipleTimes() {
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();	// idempotent

		MockAsyncContext context = (MockAsyncContext) this.request.getAsyncContext();
		assertThat(context).isNotNull();
		assertThat(context.getListeners()).hasSize(1);
	}

	@Test
	void startAsyncNotSupported() {
		this.request.setAsyncSupported(false);
		assertThatIllegalStateException().isThrownBy(
				this.asyncRequest::startAsync)
			.withMessageContaining("Async support must be enabled");
	}

	@Test
	void startAsyncAfterCompleted() throws Exception {
		this.asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
		assertThatIllegalStateException().isThrownBy(
				this.asyncRequest::startAsync)
			.withMessage("Async processing has already completed");
	}

	@Test
	void onTimeoutDefaultBehavior() throws Exception {
		this.asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	@Test
	void onTimeoutHandler() throws Exception {
		Runnable timeoutHandler = mock();
		this.asyncRequest.addTimeoutHandler(timeoutHandler);
		this.asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
		verify(timeoutHandler).run();
	}

	@Test
	void onErrorHandler() throws Exception {
		Consumer<Throwable> errorHandler = mock();
		this.asyncRequest.addErrorHandler(errorHandler);
		Exception e = new Exception();
		this.asyncRequest.onError(new AsyncEvent(new MockAsyncContext(this.request, this.response), e));
		verify(errorHandler).accept(e);
	}

	@Test
	void setTimeoutDuringConcurrentHandling() {
		this.asyncRequest.startAsync();
		assertThatIllegalStateException().isThrownBy(() ->
				this.asyncRequest.setTimeout(25L));
	}

	@Test
	void onCompletionHandler() throws Exception {
		Runnable handler = mock();
		this.asyncRequest.addCompletionHandler(handler);

		this.asyncRequest.startAsync();
		this.asyncRequest.onComplete(new AsyncEvent(this.request.getAsyncContext()));

		verify(handler).run();
		assertThat(this.asyncRequest.isAsyncComplete()).isTrue();
	}

	// SPR-13292

	@Test
	void onErrorHandlerAfterOnErrorEvent() throws Exception {
		Consumer<Throwable> handler = mock();
		this.asyncRequest.addErrorHandler(handler);

		this.asyncRequest.startAsync();
		Exception e = new Exception();
		this.asyncRequest.onError(new AsyncEvent(this.request.getAsyncContext(), e));

		verify(handler).accept(e);
	}

	@Test
	void onCompletionHandlerAfterOnCompleteEvent() throws Exception {
		Runnable handler = mock();
		this.asyncRequest.addCompletionHandler(handler);

		this.asyncRequest.startAsync();
		this.asyncRequest.onComplete(new AsyncEvent(this.request.getAsyncContext()));

		verify(handler).run();
		assertThat(this.asyncRequest.isAsyncComplete()).isTrue();
	}
}
