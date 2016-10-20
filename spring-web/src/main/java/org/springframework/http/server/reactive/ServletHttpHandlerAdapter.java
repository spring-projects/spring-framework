/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.IOException;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to an {@link HttpServlet} using Servlet Async
 * support and Servlet 3.1 Non-blocking I/O.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@WebServlet(asyncSupported = true)
@SuppressWarnings("serial")
public class ServletHttpHandlerAdapter extends HttpHandlerAdapterSupport
		implements Servlet {

	private static final int DEFAULT_BUFFER_SIZE = 8192;


	// Servlet is based on blocking I/O, hence the usage of non-direct, heap-based buffers
	// (i.e. 'false' as constructor argument)
	private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(false);

	private int bufferSize = DEFAULT_BUFFER_SIZE;


	public ServletHttpHandlerAdapter(HttpHandler httpHandler) {
		super(httpHandler);
	}

	public ServletHttpHandlerAdapter(Map<String, HttpHandler> handlerMap) {
		super(handlerMap);
	}


	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}

	public DataBufferFactory getDataBufferFactory() {
		return this.dataBufferFactory;
	}

	public void setBufferSize(int bufferSize) {
		Assert.isTrue(bufferSize > 0);
		this.bufferSize = bufferSize;
	}

	public int getBufferSize() {
		return this.bufferSize;
	}

	@Override
	public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException {

		ServletServerHttpRequest request = new ServletServerHttpRequest(
				((HttpServletRequest) servletRequest), getDataBufferFactory(), getBufferSize());
		ServletServerHttpResponse response = new ServletServerHttpResponse(
				((HttpServletResponse) servletResponse), getDataBufferFactory(), getBufferSize());

		AsyncContext asyncContext = servletRequest.startAsync();
		asyncContext.addListener(new EventHandlingAsyncListener(request, response));

		HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber(asyncContext);
		getHttpHandler().handle(request, response).subscribe(resultSubscriber);
	}

	// Other Servlet methods...

	@Override
	public void init(ServletConfig config) {
	}

	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public String getServletInfo() {
		return "";
	}

	@Override
	public void destroy() {
	}


	private class HandlerResultSubscriber implements Subscriber<Void> {

		private final AsyncContext asyncContext;

		public HandlerResultSubscriber(AsyncContext asyncContext) {
			this.asyncContext = asyncContext;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// no op
		}

		@Override
		public void onError(Throwable ex) {
			logger.error("Could not complete request", ex);
			HttpServletResponse response = (HttpServletResponse) this.asyncContext.getResponse();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			this.asyncContext.complete();
		}

		@Override
		public void onComplete() {
			logger.debug("Successfully completed request");
			this.asyncContext.complete();
		}
	}


	private static final class EventHandlingAsyncListener implements AsyncListener {

		private final ServletServerHttpRequest request;

		private final ServletServerHttpResponse response;


		public EventHandlingAsyncListener(ServletServerHttpRequest request,
				ServletServerHttpResponse response) {

			this.request = request;
			this.response = response;
		}


		@Override
		public void onTimeout(AsyncEvent event) {
			Throwable ex = event.getThrowable();
			if (ex == null) {
				ex = new IllegalStateException("Async operation timeout.");
			}
			this.request.handleAsyncListenerError(ex);
			this.response.handleAsyncListenerError(ex);
		}

		@Override
		public void onError(AsyncEvent event) {
			this.request.handleAsyncListenerError(event.getThrowable());
			this.response.handleAsyncListenerError(event.getThrowable());
		}

		@Override
		public void onStartAsync(AsyncEvent event) {
			// no op
		}

		@Override
		public void onComplete(AsyncEvent event) {
			this.request.handleAsyncListenerComplete();
			this.response.handleAsyncListenerComplete();
		}
	}

}
