/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.mock.web.MockAsyncContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

/**
 * A subclass of {@code DispatcherServlet} that saves the result in an
 * {@link MvcResult}. The {@code MvcResult} instance is expected to be available
 * as the request attribute {@link MockMvc#MVC_RESULT_ATTRIBUTE}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 3.2
 */
@SuppressWarnings("serial")
final class TestDispatcherServlet extends DispatcherServlet {

	private static final String KEY = TestDispatcherServlet.class.getName() + ".interceptor";


	/**
	 * Create a new instance with the given web application context.
	 */
	public TestDispatcherServlet(WebApplicationContext webApplicationContext) {
		super(webApplicationContext);
	}


	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		registerAsyncResultInterceptors(request);

		super.service(request, response);

		if (request.getAsyncContext() != null) {
			MockAsyncContext asyncContext;
			if (request.getAsyncContext() instanceof MockAsyncContext mockAsyncContext) {
				asyncContext = mockAsyncContext;
			}
			else {
				MockHttpServletRequest mockRequest = WebUtils.getNativeRequest(request, MockHttpServletRequest.class);
				Assert.notNull(mockRequest, "Expected MockHttpServletRequest");
				asyncContext = (MockAsyncContext) mockRequest.getAsyncContext();
				String requestClassName = request.getClass().getName();
				Assert.notNull(asyncContext, () ->
						"Outer request wrapper " + requestClassName + " has an AsyncContext," +
								"but it is not a MockAsyncContext, while the nested " +
								mockRequest.getClass().getName() + " does not have an AsyncContext at all.");
			}

			CountDownLatch dispatchLatch = new CountDownLatch(1);
			asyncContext.addDispatchHandler(dispatchLatch::countDown);
			getMvcResult(request).setAsyncDispatchLatch(dispatchLatch);
		}
	}

	private void registerAsyncResultInterceptors(HttpServletRequest request) {

		WebAsyncUtils.getAsyncManager(request).registerCallableInterceptor(KEY,
				new CallableProcessingInterceptor() {
					@Override
					public <T> void postProcess(NativeWebRequest r, Callable<T> task, @Nullable Object value) {
						// We got the result, must also wait for the dispatch
						getMvcResult(request).setAsyncResult(value);
					}
				});

		WebAsyncUtils.getAsyncManager(request).registerDeferredResultInterceptor(KEY,
				new DeferredResultProcessingInterceptor() {
					@Override
					public <T> void postProcess(NativeWebRequest r, DeferredResult<T> result, @Nullable Object value) {
						getMvcResult(request).setAsyncResult(value);
					}
				});
	}

	protected DefaultMvcResult getMvcResult(ServletRequest request) {
		return (DefaultMvcResult) request.getAttribute(MockMvc.MVC_RESULT_ATTRIBUTE);
	}

	@Override
	protected @Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
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
	protected @Nullable ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
			@Nullable Object handler, Exception ex) throws Exception {

		ModelAndView mav = super.processHandlerException(request, response, handler, ex);

		// We got this far, exception was processed..
		DefaultMvcResult mvcResult = getMvcResult(request);
		mvcResult.setResolvedException(ex);
		mvcResult.setModelAndView(mav);

		return mav;
	}

}
