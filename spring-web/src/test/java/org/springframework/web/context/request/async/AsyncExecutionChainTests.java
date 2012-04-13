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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;


/**
 * Test fixture with an AsyncExecutionChain.
 *
 * @author Rossen Stoyanchev
 */
public class AsyncExecutionChainTests {

	private AsyncExecutionChain chain;

	private MockHttpServletRequest request;

	private SimpleAsyncWebRequest asyncWebRequest;

	private ResultSavingCallable resultSavingCallable;

	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.asyncWebRequest = new SimpleAsyncWebRequest(this.request, new MockHttpServletResponse());
		this.resultSavingCallable = new ResultSavingCallable();

		this.chain = AsyncExecutionChain.getForCurrentRequest(this.request);
		this.chain.setTaskExecutor(new SyncTaskExecutor());
		this.chain.setAsyncWebRequest(this.asyncWebRequest);
		this.chain.addDelegatingCallable(this.resultSavingCallable);
	}

	@Test
	public void getForCurrentRequest() throws Exception {
		assertNotNull(this.chain);
		assertSame(this.chain, AsyncExecutionChain.getForCurrentRequest(this.request));
		assertSame(this.chain, this.request.getAttribute(AsyncExecutionChain.CALLABLE_CHAIN_ATTRIBUTE));
	}

	@Test
	public void isAsyncStarted() {
		assertFalse(this.chain.isAsyncStarted());

		this.asyncWebRequest.startAsync();
		assertTrue(this.chain.isAsyncStarted());

		this.chain.setAsyncWebRequest(null);
		assertFalse(this.chain.isAsyncStarted());
	}

	@Test
	public void startCallableChainProcessing() throws Exception {
		this.chain.addDelegatingCallable(new IntegerIncrementingCallable());
		this.chain.addDelegatingCallable(new IntegerIncrementingCallable());
		this.chain.setCallable(new Callable<Object>() {
			public Object call() throws Exception {
				return 1;
			}
		});

		this.chain.startCallableChainProcessing();

		assertEquals(3, this.resultSavingCallable.result);
	}

	@Test
	public void startCallableChainProcessing_staleRequest() {
		this.chain.setCallable(new Callable<Object>() {
			public Object call() throws Exception {
				return 1;
			}
		});

		this.asyncWebRequest.startAsync();
		this.asyncWebRequest.complete();
		this.chain.startCallableChainProcessing();
		Exception ex = this.resultSavingCallable.exception;

		assertNotNull(ex);
		assertEquals(StaleAsyncWebRequestException.class, ex.getClass());
	}

	@Test
	public void startCallableChainProcessing_requiredCallable() {
		try {
			this.chain.startCallableChainProcessing();
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("The callable field is required"));
		}
	}

	@Test
	public void startCallableChainProcessing_requiredAsyncWebRequest() {
		this.chain.setAsyncWebRequest(null);
		try {
			this.chain.startCallableChainProcessing();
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("AsyncWebRequest is required"));
		}
	}

	@Test
	public void startDeferredValueProcessing() throws Exception {
		this.chain.addDelegatingCallable(new IntegerIncrementingCallable());
		this.chain.addDelegatingCallable(new IntegerIncrementingCallable());

		DeferredResult deferredValue = new DeferredResult();
		this.chain.startDeferredResultProcessing(deferredValue);

		assertTrue(this.asyncWebRequest.isAsyncStarted());

		deferredValue.set(1);

		assertEquals(3, this.resultSavingCallable.result);
	}

	@Test(expected=StaleAsyncWebRequestException.class)
	public void startDeferredValueProcessing_staleRequest() throws Exception {
		this.asyncWebRequest.startAsync();
		this.asyncWebRequest.complete();

		DeferredResult deferredValue = new DeferredResult();
		this.chain.startDeferredResultProcessing(deferredValue);
		deferredValue.set(1);
	}

	@Test
	public void startDeferredValueProcessing_requiredDeferredValue() {
		try {
			this.chain.startDeferredResultProcessing(null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("A DeferredValue is required"));
		}
	}


	private static class SimpleAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest {

		private boolean asyncStarted;

		private boolean asyncCompleted;

		public SimpleAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
			super(request, response);
		}

		public void startAsync() {
			this.asyncStarted = true;
		}

		public boolean isAsyncStarted() {
			return this.asyncStarted;
		}

		public void setTimeout(Long timeout) { }

		public void complete() {
			this.asyncStarted = false;
			this.asyncCompleted = true;
		}

		public boolean isAsyncCompleted() {
			return this.asyncCompleted;
		}

		public void sendError(HttpStatus status, String message) {
		}
	}

	@SuppressWarnings("serial")
	private static class SyncTaskExecutor extends SimpleAsyncTaskExecutor {

		@Override
		public void execute(Runnable task, long startTimeout) {
			task.run();
		}
	}

	private static class ResultSavingCallable extends AbstractDelegatingCallable {

		Object result;

		Exception exception;

		public Object call() throws Exception {
			try {
				this.result = getNextCallable().call();
			}
			catch (Exception ex) {
				this.exception = ex;
				throw ex;
			}
			return this.result;
		}
	}

	private static class IntegerIncrementingCallable extends AbstractDelegatingCallable {

		public Object call() throws Exception {
			return ((Integer) getNextCallable().call() + 1);
		}
	}

}
