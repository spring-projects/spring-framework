/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request.async;

import java.util.concurrent.Callable;
import javax.servlet.AsyncEvent;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mock.web.test.MockAsyncContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.web.context.request.async.CallableProcessingInterceptor.*;

/**
 * {@link WebAsyncManager} tests where container-triggered timeout/completion
 * events are simulated.
 *
 * @author Rossen Stoyanchev
 */
public class WebAsyncManagerTimeoutTests {

	private static final AsyncEvent ASYNC_EVENT = null;

	private WebAsyncManager asyncManager;

	private StandardServletAsyncWebRequest asyncWebRequest;

	private MockHttpServletRequest servletRequest;

	private MockHttpServletResponse servletResponse;


	@Before
	public void setup() {
		this.servletRequest = new MockHttpServletRequest("GET", "/test");
		this.servletRequest.setAsyncSupported(true);
		this.servletResponse = new MockHttpServletResponse();
		this.asyncWebRequest = new StandardServletAsyncWebRequest(servletRequest, servletResponse);

		AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);

		this.asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);
		this.asyncManager.setTaskExecutor(executor);
		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);
	}


	@Test
	public void startCallableProcessingTimeoutAndComplete() throws Exception {
		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
		given(interceptor.handleTimeout(this.asyncWebRequest, callable)).willReturn(RESULT_NONE);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);
		this.asyncWebRequest.onComplete(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(AsyncRequestTimeoutException.class, this.asyncManager.getConcurrentResult().getClass());

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
		verify(interceptor).afterCompletion(this.asyncWebRequest, callable);
	}

	@Test
	public void startCallableProcessingTimeoutAndResumeThroughCallback() throws Exception {

		StubCallable callable = new StubCallable();
		WebAsyncTask<Object> webAsyncTask = new WebAsyncTask<>(callable);
		webAsyncTask.onTimeout(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return 7;
			}
		});

		this.asyncManager.startCallableProcessing(webAsyncTask);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(7, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());
	}

	@Test
	public void startCallableProcessingTimeoutAndResumeThroughInterceptor() throws Exception {

		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
		given(interceptor.handleTimeout(this.asyncWebRequest, callable)).willReturn(22);

		this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(22, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
	}

	@Test
	public void startCallableProcessingAfterTimeoutException() throws Exception {

		StubCallable callable = new StubCallable();
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
		given(interceptor.handleTimeout(this.asyncWebRequest, callable)).willThrow(exception);

		this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, callable);
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndComplete() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();

		DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
		given(interceptor.handleTimeout(this.asyncWebRequest, deferredResult)).willReturn(true);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);
		this.asyncWebRequest.onComplete(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(AsyncRequestTimeoutException.class, this.asyncManager.getConcurrentResult().getClass());

		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
		verify(interceptor).afterCompletion(this.asyncWebRequest, deferredResult);
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndResumeWithDefaultResult() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>(null, 23);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(23, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndResumeThroughCallback() throws Exception {

		final DeferredResult<Integer> deferredResult = new DeferredResult<>();
		deferredResult.onTimeout(new Runnable() {
			@Override
			public void run() {
				deferredResult.setResult(23);
			}
		});

		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(23, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndResumeThroughInterceptor() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptorAdapter() {
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

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(23, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());
	}

	@Test
	public void startDeferredResultProcessingAfterTimeoutException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		final Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptorAdapter() {
			@Override
			public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> result) throws Exception {
				throw exception;
			}
		};

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());
	}


	private final class StubCallable implements Callable<Object> {
		@Override
		public Object call() throws Exception {
			return 21;
		}
	}

}
