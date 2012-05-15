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

import static org.easymock.EasyMock.*;

import java.util.concurrent.Callable;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 * A test fixture with a {@link StaleAsyncRequestCheckingCallable}.
 *
 * @author Rossen Stoyanchev
 */
public class StaleAsyncRequestCheckingCallableTests {

	private StaleAsyncRequestCheckingCallable callable;

	private AsyncWebRequest asyncWebRequest;

	@Before
	public void setUp() {
		this.asyncWebRequest = EasyMock.createMock(AsyncWebRequest.class);
		this.callable = new StaleAsyncRequestCheckingCallable(asyncWebRequest);
		this.callable.setNextCallable(new Callable<Object>() {
			public Object call() throws Exception {
				return 1;
			}
		});
	}

	@Test
	public void call_notStale() throws Exception {
		expect(this.asyncWebRequest.isAsyncCompleted()).andReturn(false);
		replay(this.asyncWebRequest);

		this.callable.call();

		verify(this.asyncWebRequest);
	}

	@Test(expected=StaleAsyncWebRequestException.class)
	public void call_stale() throws Exception {
		expect(this.asyncWebRequest.isAsyncCompleted()).andReturn(true);
		replay(this.asyncWebRequest);

		try {
			this.callable.call();
		}
		finally {
			verify(this.asyncWebRequest);
		}
	}
}
