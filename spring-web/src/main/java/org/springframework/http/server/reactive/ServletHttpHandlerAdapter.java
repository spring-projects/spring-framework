/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.DispatcherType;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Adapt {@link HttpHandler} to an {@link HttpServlet} using Servlet Async
 * support and Servlet 3.1 non-blocking I/O.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see org.springframework.web.server.adapter.AbstractReactiveWebInitializer
 */
@SuppressWarnings("serial")
public class ServletHttpHandlerAdapter implements Servlet {

	private static final Log logger = LogFactory.getLog(ServletHttpHandlerAdapter.class);

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static final String WRITE_ERROR_ATTRIBUTE_NAME = ServletHttpHandlerAdapter.class.getName() + ".ERROR";


	private final HttpHandler httpHandler;

	private int bufferSize = DEFAULT_BUFFER_SIZE;

	@Nullable
	private String servletPath;

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

	/**
	 * Return the Servlet path under which the Servlet is deployed by checking
	 * the Servlet registration from {@link #init(ServletConfig)}.
	 * @return the path, or an empty string if the Servlet is deployed without
	 * a prefix (i.e. "/" or "/*"), or {@code null} if this method is invoked
	 * before the {@link #init(ServletConfig)} Servlet container callback.
	 */
	@Nullable
	public String getServletPath() {
		return this.servletPath;
	}

	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		this.dataBufferFactory = dataBufferFactory;
	}

	public DataBufferFactory getDataBufferFactory() {
		return this.dataBufferFactory;
	}


	// Servlet methods...

	@Override
	public void init(ServletConfig config) {
		this.servletPath = getServletPath(config);
	}

	private String getServletPath(ServletConfig config) {
		String name = config.getServletName();
		ServletRegistration registration = config.getServletContext().getServletRegistration(name);
		if (registration == null) {
			throw new IllegalStateException("ServletRegistration not found for Servlet '" + name + "'");
		}

		Collection<String> mappings = registration.getMappings();
		if (mappings.size() == 1) {
			String mapping = mappings.iterator().next();
			if (mapping.equals("/")) {
				return "";
			}
			if (mapping.endsWith("/*")) {
				String path = mapping.substring(0, mapping.length() - 2);
				if (!path.isEmpty()) {
					logger.info("Found Servlet mapping '" + path + "' for Servlet '" + name + "'");
				}
				return path;
			}
		}

		throw new IllegalArgumentException("Expected a single Servlet mapping: " +
				"either the default Servlet mapping (i.e. '/'), " +
				"or a path based mapping (e.g. '/*', '/foo/*'). " +
				"Actual mappings: " + mappings + " for Servlet '" + name + "'");
	}


	@Override
	public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {

		if (DispatcherType.ASYNC.equals(request.getDispatcherType())) {
			Throwable ex = (Throwable) request.getAttribute(WRITE_ERROR_ATTRIBUTE_NAME);
			throw new ServletException("Write publisher error", ex);
		}

		// Start async before Read/WriteListener registration
		AsyncContext asyncContext = request.startAsync();
		asyncContext.setTimeout(-1);

		ServerHttpRequest httpRequest;
		try {
			httpRequest = createRequest(((HttpServletRequest) request), asyncContext);
		}
		catch (URISyntaxException ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Invalid URL for incoming request: " + ex.getMessage());
			}
			((HttpServletResponse) response).setStatus(400);
			asyncContext.complete();
			return;
		}
		ServerHttpResponse httpResponse = createResponse(((HttpServletResponse) response), asyncContext);

		if (httpRequest.getMethod() == HttpMethod.HEAD) {
			httpResponse = new HttpHeadResponseDecorator(httpResponse);
		}

		AtomicBoolean isCompleted = new AtomicBoolean();
		HandlerResultAsyncListener listener = new HandlerResultAsyncListener(isCompleted);
		asyncContext.addListener(listener);

		HandlerResultSubscriber subscriber = new HandlerResultSubscriber(asyncContext, isCompleted);
		this.httpHandler.handle(httpRequest, httpResponse).subscribe(subscriber);
	}

	protected ServerHttpRequest createRequest(HttpServletRequest request, AsyncContext context)
			throws IOException, URISyntaxException {

		Assert.notNull(this.servletPath, "Servlet path is not initialized");
		return new ServletServerHttpRequest(
				request, context, this.servletPath, getDataBufferFactory(), getBufferSize());
	}

	protected ServerHttpResponse createResponse(HttpServletResponse response, AsyncContext context) throws IOException {
		return new ServletServerHttpResponse(response, context, getDataBufferFactory(), getBufferSize());
	}

	@Override
	public String getServletInfo() {
		return "";
	}

	@Override
	@Nullable
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public void destroy() {
	}


	/**
	 * We cannot combine ERROR_LISTENER and HandlerResultSubscriber due to:
	 * https://issues.jboss.org/browse/WFLY-8515
	 */
	private static void runIfAsyncNotComplete(AsyncContext asyncContext, AtomicBoolean isCompleted, Runnable task) {
		try {
			if (asyncContext.getRequest().isAsyncStarted() && isCompleted.compareAndSet(false, true)) {
				task.run();
			}
		}
		catch (IllegalStateException ex) {
			// Ignore: AsyncContext recycled and should not be used
			// e.g. TIMEOUT_LISTENER (above) may have completed the AsyncContext
		}
	}


	private static class HandlerResultAsyncListener implements AsyncListener {

		private final AtomicBoolean isCompleted;

		public HandlerResultAsyncListener(AtomicBoolean isCompleted) {
			this.isCompleted = isCompleted;
		}

		@Override
		public void onTimeout(AsyncEvent event) {
			logger.debug("Timeout notification from Servlet container");
			AsyncContext context = event.getAsyncContext();
			runIfAsyncNotComplete(context, this.isCompleted, context::complete);
		}

		@Override
		public void onError(AsyncEvent event) {
			logger.debug("Error notification from Servlet container");
			AsyncContext context = event.getAsyncContext();
			runIfAsyncNotComplete(context, this.isCompleted, context::complete);
		}

		@Override
		public void onStartAsync(AsyncEvent event) {
			// no-op
		}

		@Override
		public void onComplete(AsyncEvent event) {
			// no-op
		}
	}


	private class HandlerResultSubscriber implements Subscriber<Void> {

		private final AsyncContext asyncContext;

		private final AtomicBoolean isCompleted;

		public HandlerResultSubscriber(AsyncContext asyncContext, AtomicBoolean isCompleted) {
			this.asyncContext = asyncContext;
			this.isCompleted = isCompleted;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Void aVoid) {
			// no-op
		}

		@Override
		public void onError(Throwable ex) {
			logger.warn("Handling completed with error: " + ex.getMessage());
			runIfAsyncNotComplete(this.asyncContext, this.isCompleted, () -> {
				if (this.asyncContext.getResponse().isCommitted()) {
					logger.debug("Dispatching into container to raise error");
					this.asyncContext.getRequest().setAttribute(WRITE_ERROR_ATTRIBUTE_NAME, ex);
					this.asyncContext.dispatch();
				}
				else {
					try {
						logger.debug("Setting response status code to 500");
						this.asyncContext.getResponse().resetBuffer();
						((HttpServletResponse) this.asyncContext.getResponse()).setStatus(500);
					}
					finally {
						this.asyncContext.complete();
					}
				}
			});
		}

		@Override
		public void onComplete() {
			logger.debug("Handling completed with success");
			runIfAsyncNotComplete(this.asyncContext, this.isCompleted, this.asyncContext::complete);
		}
	}

}
