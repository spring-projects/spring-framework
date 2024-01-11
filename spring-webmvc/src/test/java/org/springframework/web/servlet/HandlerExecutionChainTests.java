/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link HandlerExecutionChain} with mock handler interceptors.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class HandlerExecutionChainTests {

	private Object handler = new Object();

	private AsyncHandlerInterceptor interceptor1 = mock();
	private AsyncHandlerInterceptor interceptor2 = mock();
	private AsyncHandlerInterceptor interceptor3 = mock();

	private HandlerExecutionChain chain = new HandlerExecutionChain(handler, interceptor1, interceptor2, interceptor3);

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse() ;


	@Test
	void successScenario() throws Exception {
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
	void successAsyncScenario() throws Exception {
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
	void earlyExitInPreHandle() throws Exception {
		given(this.interceptor1.preHandle(this.request, this.response, this.handler)).willReturn(true);
		given(this.interceptor2.preHandle(this.request, this.response, this.handler)).willReturn(false);

		this.chain.applyPreHandle(request, response);

		verify(this.interceptor1).afterCompletion(this.request, this.response, this.handler, null);
	}

	@Test
	void exceptionBeforePreHandle() {
		this.chain.triggerAfterCompletion(this.request, this.response, null);
		verifyNoInteractions(this.interceptor1, this.interceptor2, this.interceptor3);
	}

	@Test
	void exceptionDuringPreHandle() throws Exception {
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
	void exceptionAfterPreHandle() throws Exception {
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
