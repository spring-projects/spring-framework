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

package org.springframework.test.web.servlet;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;

/**
 * A sub-class of {@code DispatcherServlet} that saves the result in an
 * {@link MvcResult}. The {@code MvcResult} instance is expected to be available
 * as the request attribute {@link MockMvc#MVC_RESULT_ATTRIBUTE}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
@SuppressWarnings("serial")
final class TestDispatcherServlet extends DispatcherServlet {

	/**
	 * Create a new instance with the given web application context.
	 */
	public TestDispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		CountDownLatch latch = registerAsyncInterceptors(request);
		getMvcResult(request).setAsyncResultLatch(latch);

		super.service(request, response);
	}

	private CountDownLatch registerAsyncInterceptors(HttpServletRequest request) {

		final CountDownLatch asyncResultLatch = new CountDownLatch(1);

		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);

		asyncManager.registerCallableInterceptor("mockmvc", new CallableProcessingInterceptor() {
			public void preProcess(NativeWebRequest request, Callable<?> task) throws Exception { }
			public void postProcess(NativeWebRequest request, Callable<?> task, Object value) throws Exception {
				asyncResultLatch.countDown();
			}
		});

		asyncManager.registerDeferredResultInterceptor("mockmvc", new DeferredResultProcessingInterceptor() {
			public void preProcess(NativeWebRequest request, DeferredResult<?> result) throws Exception { }
			public void postProcess(NativeWebRequest request, DeferredResult<?> result, Object value) throws Exception {
				asyncResultLatch.countDown();
			}
			public void afterExpiration(NativeWebRequest request, DeferredResult<?> result) throws Exception { }
		});

		return asyncResultLatch;
	}

	protected DefaultMvcResult getMvcResult(ServletRequest request) {
		return (DefaultMvcResult) request.getAttribute(MockMvc.MVC_RESULT_ATTRIBUTE);
	}

	@Override
	protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		HandlerExecutionChain chain = super.getHandler(request);
		if (chain != null) {
			DefaultMvcResult mvcResult = getMvcResult(request);
			mvcResult.setHandler(chain.getHandler());
			mvcResult.setInterceptors(chain.getInterceptors());
		}
		return chain;
	}

	@Override
	protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		DefaultMvcResult mvcResult = getMvcResult(request);
		mvcResult.setModelAndView(mv);
		super.render(mv, request, response);
	}

	@Override
	protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) throws Exception {

		ModelAndView mav = super.processHandlerException(request, response, handler, ex);

		// We got this far, exception was processed..
		DefaultMvcResult mvcResult = getMvcResult(request);
		mvcResult.setResolvedException(ex);
		mvcResult.setModelAndView(mav);

		return mav;
	}

}
