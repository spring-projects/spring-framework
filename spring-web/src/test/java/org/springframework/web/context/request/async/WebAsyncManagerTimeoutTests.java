/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import jakarta.servlet.AsyncEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.testfixture.servlet.MockAsyncContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.web.context.request.async.CallableProcessingInterceptor.RESULT_NONE;

/**
 * {@link WebAsyncManager} tests where container-triggered timeout/completion
 * events are simulated.
 *
 * @author Rossen Stoyanchev
 */
class WebAsyncManagerTimeoutTests {

	private static final AsyncEvent ASYNC_EVENT = null;

	private WebAsyncManager asyncManager;

	private StandardServletAsyncWebRequest asyncWebRequest;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;


	@BeforeEach
	void setup() {
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
	void startCallableProcessingTimeoutAndComplete() throws Exception {
		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = mock();
		given(interceptor.handleTimeout(this.asyncWebRequest, callable)).willReturn(RESULT_NONE);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);
		this.asyncWebRequest.onComplete(ASYNC_EVENT);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult().getClass()).isEqualTo(AsyncRequestTimeoutException.class);

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
		verify(interceptor).afterCompletion(this.asyncWebRequest, callable);
	}

	@Test
	void startCallableProcessingTimeoutAndResumeThroughCallback() throws Exception {

		StubCallable callable = new StubCallable();
		WebAsyncTask<Object> webAsyncTask = new WebAsyncTask<>(callable);
		webAsyncTask.onTimeout(() -> 7);

		this.asyncManager.startCallableProcessing(webAsyncTask);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(7);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	void startCallableProcessingTimeoutAndResumeThroughInterceptor() throws Exception {

		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = mock();
		given(interceptor.handleTimeout(this.asyncWebRequest, callable)).willReturn(22);

		this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(22);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
	}

	@Test
	void startCallableProcessingAfterTimeoutException() throws Exception {

		StubCallable callable = new StubCallable();
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock();
		given(interceptor.handleTimeout(this.asyncWebRequest, callable)).willThrow(exception);

		this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void startCallableProcessingTimeoutAndCheckThreadInterrupted() throws Exception {

		StubCallable callable = new StubCallable();
		Future future = mock();

		AsyncTaskExecutor executor = mock();
		given(executor.submit(any(Runnable.class))).willReturn(future);

		this.asyncManager.setTaskExecutor(executor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();

		verify(future).cancel(true);
		verifyNoMoreInteractions(future);
	}

	@Test
	void startDeferredResultProcessingTimeoutAndComplete() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();

		DeferredResultProcessingInterceptor interceptor = mock();
		given(interceptor.handleTimeout(this.asyncWebRequest, deferredResult)).willReturn(true);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);
		this.asyncWebRequest.onComplete(ASYNC_EVENT);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult().getClass()).isEqualTo(AsyncRequestTimeoutException.class);

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
		verify(interceptor).afterCompletion(this.asyncWebRequest, deferredResult);
	}

	@Test
	void startDeferredResultProcessingTimeoutAndResumeWithDefaultResult() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>(null, 23);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(23);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	void startDeferredResultProcessingTimeoutAndResumeThroughCallback() throws Exception {

		final DeferredResult<Integer> deferredResult = new DeferredResult<>();
		deferredResult.onTimeout(() -> deferredResult.setResult(23));

		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(23);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	void startDeferredResultProcessingTimeoutAndResumeThroughInterceptor() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
			@Override
			public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
				result.setErrorResult(23);
				return true;
			}
		};

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(23);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}

	@Test
	void startDeferredResultProcessingAfterTimeoutException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		final Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptor() {
			@Override
			public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
				throw exception;
			}
		};

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertThat(this.asyncManager.hasConcurrentResult()).isTrue();
		assertThat(this.asyncManager.getConcurrentResult()).isEqualTo(exception);
		assertThat(((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath()).isEqualTo("/test");
	}


	private static final class StubCallable implements Callable<Object> {
		@Override
		public Object call() {
			return 21;
		}
	}

}
