/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.concurrent.Callable;

import jakarta.servlet.AsyncEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.web.context.request.async.CallableProcessingInterceptor.RESULT_NONE;

/**
 * {@link WebAsyncManager} tests where container-triggered error/completion
 * events are simulated.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public class WebAsyncManagerErrorTests {

	private WebAsyncManager asyncManager;

	private StandardServletAsyncWebRequest asyncWebRequest;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;


	@BeforeEach
	public void setup() {
		this.servletRequest = new MockHttpServletRequest("GET", "/test");
		this.servletRequest.setAsyncSupported(true);
		this.servletResponse = new MockHttpServletResponse();
		this.asyncWebRequest = new StandardServletAsyncWebRequest(servletRequest, servletResponse);

		AsyncTaskExecutor executor = mock();

		this.asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);
		this.asyncManager.setTaskExecutor(executor);
		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);
	}


	@Test
	public void startCallableProcessingErrorAndComplete() throws Exception {
		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = mock();
		Exception e = new Exception();
		given(interceptor.handleError(this.asyncWebRequest, callable, e)).willReturn(RESULT_NONE);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);
		this.asyncWebRequest.onComplete(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
		verify(interceptor).afterCompletion(this.asyncWebRequest, callable);
	}

	@Test
	public void startCallableProcessingErrorAndResumeThroughCallback() throws Exception {

		StubCallable callable = new StubCallable();
		WebAsyncTask<Object> webAsyncTask = new WebAsyncTask<>(callable);
		webAsyncTask.onError(() -> 7);

		this.asyncManager.startCallableProcessing(webAsyncTask);

		Exception e = new Exception();
		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(7);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	public void startCallableProcessingErrorAndResumeThroughInterceptor() throws Exception {

		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = mock();
		Exception e = new Exception();
		given(interceptor.handleError(this.asyncWebRequest, callable, e)).willReturn(22);

		this.asyncManager.registerCallableInterceptor("errorInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(22);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
	}

	@Test
	public void startCallableProcessingAfterException() throws Exception {

		StubCallable callable = new StubCallable();
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock();
		Exception e = new Exception();
		given(interceptor.handleError(this.asyncWebRequest, callable, e)).willThrow(exception);

		this.asyncManager.registerCallableInterceptor("errorInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
	}

	@Test
	public void startDeferredResultProcessingErrorAndComplete() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();

		DeferredResultProcessingInterceptor interceptor = mock();
		Exception e = new Exception();
		given(interceptor.handleError(this.asyncWebRequest, deferredResult, e)).willReturn(true);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);
		this.asyncWebRequest.onComplete(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
		verify(interceptor).afterCompletion(this.asyncWebRequest, deferredResult);
	}

	@Test
	public void startDeferredResultProcessingErrorAndResumeWithDefaultResult() throws Exception {

		Exception e = new Exception();
		DeferredResult<Throwable> deferredResult = new DeferredResult<>(null, e);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	public void startDeferredResultProcessingErrorAndResumeThroughCallback() throws Exception {

		final DeferredResult<Throwable> deferredResult = new DeferredResult<>();
		deferredResult.onError(t -> deferredResult.setResult(t));

		this.asyncManager.startDeferredResultProcessing(deferredResult);

		Exception e = new Exception();
		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	public void startDeferredResultProcessingErrorAndResumeThroughInterceptor() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
			@Override
			public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> result, Throwable t)
					throws Exception {
				result.setErrorResult(t);
				return true;
			}
		};

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		Exception e = new Exception();
		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	public void startDeferredResultProcessingAfterException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		final Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
			@Override
			public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> result, Throwable t)
					throws Exception {
				throw exception;
			}
		};

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		Exception e = new Exception();
		AsyncEvent event = new AsyncEvent(new MockAsyncContext(this.servletRequest, this.servletResponse), e);
		this.asyncWebRequest.onError(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(e);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}


	private final class StubCallable implements Callable<Object> {
		@Override
		public Object call() throws Exception {
			return 21;
		}
	}

}
