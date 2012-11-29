/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.web.context.request.async.CallableProcessingInterceptor.RESULT_NONE;

import java.util.concurrent.Callable;

import javax.servlet.AsyncEvent;
import javax.servlet.DispatcherType;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.NativeWebRequest;

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
	public void setUp() {
		this.servletRequest = new MockHttpServletRequest("GET", "/test");
		this.servletRequest.setAsyncSupported(true);
		this.servletResponse = new MockHttpServletResponse();
		this.asyncWebRequest = new StandardServletAsyncWebRequest(servletRequest, servletResponse);

		AsyncTaskExecutor executor = createMock(AsyncTaskExecutor.class);
		expect(executor.submit((Runnable) notNull())).andReturn(null);
		replay(executor);

		this.asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);
		this.asyncManager.setTaskExecutor(executor);
		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingTimeoutAndComplete() throws Exception {

		StubCallable callable = new StubCallable();

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, callable);
		expect(interceptor.handleTimeout(this.asyncWebRequest, callable)).andReturn(RESULT_NONE);
		interceptor.afterCompletion(this.asyncWebRequest, callable);
		replay(interceptor);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);
		this.asyncWebRequest.onComplete(ASYNC_EVENT);

		assertFalse(this.asyncManager.hasConcurrentResult());
		assertEquals(DispatcherType.REQUEST, this.servletRequest.getDispatcherType());
		assertEquals(503, this.servletResponse.getStatus());

		verify(interceptor);
	}

	@Test
	public void startCallableProcessingTimeoutAndResumeThroughCallback() throws Exception {

		StubCallable callable = new StubCallable();
		WebAsyncTask<Object> webAsyncTask = new WebAsyncTask<Object>(callable);
		webAsyncTask.onTimeout(new Callable<Object>() {
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

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, callable);
		expect(interceptor.handleTimeout(this.asyncWebRequest, callable)).andReturn(22);
		replay(interceptor);

		this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(22, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());

		verify(interceptor);
	}

	@Test
	public void startCallableProcessingAfterTimeoutException() throws Exception {

		StubCallable callable = new StubCallable();
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, callable);
		expect(interceptor.handleTimeout(this.asyncWebRequest, callable)).andThrow(exception);
		replay(interceptor);

		this.asyncManager.registerCallableInterceptor("timeoutInterceptor", interceptor);
		this.asyncManager.startCallableProcessing(callable);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());

		verify(interceptor);
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndComplete() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();

		DeferredResultProcessingInterceptor interceptor = createStrictMock(DeferredResultProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		interceptor.preProcess(this.asyncWebRequest, deferredResult);
		expect(interceptor.handleTimeout(this.asyncWebRequest, deferredResult)).andReturn(true);
		interceptor.afterCompletion(this.asyncWebRequest, deferredResult);
		replay(interceptor);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		this.asyncWebRequest.onTimeout(ASYNC_EVENT);
		this.asyncWebRequest.onComplete(ASYNC_EVENT);

		assertFalse(this.asyncManager.hasConcurrentResult());
		assertEquals(DispatcherType.REQUEST, this.servletRequest.getDispatcherType());
		assertEquals(503, this.servletResponse.getStatus());

		verify(interceptor);
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndResumeWithDefaultResult() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>(null, 23);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		AsyncEvent event = null;
		this.asyncWebRequest.onTimeout(event);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(23, this.asyncManager.getConcurrentResult());
		assertEquals("/test", ((MockAsyncContext) this.servletRequest.getAsyncContext()).getDispatchedPath());
	}

	@Test
	public void startDeferredResultProcessingTimeoutAndResumeThroughCallback() throws Exception {

		final DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		deferredResult.onTimeout(new Runnable() {
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

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptorAdapter() {
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

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		final Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = new DeferredResultProcessingInterceptorAdapter() {
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
		public Object call() throws Exception {
			return 21;
		}
	}

}
