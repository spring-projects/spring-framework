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
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.MockHttpServletRequest;


/**
 * Test fixture with an {@link WebAsyncManager}.
 *
 * @author Rossen Stoyanchev
 */
public class WebAsyncManagerTests {

	private WebAsyncManager asyncManager;

	private AsyncWebRequest asyncWebRequest;

	@Before
	public void setUp() {
		this.asyncManager = WebAsyncUtils.getAsyncManager(new MockHttpServletRequest());
		this.asyncManager.setTaskExecutor(new SyncTaskExecutor());

		this.asyncWebRequest = createStrictMock(AsyncWebRequest.class);
		this.asyncWebRequest.addCompletionHandler((Runnable) notNull());
		replay(this.asyncWebRequest);

		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);

		verify(this.asyncWebRequest);
		reset(this.asyncWebRequest);
	}

	@Test
	public void isConcurrentHandlingStarted() {

		expect(this.asyncWebRequest.isAsyncStarted()).andReturn(false);
		replay(this.asyncWebRequest);

		assertFalse(this.asyncManager.isConcurrentHandlingStarted());

		verify(this.asyncWebRequest);
		reset(this.asyncWebRequest);

		expect(this.asyncWebRequest.isAsyncStarted()).andReturn(true);
		replay(this.asyncWebRequest);

		assertTrue(this.asyncManager.isConcurrentHandlingStarted());

		verify(this.asyncWebRequest);
	}

	@Test(expected=IllegalArgumentException.class)
	public void setAsyncWebRequestAfterAsyncStarted() {
		this.asyncWebRequest.startAsync();
		this.asyncManager.setAsyncWebRequest(null);
	}

	@Test
	public void startCallableProcessing() throws Exception {

		Callable<Object> task = new Callable<Object>() {
			public Object call() throws Exception {
				return 1;
			}
		};

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.preProcess(this.asyncWebRequest, task);
		interceptor.postProcess(this.asyncWebRequest, task, new Integer(1));
		replay(interceptor);

		this.asyncWebRequest.startAsync();
		expect(this.asyncWebRequest.isAsyncComplete()).andReturn(false);
		this.asyncWebRequest.dispatch();
		replay(this.asyncWebRequest);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		verify(interceptor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingAsyncTask() {

		AsyncTaskExecutor executor = createMock(AsyncTaskExecutor.class);
		expect(executor.submit((Runnable) notNull())).andReturn(null);
		replay(executor);

		this.asyncWebRequest.setTimeout(1000L);
		this.asyncWebRequest.startAsync();
		replay(this.asyncWebRequest);

		AsyncTask asyncTask = new AsyncTask(1000L, executor, createMock(Callable.class));
		this.asyncManager.startCallableProcessing(asyncTask);

		verify(executor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingNullCallable() {
		try {
			this.asyncManager.startCallableProcessing((Callable<?>) null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertEquals(ex.getMessage(), "Callable must not be null");
		}
	}

	@Test
	public void startCallableProcessingNullRequest() {
		WebAsyncManager manager = WebAsyncUtils.getAsyncManager(new MockHttpServletRequest());
		try {
			manager.startCallableProcessing(new Callable<Object>() {
				public Object call() throws Exception {
					return 1;
				}
			});
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals(ex.getMessage(), "AsyncWebRequest must not be null");
		}
	}

	@Test
	public void startDeferredResultProcessing() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>(1000L, 10);

		this.asyncWebRequest.setTimeout(1000L);
		this.asyncWebRequest.setTimeoutHandler((Runnable) notNull());
		this.asyncWebRequest.addCompletionHandler((Runnable) notNull());
		this.asyncWebRequest.startAsync();
		replay(this.asyncWebRequest);

		DeferredResultProcessingInterceptor interceptor = createStrictMock(DeferredResultProcessingInterceptor.class);
		interceptor.preProcess(this.asyncWebRequest, deferredResult);
		replay(interceptor);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		verify(this.asyncWebRequest, interceptor);
		reset(this.asyncWebRequest, interceptor);

		this.asyncWebRequest.dispatch();
		replay(this.asyncWebRequest);

		interceptor.postProcess(asyncWebRequest, deferredResult, 25);
		replay(interceptor);

		deferredResult.setResult(25);

		assertEquals(25, this.asyncManager.getConcurrentResult());
		verify(this.asyncWebRequest, interceptor);
	}


	@SuppressWarnings("serial")
	private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

		@Override
		public void execute(Runnable task, long startTimeout) {
			task.run();
		}
	}

}
