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

package org.springframework.web.servlet.function.support;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * {@code HandlerAdapter} implementation that supports {@link HandlerFunction}s.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public class HandlerFunctionAdapter implements HandlerAdapter, Ordered {

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Specify the order value for this HandlerAdapter bean.
	 * <p>The default value is {@code Ordered.LOWEST_PRECEDENCE}, meaning non-ordered.
	 * @see org.springframework.core.Ordered#getOrder()
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerFunction;
	}

	@Nullable
	@Override
	public ModelAndView handle(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse,
			Object handler) throws Exception {


		HandlerFunction<?> handlerFunction = (HandlerFunction<?>) handler;

		ServerRequest serverRequest = getServerRequest(servletRequest);
		ServerResponse serverResponse = handlerFunction.handle(serverRequest);

		return serverResponse.writeTo(servletRequest, servletResponse,
				new ServerRequestContext(serverRequest));
	}

	private ServerRequest getServerRequest(HttpServletRequest servletRequest) {
		ServerRequest serverRequest =
				(ServerRequest) servletRequest.getAttribute(RouterFunctions.REQUEST_ATTRIBUTE);
		Assert.state(serverRequest != null, () -> "Required attribute '" +
				RouterFunctions.REQUEST_ATTRIBUTE + "' is missing");
		return serverRequest;
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1L;
	}


	private static class ServerRequestContext implements ServerResponse.Context {

		private final ServerRequest serverRequest;


		public ServerRequestContext(ServerRequest serverRequest) {
			this.serverRequest = serverRequest;
		}

		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return this.serverRequest.messageConverters();
		}
	}
}
