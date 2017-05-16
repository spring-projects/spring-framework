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

import org.junit.Test;

import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * DeferredResult tests.
 *
 * @author Rossen Stoyanchev
 */
public class DeferredResultTests {

	@Test
	public void setResult() {
		DeferredResultHandler handler = mock(DeferredResultHandler.class);

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertTrue(result.setResult("hello"));
		verify(handler).handleResult("hello");
	}

	@Test
	public void setResultTwice() {
		DeferredResultHandler handler = mock(DeferredResultHandler.class);

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertTrue(result.setResult("hello"));
		assertFalse(result.setResult("hi"));

		verify(handler).handleResult("hello");
	}

	@Test
	public void isSetOrExpired() {
		DeferredResultHandler handler = mock(DeferredResultHandler.class);

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertFalse(result.isSetOrExpired());

		result.setResult("hello");

		assertTrue(result.isSetOrExpired());

		verify(handler).handleResult("hello");
	}

	@Test
	public void hasResult() {
		DeferredResultHandler handler = mock(DeferredResultHandler.class);

		DeferredResult<String> result = new DeferredResult<>();
		result.setResultHandler(handler);

		assertFalse(result.hasResult());
		assertNull(result.getResult());

		result.setResult("hello");

		assertEquals("hello", result.getResult());
	}

	@Test
	public void onCompletion() throws Exception {
		final StringBuilder sb = new StringBuilder();

		DeferredResult<String> result = new DeferredResult<>();
		result.onCompletion(new Runnable() {
			@Override
			public void run() {
				sb.append("completion event");
			}
		});

		result.getInterceptor().afterCompletion(null, null);

		assertTrue(result.isSetOrExpired());
		assertEquals("completion event", sb.toString());
	}

	@Test
	public void onTimeout() throws Exception {
		final StringBuilder sb = new StringBuilder();

		DeferredResultHandler handler = mock(DeferredResultHandler.class);

		DeferredResult<String> result = new DeferredResult<>(null, "timeout result");
		result.setResultHandler(handler);
		result.onTimeout(new Runnable() {
			@Override
			public void run() {
				sb.append("timeout event");
			}
		});

		result.getInterceptor().handleTimeout(null, null);

		assertEquals("timeout event", sb.toString());
		assertFalse("Should not be able to set result a second time", result.setResult("hello"));
		verify(handler).handleResult("timeout result");
	}

}
