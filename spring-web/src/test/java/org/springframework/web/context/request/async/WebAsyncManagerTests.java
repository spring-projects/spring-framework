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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;


/**
 * Test fixture with an {@link WebAsyncManager}.
 *
 * @author Rossen Stoyanchev
 */
public class WebAsyncManagerTests {

	private WebAsyncManager asyncManager;

	private MockHttpServletRequest request;

	private StubAsyncWebRequest stubAsyncWebRequest;

	@Before
	public void setUp() {
		this.request = new MockHttpServletRequest();
		this.stubAsyncWebRequest = new StubAsyncWebRequest(this.request, new MockHttpServletResponse());

		this.asyncManager = AsyncWebUtils.getAsyncManager(this.request);
		this.asyncManager.setTaskExecutor(new SyncTaskExecutor());
		this.asyncManager.setAsyncWebRequest(this.stubAsyncWebRequest);
	}

	@Test
	public void getForCurrentRequest() throws Exception {
		assertNotNull(this.asyncManager);
		assertSame(this.asyncManager, AsyncWebUtils.getAsyncManager(this.request));
		assertSame(this.asyncManager, this.request.getAttribute(AsyncWebUtils.WEB_ASYNC_MANAGER_ATTRIBUTE));
	}

	@Test
	public void isConcurrentHandlingStarted() {
		assertFalse(this.asyncManager.isConcurrentHandlingStarted());

		this.stubAsyncWebRequest.startAsync();
		assertTrue(this.asyncManager.isConcurrentHandlingStarted());
	}

	@Test(expected=IllegalArgumentException.class)
	public void setAsyncWebRequestAfterAsyncStarted() {
		this.stubAsyncWebRequest.startAsync();
		this.asyncManager.setAsyncWebRequest(null);
	}

	@Test
	public void startCallableChainProcessing() throws Exception {
		this.asyncManager.startCallableProcessing(new Callable<Object>() {
			public Object call() throws Exception {
				return 1;
			}
		});

		assertTrue(this.asyncManager.isConcurrentHandlingStarted());
		assertTrue(this.stubAsyncWebRequest.isDispatched());
	}

	@Test
	public void startCallableChainProcessingStaleRequest() {
		this.stubAsyncWebRequest.setAsyncComplete(true);
		this.asyncManager.startCallableProcessing(new Callable<Object>() {
			public Object call() throws Exception {
				return 1;
			}
		});

		assertFalse(this.stubAsyncWebRequest.isDispatched());
	}

	@Test
	public void startCallableChainProcessingCallableRequired() {
		try {
			this.asyncManager.startCallableProcessing(null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertEquals(ex.getMessage(), "Callable is required");
		}
	}

	@Test
	public void startCallableChainProcessingAsyncWebRequestRequired() {
		this.request.removeAttribute(AsyncWebUtils.WEB_ASYNC_MANAGER_ATTRIBUTE);
		this.asyncManager = AsyncWebUtils.getAsyncManager(this.request);
		try {
			this.asyncManager.startCallableProcessing(new Callable<Object>() {
				public Object call() throws Exception {
					return null;
				}
			});
			fail("Expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals(ex.getMessage(), "AsyncWebRequest was not set");
		}
	}

	@Test
	public void startDeferredResultProcessing() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		assertTrue(this.asyncManager.isConcurrentHandlingStarted());

		deferredResult.setResult(25);
		assertEquals(25, this.asyncManager.getConcurrentResult());
	}

	@Test
	public void startDeferredResultProcessingStaleRequest() throws Exception {
		DeferredResult<Integer> deferredResult = new DeferredResult<Integer>();
		this.asyncManager.startDeferredResultProcessing(deferredResult);

		this.stubAsyncWebRequest.setAsyncComplete(true);
		assertFalse(deferredResult.setResult(1));
	}

	@Test
	public void startDeferredResultProcessingDeferredResultRequired() {
		try {
			this.asyncManager.startDeferredResultProcessing(null);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertThat(ex.getMessage(), containsString("DeferredResult is required"));
		}
	}


	private static class StubAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest {

		private boolean asyncStarted;

		private boolean dispatched;

		private boolean asyncComplete;

		public StubAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
			super(request, response);
		}

		public void setTimeout(Long timeout) { }

		public void setTimeoutHandler(Runnable runnable) { }

		public void startAsync() {
			this.asyncStarted = true;
		}

		public boolean isAsyncStarted() {
			return this.asyncStarted;
		}

		public void dispatch() {
			this.dispatched = true;
		}

		public boolean isDispatched() {
			return dispatched;
		}

		public void setAsyncComplete(boolean asyncComplete) {
			this.asyncComplete = asyncComplete;
		}

		public boolean isAsyncComplete() {
			return this.asyncComplete;
		}

		public void addCompletionHandler(Runnable runnable) {
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
