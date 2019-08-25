/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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


	@BeforeEach
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.response= new MockHttpServletResponse() ;

		this.handler = new Object();
		this.chain = new HandlerExecutionChain(this.handler);

		this.interceptor1 = mock(AsyncHandlerInterceptor.class);
		this.interceptor2 = mock(AsyncHandlerInterceptor.class);
		this.interceptor3 = mock(AsyncHandlerInterceptor.class);

		this.chain.addInterceptor(this.interceptor1);
		this.chain.addInterceptor(this.interceptor2);
		assertThat(this.chain.getInterceptors().length).isEqualTo(2);
		this.chain.addInterceptor(this.interceptor3);
		assertThat(this.chain.getInterceptors().length).isEqualTo(3);
	}


	@Test
	public void successScenario() throws Exception {
		ModelAndView mav = new ModelAndView();

		given(this.interceptor1.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor2.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor3.preHandle(this.request, this.response, this.handler)).willReturn(true);

		this.chain.applyPreHandle(request, response);
		this.chain.applyPostHandle(request, response, mav);
		this.chain.triggerAfterCompletion(this.request, this.response, null);

		verify(this.interceptor1).postHandle(this.request, this.response, this.handler, mav);
		verify(this.interceptor2).postHandle(this.request, this.response, this.handler, mav);
		verify(this.interceptor3).postHandle(this.request, this.response, this.handler, mav);

		verify(this.interceptor3).afterCompletion(this.request, this.response, this.handler, null);
		verify(this.interceptor2).afterCompletion(this.request, this.response, this.handler, null);
		verify(this.interceptor1).afterCompletion(this.request, this.response, this.handler, null);
	}

	@Test
	public void successAsyncScenario() throws Exception {
		given(this.interceptor1.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor2.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor3.preHandle(this.request, this.response, this.handler)).willReturn(true);

		this.chain.applyPreHandle(request, response);
		this.chain.applyAfterConcurrentHandlingStarted(request, response);
		this.chain.triggerAfterCompletion(this.request, this.response, null);

		verify(this.interceptor1).afterConcurrentHandlingStarted(request, response, this.handler);
		verify(this.interceptor2).afterConcurrentHandlingStarted(request, response, this.handler);
		verify(this.interceptor3).afterConcurrentHandlingStarted(request, response, this.handler);
	}

	@Test
	public void earlyExitInPreHandle() throws Exception {
		given(this.interceptor1.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor2.preHandle(this.request, this.response, this.handler)).willReturn(false);

		this.chain.applyPreHandle(request, response);

		verify(this.interceptor1).afterCompletion(this.request, this.response, this.handler, null);
	}

	@Test
	public void exceptionBeforePreHandle() throws Exception {
		this.chain.triggerAfterCompletion(this.request, this.response, null);
		verifyZeroInteractions(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	public void exceptionDuringPreHandle() throws Exception {
		Exception ex = new Exception("");

		given(this.interceptor1.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor2.preHandle(this.request, this.response, this.handler)).willThrow(ex);

		try {
			this.chain.applyPreHandle(request, response);
		}
		catch (Exception actual) {
			assertThat(actual).isSameAs(ex);
		}
		this.chain.triggerAfterCompletion(this.request, this.response, ex);

		verify(this.interceptor1).afterCompletion(this.request, this.response, this.handler, ex);
		verify(this.interceptor3, never()).preHandle(this.request, this.response, this.handler);
	}

	@Test
	public void exceptionAfterPreHandle() throws Exception {
		Exception ex = new Exception("");

		given(this.interceptor1.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor2.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor3.preHandle(this.request, this.response, this.handler)).willReturn(true);

		this.chain.applyPreHandle(request, response);
		this.chain.triggerAfterCompletion(this.request, this.response, ex);

		verify(this.interceptor3).afterCompletion(this.request, this.response, this.handler, ex);
		verify(this.interceptor2).afterCompletion(this.request, this.response, this.handler, ex);
		verify(this.interceptor1).afterCompletion(this.request, this.response, this.handler, ex);
	}

}
