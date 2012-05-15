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
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.web.context.request.async.DeferredResult.DeferredResultHandler;

/**
 * DeferredResult tests.
 *
 * @author Rossen Stoyanchev
 */
public class DeferredResultTests {

	@Test
	public void canHandleTimeout() {
		assertFalse(new DeferredResult().canHandleTimeout());
		assertTrue(new DeferredResult("foo").canHandleTimeout());
	}

	@Test
	public void set() {
		DeferredResultHandler resultHandler = createMock(DeferredResultHandler.class);
		DeferredResult deferredResult = new DeferredResult();
		deferredResult.init(resultHandler);

		resultHandler.handle("foo");
		replay(resultHandler);

		deferredResult.set("foo");

		verify(resultHandler);
	}

	@Test
	public void handleTimeout() {
		DeferredResultHandler resultHandler = createMock(DeferredResultHandler.class);
		DeferredResult deferredResult = new DeferredResult("foo");
		deferredResult.init(resultHandler);

		resultHandler.handle("foo");
		replay(resultHandler);

		deferredResult.handleTimeout();

		verify(resultHandler);
	}

	@Test(expected=IllegalStateException.class)
	public void handleTimeout_timeoutResultNone() {
		new DeferredResult().handleTimeout();
	}

	@Test
	public void setAfterHandleTimeout() {
		DeferredResultHandler resultHandler = createMock(DeferredResultHandler.class);
		DeferredResult deferredResult = new DeferredResult("foo");
		deferredResult.init(resultHandler);

		resultHandler.handle("foo");
		replay(resultHandler);

		deferredResult.handleTimeout();

		verify(resultHandler);

		try {
			deferredResult.set("foo");
			fail("Expected exception");
		}
		catch (StaleAsyncWebRequestException ex) {
			// expected
		}
	}

	@Test
	public void setBeforeHandleTimeout() {
		DeferredResultHandler resultHandler = createMock(DeferredResultHandler.class);
		DeferredResult deferredResult = new DeferredResult("foo");
		deferredResult.init(resultHandler);

		resultHandler.handle("foo");
		replay(resultHandler);

		deferredResult.set("foo");

		verify(resultHandler);

		reset(resultHandler);
		replay(resultHandler);

		deferredResult.handleTimeout();

		verify(resultHandler);
	}

}
