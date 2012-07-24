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


import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.AsyncEvent;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * A test fixture with a {@link StandardServletAsyncWebRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardServletAsyncWebRequestTests {

	private StandardServletAsyncWebRequest asyncRequest;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.request.setAsyncSupported(true);
		this.response = new MockHttpServletResponse();
		this.asyncRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		this.asyncRequest.setTimeout(44*1000L);
	}

	@Test
	public void isAsyncStarted() throws Exception {
		assertFalse(this.asyncRequest.isAsyncStarted());

		this.asyncRequest.startAsync();
		assertTrue(this.asyncRequest.isAsyncStarted());
	}

	@Test
	public void startAsync() throws Exception {
		this.asyncRequest.startAsync();

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();

		assertNotNull(asyncContext);
		assertEquals("Timeout value not set", 44 * 1000, asyncContext.getTimeout());
		assertEquals(1, asyncContext.getListeners().size());
		assertSame(this.asyncRequest, asyncContext.getListeners().get(0));
	}

	@Test
	public void startAsyncMultipleTimes() throws Exception {
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();
		this.asyncRequest.startAsync();	// idempotent

		MockAsyncContext asyncContext = (MockAsyncContext) this.request.getAsyncContext();

		assertNotNull(asyncContext);
		assertEquals(1, asyncContext.getListeners().size());
	}

	@Test
	public void startAsyncNotSupported() throws Exception {
		this.request.setAsyncSupported(false);
		try {
			this.asyncRequest.startAsync();
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("Async support must be enabled"));
		}
	}

	@Test
	public void startAsyncAfterCompleted() throws Exception {
		this.asyncRequest.onComplete(new AsyncEvent(null));
		try {
			this.asyncRequest.startAsync();
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals("Async processing has already completed", ex.getMessage());
		}
	}

	@Test
	public void onTimeoutDefaultBehavior() throws Exception {
		this.asyncRequest.onTimeout(new AsyncEvent(null));
		assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), this.response.getStatus());
	}

	@Test
	public void onTimeoutTimeoutHandler() throws Exception {
		Runnable timeoutHandler = EasyMock.createMock(Runnable.class);
		timeoutHandler.run();
		replay(timeoutHandler);

		this.asyncRequest.setTimeoutHandler(timeoutHandler);
		this.asyncRequest.onTimeout(new AsyncEvent(null));

		verify(timeoutHandler);
	}

}
