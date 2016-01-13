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
package org.springframework.web.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Assist with building an
 * {@link org.springframework.http.server.reactive.HttpHandler HttpHandler} to
 * invoke a target {@link WebHandler} with an optional chain of
 * {@link WebFilter}s and one or more {@link WebExceptionHandler}s.
 *
 * <p>Effective this sets up the following {@code WebHandler} delegation:<br>
 * {@link WebToHttpHandlerAdapter} {@code -->}
 * {@link ExceptionHandlingWebHandler} {@code -->}
 * {@link FilteringWebHandler}
 *
 * @author Rossen Stoyanchev
 */
public class WebToHttpHandlerBuilder {

	private final WebHandler targetHandler;

	private final List<WebFilter> filters = new ArrayList<>();

	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	private WebSessionManager sessionManager;


	private WebToHttpHandlerBuilder(WebHandler targetHandler) {
		Assert.notNull(targetHandler, "'targetHandler' must not be null");
		this.targetHandler = targetHandler;
	}


	public static WebToHttpHandlerBuilder webHandler(WebHandler webHandler) {
		return new WebToHttpHandlerBuilder(webHandler);
	}

	public WebToHttpHandlerBuilder filters(WebFilter... filters) {
		if (!ObjectUtils.isEmpty(filters)) {
			this.filters.addAll(Arrays.asList(filters));
		}
		return this;
	}

	public WebToHttpHandlerBuilder exceptionHandlers(WebExceptionHandler... exceptionHandlers) {
		if (!ObjectUtils.isEmpty(exceptionHandlers)) {
			this.exceptionHandlers.addAll(Arrays.asList(exceptionHandlers));
		}
		return this;
	}

	public WebToHttpHandlerBuilder sessionManager(WebSessionManager sessionManager) {
		this.sessionManager = sessionManager;
		return this;
	}

	public WebToHttpHandlerAdapter build() {
		WebHandler handler = this.targetHandler;
		if (!this.exceptionHandlers.isEmpty()) {
			WebExceptionHandler[] array = new WebExceptionHandler[this.exceptionHandlers.size()];
			handler = new ExceptionHandlingWebHandler(handler,  this.exceptionHandlers.toArray(array));
		}
		if (!this.filters.isEmpty()) {
			WebFilter[] array = new WebFilter[this.filters.size()];
			handler = new FilteringWebHandler(handler, this.filters.toArray(array));
		}
		WebToHttpHandlerAdapter adapter = new WebToHttpHandlerAdapter(handler);
		if (this.sessionManager != null) {
			adapter.setSessionManager(this.sessionManager);
		}
		return adapter;
	}

}
