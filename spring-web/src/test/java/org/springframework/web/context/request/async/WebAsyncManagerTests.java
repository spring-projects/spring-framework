/*
 * Copyright 2002-2016 the original author or authors.
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
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

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
		this.asyncWebRequest = mock(AsyncWebRequest.class);
		this.asyncManager.setAsyncWebRequest(this.asyncWebRequest);
		verify(this.asyncWebRequest).addCompletionHandler((Runnable) notNull());
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
		given(this.asyncWebRequest.isAsyncStarted()).willReturn(false);

		assertFalse(this.asyncManager.isConcurrentHandlingStarted());

		reset(this.asyncWebRequest);
		given(this.asyncWebRequest.isAsyncStarted()).willReturn(true);

		assertTrue(this.asyncManager.isConcurrentHandlingStarted());
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

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(concurrentResult, this.asyncManager.getConcurrentResult());

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor).preProcess(this.asyncWebRequest, task);
		verify(interceptor).postProcess(this.asyncWebRequest, task, new Integer(concurrentResult));
	}

	@Test
	public void startCallableProcessingCallableException() throws Exception {

		Exception concurrentResult = new Exception();
		Callable<Object> task = new StubCallable(concurrentResult);

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(concurrentResult, this.asyncManager.getConcurrentResult());

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor).preProcess(this.asyncWebRequest, task);
		verify(interceptor).postProcess(this.asyncWebRequest, task, concurrentResult);
	}

	@Test
	public void startCallableProcessingBeforeConcurrentHandlingException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
		willThrow(exception).given(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);

		try {
			this.asyncManager.startCallableProcessing(task);
			fail("Expected Exception");
		}
		catch (Exception ex) {
			assertEquals(exception, ex);
		}

		assertFalse(this.asyncManager.hasConcurrentResult());

		verify(this.asyncWebRequest).addTimeoutHandler((Runnable) notNull());
		verify(this.asyncWebRequest).addCompletionHandler((Runnable) notNull());
	}

	@Test
	public void startCallableProcessingPreProcessException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
		willThrow(exception).given(interceptor).preProcess(this.asyncWebRequest, task);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
	}

	@Test
	public void startCallableProcessingPostProcessException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor = mock(CallableProcessingInterceptor.class);
		willThrow(exception).given(interceptor).postProcess(this.asyncWebRequest, task, 21);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptor("interceptor", interceptor);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());

		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor).preProcess(this.asyncWebRequest, task);
	}

	@Test
	public void startCallableProcessingPostProcessContinueAfterException() throws Exception {
		Callable<Object> task = new StubCallable(21);
		Exception exception = new Exception();

		CallableProcessingInterceptor interceptor1 = mock(CallableProcessingInterceptor.class);
		CallableProcessingInterceptor interceptor2 = mock(CallableProcessingInterceptor.class);
		willThrow(exception).given(interceptor2).postProcess(this.asyncWebRequest, task, 21);

		setupDefaultAsyncScenario();

		this.asyncManager.registerCallableInterceptors(interceptor1, interceptor2);
		this.asyncManager.startCallableProcessing(task);

		assertTrue(this.asyncManager.hasConcurrentResult());
		assertEquals(exception, this.asyncManager.getConcurrentResult());

		verifyDefaultAsyncScenario();
		verify(interceptor1).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor1).preProcess(this.asyncWebRequest, task);
		verify(interceptor1).postProcess(this.asyncWebRequest, task, 21);
		verify(interceptor2).beforeConcurrentHandling(this.asyncWebRequest, task);
		verify(interceptor2).preProcess(this.asyncWebRequest, task);
	}

	@Test
	public void startCallableProcessingWithAsyncTask() throws Exception {
		AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
		given(this.asyncWebRequest.getNativeRequest(HttpServletRequest.class)).willReturn(this.servletRequest);

		@SuppressWarnings("unchecked")
		WebAsyncTask<Object> asyncTask = new WebAsyncTask<>(1000L, executor, mock(Callable.class));
		this.asyncManager.startCallableProcessing(asyncTask);

		verify(executor).submit((Runnable) notNull());
		verify(this.asyncWebRequest).setTimeout(1000L);
		verify(this.asyncWebRequest).addTimeoutHandler(any(Runnable.class));
		verify(this.asyncWebRequest).addCompletionHandler(any(Runnable.class));
		verify(this.asyncWebRequest).startAsync();
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
		DeferredResult<String> deferredResult = new DeferredResult<>(1000L);
		String concurrentResult = "abc";

		DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(concurrentResult);

		assertEquals(concurrentResult, this.asyncManager.getConcurrentResult());
		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
		verify(interceptor).postProcess(asyncWebRequest, deferredResult, concurrentResult);
		verify(this.asyncWebRequest).setTimeout(1000L);
	}

	@Test
	public void startDeferredResultProcessingBeforeConcurrentHandlingException() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
		willThrow(exception).given(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);

		try {
			this.asyncManager.startDeferredResultProcessing(deferredResult);
			fail("Expected Exception");
		}
		catch (Exception success) {
			assertEquals(exception, success);
		}

		assertFalse(this.asyncManager.hasConcurrentResult());

		verify(this.asyncWebRequest).addTimeoutHandler((Runnable) notNull());
		verify(this.asyncWebRequest).addCompletionHandler((Runnable) notNull());
	}

	@Test
	public void startDeferredResultProcessingPreProcessException() throws Exception {

		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
		willThrow(exception).given(interceptor).preProcess(this.asyncWebRequest, deferredResult);

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(25);

		assertEquals(exception, this.asyncManager.getConcurrentResult());
		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
	}

	@Test
	public void startDeferredResultProcessingPostProcessException() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<>();
		Exception exception = new Exception();

		DeferredResultProcessingInterceptor interceptor = mock(DeferredResultProcessingInterceptor.class);
		willThrow(exception).given(interceptor).postProcess(this.asyncWebRequest, deferredResult, 25);;

		setupDefaultAsyncScenario();

		this.asyncManager.registerDeferredResultInterceptor("interceptor", interceptor);
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		deferredResult.setResult(25);

		assertEquals(exception, this.asyncManager.getConcurrentResult());
		verifyDefaultAsyncScenario();
		verify(interceptor).beforeConcurrentHandling(this.asyncWebRequest, deferredResult);
		verify(interceptor).preProcess(this.asyncWebRequest, deferredResult);
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
		given(this.asyncWebRequest.getNativeRequest(HttpServletRequest.class)).willReturn(this.servletRequest);
		given(this.asyncWebRequest.isAsyncComplete()).willReturn(false);
	}

	private void verifyDefaultAsyncScenario() {
		verify(this.asyncWebRequest).addTimeoutHandler((Runnable) notNull());
		verify(this.asyncWebRequest).addCompletionHandler((Runnable) notNull());
		verify(this.asyncWebRequest).startAsync();
		verify(this.asyncWebRequest).dispatch();
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
