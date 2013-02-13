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

package org.springframework.web.servlet;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

/**
 * A test fixture with HandlerExecutionChain and mock handler interceptors.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerExecutionChainTests {

	private HandlerExecutionChain chain;

	private Object handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private AsyncHandlerInterceptor interceptor1;

	private AsyncHandlerInterceptor interceptor2;

	private AsyncHandlerInterceptor interceptor3;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.response= new MockHttpServletResponse() ;

		this.handler = new Object();
		this.chain = new HandlerExecutionChain(this.handler);

		this.interceptor1 = createStrictMock(AsyncHandlerInterceptor.class);
		this.interceptor2 = createStrictMock(AsyncHandlerInterceptor.class);
		this.interceptor3 = createStrictMock(AsyncHandlerInterceptor.class);

		this.chain.addInterceptor(this.interceptor1);
		this.chain.addInterceptor(this.interceptor2);
		this.chain.addInterceptor(this.interceptor3);
	}

	@Test
	public void successScenario() throws Exception {
		ModelAndView mav = new ModelAndView();

		expect(this.interceptor1.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor2.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor3.preHandle(this.request, this.response, this.handler)).andReturn(true);

		this.interceptor1.postHandle(this.request, this.response, this.handler, mav);
		this.interceptor2.postHandle(this.request, this.response, this.handler, mav);
		this.interceptor3.postHandle(this.request, this.response, this.handler, mav);

		this.interceptor3.afterCompletion(this.request, this.response, this.handler, null);
		this.interceptor2.afterCompletion(this.request, this.response, this.handler, null);
		this.interceptor1.afterCompletion(this.request, this.response, this.handler, null);

		replay(this.interceptor1, this.interceptor2, this.interceptor3);

		this.chain.applyPreHandle(request, response);
		this.chain.applyPostHandle(request, response, mav);
		this.chain.triggerAfterCompletion(this.request, this.response, null);

		verify(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	public void successAsyncScenario() throws Exception {
		expect(this.interceptor1.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor2.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor3.preHandle(this.request, this.response, this.handler)).andReturn(true);

		this.interceptor1.afterConcurrentHandlingStarted(request, response, this.handler);
		this.interceptor2.afterConcurrentHandlingStarted(request, response, this.handler);
		this.interceptor3.afterConcurrentHandlingStarted(request, response, this.handler);

		replay(this.interceptor1, this.interceptor2, this.interceptor3);

		this.chain.applyPreHandle(request, response);
		this.chain.applyAfterConcurrentHandlingStarted(request, response);
		this.chain.triggerAfterCompletion(this.request, this.response, null);

		verify(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	public void earlyExitInPreHandle() throws Exception {
		expect(this.interceptor1.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor2.preHandle(this.request, this.response, this.handler)).andReturn(false);

		this.interceptor1.afterCompletion(this.request, this.response, this.handler, null);

		replay(this.interceptor1, this.interceptor2, this.interceptor3);

		this.chain.applyPreHandle(request, response);

		verify(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	public void exceptionBeforePreHandle() throws Exception {
		replay(this.interceptor1, this.interceptor2, this.interceptor3);

		this.chain.triggerAfterCompletion(this.request, this.response, null);

		verify(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	public void exceptionDuringPreHandle() throws Exception {
		Exception ex = new Exception("");

		expect(this.interceptor1.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor2.preHandle(this.request, this.response, this.handler)).andThrow(ex);

		this.interceptor1.afterCompletion(this.request, this.response, this.handler, ex);

		replay(this.interceptor1, this.interceptor2, this.interceptor3);

		try {
			this.chain.applyPreHandle(request, response);
		}
		catch (Exception actual) {
			assertSame(ex, actual);
		}
		this.chain.triggerAfterCompletion(this.request, this.response, ex);

		verify(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	public void exceptionAfterPreHandle() throws Exception {
		Exception ex = new Exception("");

		expect(this.interceptor1.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor2.preHandle(this.request, this.response, this.handler)).andReturn(true);
		expect(this.interceptor3.preHandle(this.request, this.response, this.handler)).andReturn(true);

		this.interceptor3.afterCompletion(this.request, this.response, this.handler, ex);
		this.interceptor2.afterCompletion(this.request, this.response, this.handler, ex);
		this.interceptor1.afterCompletion(this.request, this.response, this.handler, ex);

		replay(this.interceptor1, this.interceptor2, this.interceptor3);

		this.chain.applyPreHandle(request, response);
		this.chain.triggerAfterCompletion(this.request, this.response, ex);

		verify(this.interceptor1, this.interceptor2, this.interceptor3);
	}

}
