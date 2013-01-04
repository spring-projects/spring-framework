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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.test.MockHttpServletRequest;

/**
 * Test fixture with an {@link WebAsyncManager} with a mock AsyncWebRequest.
 *
 * @author Rossen Stoyanchev
 */
public class WebAsyncManagerTests {

	private WebAsyncManager asyncManager;

	private AsyncWebRequest asyncWebRequest;

	private MockHttpServletRequest servletRequest;


	@Before
	public void setUp() {
		this.servletRequest = new MockHttpServletRequest();
		this.asyncManager = WebAsyncUtils.getAsyncManager(servletRequest);
		this.asyncManager.setTaskExecutor(new SyncTaskExecutor());

		this.asyncWebRequest = createStrictMock(AsyncWebRequest.class);
		this.asyncWebRequest.addCompletionHandler((Runnable) notNull());
		replay(this.asyncWebRequest);

		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);

		verify(this.asyncWebRequest);
		reset(this.asyncWebRequest);
	}

	@Test
	public void startAsyncProcessingWithoutAsyncWebRequest() throws Exception {
		WebAsyncManager manager = WebAsyncUtils.getAsyncManager(new MockHttpServletRequest());

		try {
			manager.startCallableProcessing(new StubCallable(1));
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals(ex.getMessage(), "AsyncWebRequest must not be null");
		}

		try {
			manager.startDeferredResultProcessing(new DeferredResult<String>());
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals(ex.getMessage(), "AsyncWebRequest must not be null");
		}
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

		int concurrentResult = 21;
		Callable<Object> task = new StubCallable(concurrentResult);

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, task);
		interceptor.preProcess(this.asyncWebRequest, task);
		interceptor.postProcess(this.asyncWebRequest, task, new Integer(concurrentResult));
		replay(interceptor);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(concurrentResult, this.asyncManager.getConcurrentResult());

		verify(interceptor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingCallableException() throws Exception {

		Exception concurrentResult = new Exception();
		Callable<Object> task = new StubCallable(concurrentResult);

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, task);
		interceptor.preProcess(this.asyncWebRequest, task);
		interceptor.postProcess(this.asyncWebRequest, task, concurrentResult);
		replay(interceptor);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(concurrentResult, this.asyncManager.getConcurrentResult());

		verify(interceptor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingBeforeConcurrentHandlingException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, task);
		expectLastCall().andThrow(exception);
		replay(interceptor);

		this.asyncWebRequest.addTimeoutHandler((Runnable) notNull());
		this.asyncWebRequest.addCompletionHandler((Runnable) notNull());
		replay(this.asyncWebRequest);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);

		try {
			this.asyncManager.startCallableProcessing(task);
			fail("Expected Exception");
		}catch(Exception e) {
			assertEquals(exception, e);
		}

		assertFalse(this.asyncManager.hasConcurrentResult());

		verify(this.asyncWebRequest, interceptor);
	}

	@Test
	public void startCallableProcessingPreProcessException() throws Exception {

		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, task);
		interceptor.preProcess(this.asyncWebRequest, task);
		expectLastCall().andThrow(exception);
		replay(interceptor);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());

		verify(interceptor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingPostProcessException() throws Exception {

		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = createStrictMock(CallableProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, task);
		interceptor.preProcess(this.asyncWebRequest, task);
		interceptor.postProcess(this.asyncWebRequest, task, 21);
		expectLastCall().andThrow(exception);
		replay(interceptor);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());

		verify(interceptor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingPostProcessContinueAfterException() throws Exception {

		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor1 = createMock(CallableProcessingInterceptor.class);
		interceptor1.beforeConcurrentHandling(this.asyncWebRequest, task);
		interceptor1.preProcess(this.asyncWebRequest, task);
		interceptor1.postProcess(this.asyncWebRequest, task, 21);
		replay(interceptor1);

		CallableProcessingInterceptor interceptor2 = createMock(CallableProcessingInterceptor.class);
		interceptor2.beforeConcurrentHandling(this.asyncWebRequest, task);
		interceptor2.preProcess(this.asyncWebRequest, task);
		interceptor2.postProcess(this.asyncWebRequest, task, 21);
		expectLastCall().andThrow(exception);
		replay(interceptor2);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptors(interceptor1, interceptor2);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());

		verify(interceptor1);
		verify(interceptor2);
		verify(this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingWithAsyncTask() throws Exception {

		AsyncTaskExecutor executor = createMock(AsyncTaskExecutor.class);
		expect(executor.submit((Runnable) notNull())).andReturn(null);
		replay(executor);

		this.asyncWebRequest.setTimeout(1000L);
		this.asyncWebRequest.addTimeoutHandler(EasyMock.<Runnable>anyObject());
		this.asyncWebRequest.addCompletionHandler(EasyMock.<Runnable>anyObject());
		this.asyncWebRequest.startAsync();
		expect(this.asyncWebRequest.getNativeRequest(HttpServletRequest.class)).andReturn(this.servletRequest).times(0, 1);
		replay(this.asyncWebRequest);

		@SuppressWarnings("unchecked")
		WebAsyncTask<Object> asyncTask = new WebAsyncTask<Object>(1000L, executor, createMock(Callable.class));
		this.asyncManager.startCallableProcessing(asyncTask);

		verify(executor, this.asyncWebRequest);
	}

	@Test
	public void startCallableProcessingNullInput() throws Exception {
		try {
			this.asyncManager.startCallableProcessing((Callable<?>) null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertEquals(ex.getMessage(), "Callable must not be null");
		}
	}

	@Test
	public void startDeferredResultProcessing() throws Exception {

		DeferredResult<String> deferredResult = new DeferredResult<String>(1000L);
		String concurrentResult = "abc";

		DeferredResultProcessingInterceptor interceptor = createStrictMock(DeferredResultProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		interceptor.preProcess(this.asyncWebRequest, deferredResult);
		interceptor.postProcess(asyncWebRequest, deferredResult, concurrentResult);
		replay(interceptor);

		this.asyncWebRequest.setTimeout(1000L);
		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(concurrentResult);

		assertEquals(concurrentResult, this.asyncManager.getConcurrentResult());
		verify(this.asyncWebRequest, interceptor);
	}

	@Test
	public void startDeferredResultProcessingBeforeConcurrentHandlingException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = createStrictMock(DeferredResultProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		expectLastCall().andThrow(exception);
		replay(interceptor);

		this.asyncWebRequest.addTimeoutHandler((Runnable) notNull());
		this.asyncWebRequest.addCompletionHandler((Runnable) notNull());
		replay(this.asyncWebRequest);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);

		try {
			this.asyncManager.startDeferredResultProcessing(deferredResult);
			fail("Expected Exception");
		}
		catch(Exception success) {
			assertEquals(exception, success);
		}

		assertFalse(this.asyncManager.hasConcurrentResult());

		verify(this.asyncWebRequest, interceptor);
	}

	@Test
	public void startDeferredResultProcessingPreProcessException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = createStrictMock(DeferredResultProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		interceptor.preProcess(this.asyncWebRequest, deferredResult);
		expectLastCall().andThrow(exception);
		replay(interceptor);

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(25);

		assertEquals(exception, this.asyncManager.getConcurrentResult());
		verify(this.asyncWebRequest, interceptor);
	}

	@Test
	public void startDeferredResultProcessingPostProcessException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = createStrictMock(DeferredResultProcessingInterceptor.class);
		interceptor.beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		interceptor.preProcess(this.asyncWebRequest, deferredResult);
		interceptor.postProcess(this.asyncWebRequest, deferredResult, 25);
		expectLastCall().andThrow(exception);
		replay(interceptor);

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(25);

		assertEquals(exception, this.asyncManager.getConcurrentResult());
		verify(this.asyncWebRequest, interceptor);
	}

	@Test
	public void startDeferredResultProcessingNullInput() throws Exception {
		try {
			this.asyncManager.startDeferredResultProcessing((DeferredResult<?>) null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertEquals(ex.getMessage(), "DeferredResult must not be null");
		}
	}

	private void setupDefaultAsyncScenario() {
		this.asyncWebRequest.addTimeoutHandler((Runnable) notNull());
		this.asyncWebRequest.addCompletionHandler((Runnable) notNull());
		this.asyncWebRequest.startAsync();
		expect(this.asyncWebRequest.getNativeRequest(HttpServletRequest.class)).andReturn(this.servletRequest).times(0, 1);
		expect(this.asyncWebRequest.isAsyncComplete()).andReturn(false);
		this.asyncWebRequest.dispatch();
		replay(this.asyncWebRequest);
	}


	private final class StubCallable implements Callable<Object> {

		private Object value;

		public StubCallable(Object value) {
			this.value = value;
		}

		@Override
		public Object call() throws Exception {
			if (this.value instanceof Exception) {
				throw ((Exception) this.value);
			}
			return this.value;
		}
	}

	@SuppressWarnings("serial")
	private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

		@Override
		public void execute(Runnable task, long startTimeout) {
			task.run();
		}
	}

}
