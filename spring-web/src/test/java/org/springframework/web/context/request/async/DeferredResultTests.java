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
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

/**
 * DeferredResult tests.
 *
 * @author Rossen Stoyanchev
 */
public class DeferredResultTests {

	@Test
	public void setResult() {
		DeferredResultHandler handler = createMock(DeferredResultHandler.class);
		handler.handleResult("hello");
		replay(handler);

		DeferredResult<String> result = new DeferredResult<String>();
		result.setResultHandler(handler);

		assertTrue(result.setResult("hello"));

		verify(handler);
	}

	@Test
	public void setResultTwice() {
		DeferredResultHandler handler = createMock(DeferredResultHandler.class);
		handler.handleResult("hello");
		replay(handler);

		DeferredResult<String> result = new DeferredResult<String>();
		result.setResultHandler(handler);

		assertTrue(result.setResult("hello"));
		assertFalse(result.setResult("hi"));

		verify(handler);
	}

	@Test
	public void setResultWithException() {
		DeferredResultHandler handler = createMock(DeferredResultHandler.class);
		handler.handleResult("hello");
		expectLastCall().andThrow(new IllegalStateException());
		replay(handler);

		DeferredResult<String> result = new DeferredResult<String>();
		result.setResultHandler(handler);

		assertFalse(result.setResult("hello"));

		verify(handler);
	}

	@Test
	public void isSetOrExpired() {
		DeferredResultHandler handler = createMock(DeferredResultHandler.class);
		handler.handleResult("hello");
		replay(handler);

		DeferredResult<String> result = new DeferredResult<String>();
		result.setResultHandler(handler);

		assertFalse(result.isSetOrExpired());

		result.setResult("hello");

		assertTrue(result.isSetOrExpired());

		verify(handler);
	}

	@Test
	public void setExpired() {
		DeferredResult<String> result = new DeferredResult<String>();
		assertFalse(result.isSetOrExpired());

		result.expire();
		assertTrue(result.isSetOrExpired());
		assertFalse(result.setResult("hello"));
	}

	@Test
	public void hasTimeout() {
		assertFalse(new DeferredResult<String>().hasTimeoutResult());
		assertTrue(new DeferredResult<String>(null, "timed out").hasTimeoutResult());
	}

	@Test
	public void applyTimeoutResult() {
		DeferredResultHandler handler = createMock(DeferredResultHandler.class);
		handler.handleResult("timed out");
		replay(handler);

		DeferredResult<String> result = new DeferredResult<String>(null, "timed out");
		result.setResultHandler(handler);

		assertTrue(result.applyTimeoutResult());
		assertFalse("Shouldn't be able to set result after timeout", result.setResult("hello"));

		verify(handler);
	}

}
