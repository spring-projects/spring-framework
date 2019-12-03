/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.servlet.AsyncEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.test.MockAsyncContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * A test fixture with a {@link StandardServletAsyncWebRequest}.
 * @author Rossen Stoyanchev
 */
public class StandardServletAsyncWebRequestTests {

	private StandardServletAsyncWebRequest asyncRequest;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@BeforeEach
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();
		this.asyncRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		this.asyncRequest.setTimeout(44*1000L);
	}


	@Test
	public void isAsyncStarted() throws Exception {
		assertThat(this.asyncRequest.isAsyncStarted()).isFalse();
		this.asyncRequest.startAsync();
		assertThat(this.asyncRequest.isAsyncStarted()).isTrue();
	}

	@Test
	public void startAsync() throws Exception {
		this.asyncRequest.startAsync();

		MockAsyncContext context = (MockAsyncContext) this.request.getAsyncContext();
		assertThat(context).isNotNull();
		assertThat(context.getTimeout()).as("Timeout value not set").isEqualTo((44 * 1000));
		assertThat(context.getListeners().size()).isEqualTo(1);
		assertThat(context.getListeners().get(0)).isSameAs(this.asyncRequest);
	}

	@Test
	public void startAsyncMultipleTimes() throws Exception {
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();	// idempotent

		MockAsyncContext context = (MockAsyncContext) this.request.getAsyncContext();
		assertThat(context).isNotNull();
		assertThat(context.getListeners().size()).isEqualTo(1);
	}

	@Test
	public void startAsyncNotSupported() throws Exception {
		this.request.setAsyncSupported(false);
		assertThatIllegalStateException().isThrownBy(
				this.asyncRequest::startAsync)
			.withMessageContaining("Async support must be enabled");
	}

	@Test
	public void startAsyncAfterCompleted() throws Exception {
		this.asyncRequest.onComplete(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
		assertThatIllegalStateException().isThrownBy(
				this.asyncRequest::startAsync)
			.withMessage("Async processing has already completed");
	}

	@Test
	public void onTimeoutDefaultBehavior() throws Exception {
		this.asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
		assertThat(this.response.getStatus()).isEqualTo(200);
	}

	@Test
	public void onTimeoutHandler() throws Exception {
		Runnable timeoutHandler = mock(Runnable.class);
		this.asyncRequest.addTimeoutHandler(timeoutHandler);
		this.asyncRequest.onTimeout(new AsyncEvent(new MockAsyncContext(this.request, this.response)));
		verify(timeoutHandler).run();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void onErrorHandler() throws Exception {
		Consumer<Throwable> errorHandler = mock(Consumer.class);
		this.asyncRequest.addErrorHandler(errorHandler);
		Exception e = new Exception();
		this.asyncRequest.onError(new AsyncEvent(new MockAsyncContext(this.request, this.response), e));
		verify(errorHandler).accept(e);
	}

	@Test
	public void setTimeoutDuringConcurrentHandling() {
		this.asyncRequest.startAsync();
		assertThatIllegalStateException().isThrownBy(() ->
				this.asyncRequest.setTimeout(25L));
	}

	@Test
	public void onCompletionHandler() throws Exception {
		Runnable handler = mock(Runnable.class);
		this.asyncRequest.addCompletionHandler(handler);

		this.asyncRequest.startAsync();
		this.asyncRequest.onComplete(new AsyncEvent(this.request.getAsyncContext()));

		verify(handler).run();
		assertThat(this.asyncRequest.isAsyncComplete()).isTrue();
	}

	// SPR-13292

	@SuppressWarnings("unchecked")
	@Test
	public void onErrorHandlerAfterOnErrorEvent() throws Exception {
		Consumer<Throwable> handler = mock(Consumer.class);
		this.asyncRequest.addErrorHandler(handler);

		this.asyncRequest.startAsync();
		Exception e = new Exception();
		this.asyncRequest.onError(new AsyncEvent(this.request.getAsyncContext(), e));

		verify(handler).accept(e);
	}

	@Test
	public void onCompletionHandlerAfterOnCompleteEvent() throws Exception {
		Runnable handler = mock(Runnable.class);
		this.asyncRequest.addCompletionHandler(handler);

		this.asyncRequest.startAsync();
		this.asyncRequest.onComplete(new AsyncEvent(this.request.getAsyncContext()));

		verify(handler).run();
		assertThat(this.asyncRequest.isAsyncComplete()).isTrue();
	}
}
