/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.server.reactive;

import javax.servlet.AsyncContext;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.verify;

/**
 * @author Arjen Poutsma
 */
public class AsyncContextSynchronizerTests {

	private AsyncContext asyncContext;

	private ServletAsyncContextSynchronizer synchronizer;

	@Before
	public void setUp() throws Exception {
		asyncContext = mock(AsyncContext.class);
		synchronizer = new ServletAsyncContextSynchronizer(asyncContext);
	}

	@Test
	public void readThenWrite() {
		synchronizer.readComplete();
		synchronizer.writeComplete();

		verify(asyncContext).complete();
	}

	@Test
	public void writeThenRead() {
		synchronizer.writeComplete();
		synchronizer.readComplete();

		verify(asyncContext).complete();
	}
}