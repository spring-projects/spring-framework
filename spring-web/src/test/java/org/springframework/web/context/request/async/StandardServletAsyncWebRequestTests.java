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


import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * A test fixture with a {@link StandardServletAsyncWebRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class StandardServletAsyncWebRequestTests {

	private StandardServletAsyncWebRequest asyncRequest;

	private HttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setup() {
		this.request = EasyMock.createMock(HttpServletRequest.class);
		this.response = new MockHttpServletResponse();
		this.asyncRequest = new StandardServletAsyncWebRequest(this.request, this.response);
		this.asyncRequest.setTimeout(60*1000L);
	}

	@Test
	public void isAsyncStarted() throws Exception {
		replay(this.request);
		assertEquals("Should be \"false\" before startAsync()", false, this.asyncRequest.isAsyncStarted());
		verify(this.request);

		startAsync();

		reset(this.request);
		expect(this.request.isAsyncStarted()).andReturn(true);
		replay(this.request);

		assertTrue("Should be \"true\" true startAsync()", this.asyncRequest.isAsyncStarted());
		verify(this.request);

		this.asyncRequest.onComplete(new AsyncEvent(null));

		assertFalse("Should be \"false\" after complete()", this.asyncRequest.isAsyncStarted());
	}

	@Test
	public void startAsync() throws Exception {
		AsyncContext asyncContext = EasyMock.createMock(AsyncContext.class);

		reset(this.request);
		expect(this.request.isAsyncSupported()).andReturn(true);
		expect(this.request.startAsync(this.request, this.response)).andStubReturn(asyncContext);
		replay(this.request);

		asyncContext.addListener(this.asyncRequest);
		asyncContext.setTimeout(60*1000);
		replay(asyncContext);

		this.asyncRequest.startAsync();

		verify(this.request);
	}

	@Test
	public void startAsync_notSupported() throws Exception {
		expect(this.request.isAsyncSupported()).andReturn(false);
		replay(this.request);
		try {
			this.asyncRequest.startAsync();
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
			assertThat(ex.getMessage(), containsString("Async support must be enabled"));
		}
	}

	@Test
	public void startAsync_alreadyStarted() throws Exception {
		startAsync();

		reset(this.request);

		expect(this.request.isAsyncSupported()).andReturn(true);
		expect(this.request.isAsyncStarted()).andReturn(true);
		replay(this.request);

		try {
			this.asyncRequest.startAsync();
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals("Async processing already started", ex.getMessage());
		}

		verify(this.request);
	}

	@Test
	public void startAsync_stale() throws Exception {
		expect(this.request.isAsyncSupported()).andReturn(true);
		replay(this.request);
		this.asyncRequest.onComplete(new AsyncEvent(null));
		try {
			this.asyncRequest.startAsync();
			fail("expected exception");
		}
		catch (IllegalStateException ex) {
			assertEquals("Cannot use async request after completion", ex.getMessage());
		}
	}

	@Test
	public void complete_stale() throws Exception {
		this.asyncRequest.onComplete(new AsyncEvent(null));
		this.asyncRequest.complete();

		assertFalse(this.asyncRequest.isAsyncStarted());
		assertTrue(this.asyncRequest.isAsyncCompleted());
	}

	@Test
	public void sendError() throws Exception {
		this.asyncRequest.sendError(HttpStatus.INTERNAL_SERVER_ERROR, "error");
		assertEquals(500, this.response.getStatus());
	}

	@Test
	public void sendError_stale() throws Exception {
		this.asyncRequest.onComplete(new AsyncEvent(null));
		this.asyncRequest.sendError(HttpStatus.INTERNAL_SERVER_ERROR, "error");
		assertEquals(200, this.response.getStatus());
	}

}
