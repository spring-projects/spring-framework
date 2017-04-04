/*
 * Copyright 2002-2017 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to an {@link HttpServlet} using Servlet Async
 * support and Servlet 3.1 non-blocking I/O.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@WebServlet(asyncSupported = true)
@SuppressWarnings("serial")
public class ServletHttpHandlerAdapter implements Servlet {

	private static final Log logger = LogFactory.getLog(ServletHttpHandlerAdapter.class);


	private static final int DEFAULT_BUFFER_SIZE = 8192;


	private final HttpHandler httpHandler;

	private int bufferSize = DEFAULT_BUFFER_SIZE;


	// Servlet is based on blocking I/O, hence the usage of non-direct, heap-based buffers
	// (i.e. 'false' as constructor argument)
	private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(false);


	public ServletHttpHandlerAdapter(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "HttpHandler must not be null");
		this.httpHandler = httpHandler;
	}


	/**
	 * Set the size of the input buffer used for reading in bytes.
	 * <p>By default this is set to 8192.
	 */
	public void setBufferSize(int bufferSize) {
		Assert.isTrue(bufferSize > 0, "Buffer size must be larger than zero");
		this.bufferSize = bufferSize;
	}

	/**
	 * Return the configured input buffer size.
	 */
	public int getBufferSize() {
		return this.bufferSize;
	}

	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}

	public DataBufferFactory getDataBufferFactory() {
		return this.dataBufferFactory;
	}


	// The Servlet.service method

	@Override
	public void service(ServletRequest request, ServletResponse response) throws IOException {
		// Start async before Read/WriteListener registration
		AsyncContext asyncContext = request.startAsync();

		ServerHttpRequest httpRequest = createRequest(((HttpServletRequest) request), asyncContext);
		ServerHttpResponse httpResponse = createResponse(((HttpServletResponse) response), asyncContext);

		asyncContext.addListener(TIMEOUT_LISTENER);

		HandlerResultSubscriber subscriber = new HandlerResultSubscriber(asyncContext);
		this.httpHandler.handle(httpRequest, httpResponse).subscribe(subscriber);
	}

	protected ServerHttpRequest createRequest(HttpServletRequest request,
			AsyncContext context) throws IOException {

		return new ServletServerHttpRequest(
				request, context, getDataBufferFactory(), getBufferSize());
	}

	protected ServerHttpResponse createResponse(HttpServletResponse response,
			AsyncContext context) throws IOException {

		return new ServletServerHttpResponse(
				response, context, getDataBufferFactory(), getBufferSize());
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


	private final static AsyncListener TIMEOUT_LISTENER = new AsyncListener() {

		@Override
		public void onTimeout(AsyncEvent event) throws IOException {
			event.getAsyncContext().complete();
		}

		@Override
		public void onError(AsyncEvent event) throws IOException {
			event.getAsyncContext().complete();
		}

		@Override
		public void onStartAsync(AsyncEvent event) throws IOException {
			// no-op
		}

		@Override
		public void onComplete(AsyncEvent event) throws IOException {
			// no-op
		}
	};


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
			runIfAsyncNotComplete(() -> {
				logger.error("Could not complete request", ex);
				HttpServletResponse response = (HttpServletResponse) this.asyncContext.getResponse();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				this.asyncContext.complete();
			});
		}

		@Override
		public void onComplete() {
			runIfAsyncNotComplete(() -> {
				logger.debug("Successfully completed request");
				this.asyncContext.complete();
			});
		}

		private void runIfAsyncNotComplete(Runnable task) {
			try {
				if (this.asyncContext.getRequest().isAsyncStarted()) {
					task.run();
				}
			}
			catch (IllegalStateException ex) {
				// Ignore:
				// AsyncContext recycled and should not be used
				// e.g. TIMEOUT_LISTENER (above) may have completed the AsyncContext
			}
		}
	}

}
