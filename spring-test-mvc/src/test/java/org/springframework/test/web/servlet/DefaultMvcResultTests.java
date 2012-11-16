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
package org.springframework.test.web.servlet;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test fixture for {@link DefaultMvcResult}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultMvcResultTests {

	private static final long DEFAULT_TIMEOUT = 10000L;

	private DefaultMvcResult mvcResult;

	private CountDownLatch countDownLatch;


	@Before
	public void setup() {
		ExtendedMockHttpServletRequest request = new ExtendedMockHttpServletRequest();
		request.setAsyncStarted(true);

		this.countDownLatch = EasyMock.createMock(CountDownLatch.class);

		this.mvcResult = new DefaultMvcResult(request, null);
		this.mvcResult.setAsyncResultLatch(this.countDownLatch);
	}

	@Test
	public void getAsyncResultWithTimeout() throws Exception {
		long timeout = 1234L;

		expect(this.countDownLatch.await(timeout, TimeUnit.MILLISECONDS)).andReturn(true);
		replay(this.countDownLatch);

		this.mvcResult.getAsyncResult(timeout);

		verify(this.countDownLatch);
	}

	@Test
	public void getAsyncResultWithTimeoutNegativeOne() throws Exception {
		expect(this.countDownLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)).andReturn(true);
		replay(this.countDownLatch);

		this.mvcResult.getAsyncResult(-1);

		verify(this.countDownLatch);
	}

	@Test
	public void getAsyncResultWithoutTimeout() throws Exception {
		expect(this.countDownLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)).andReturn(true);
		replay(this.countDownLatch);

		this.mvcResult.getAsyncResult();

		verify(this.countDownLatch);
	}

	@Test
	public void getAsyncResultWithTimeoutZero() throws Exception {
		replay(this.countDownLatch);

		this.mvcResult.getAsyncResult(0);

		verify(this.countDownLatch);
	}

	@Test(expected=IllegalStateException.class)
	public void getAsyncResultAndTimeOut() throws Exception {
		expect(this.countDownLatch.await(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)).andReturn(false);
		replay(this.countDownLatch);

		this.mvcResult.getAsyncResult(-1);
	}


	private static class ExtendedMockHttpServletRequest extends MockHttpServletRequest {

		private boolean asyncStarted;
		private AsyncContext asyncContext;

		public ExtendedMockHttpServletRequest() {
			super();
			this.asyncContext = EasyMock.createMock(AsyncContext.class);
			expect(this.asyncContext.getTimeout()).andReturn(new Long(DEFAULT_TIMEOUT));
			replay(this.asyncContext);
		}

		public void setAsyncStarted(boolean asyncStarted) {
			this.asyncStarted = asyncStarted;
		}

		public boolean isAsyncStarted() {
			return this.asyncStarted;
		}

		public AsyncContext getAsyncContext() {
			return asyncContext;
		}
	}

}
