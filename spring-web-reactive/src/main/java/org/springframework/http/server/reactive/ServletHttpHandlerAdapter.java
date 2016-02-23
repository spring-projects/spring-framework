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
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.core.io.buffer.DefaultDataBufferAllocator;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
@WebServlet(asyncSupported = true)
public class ServletHttpHandlerAdapter extends HttpServlet {

	private static final int DEFAULT_BUFFER_SIZE = 8192;

	private static Log logger = LogFactory.getLog(ServletHttpHandlerAdapter.class);


	private HttpHandler handler;

	// Servlet is based on blocking I/O, hence the usage of non-direct, heap-based buffers
	// (i.e. 'false' as constructor argument)
	private DataBufferAllocator allocator = new DefaultDataBufferAllocator(false);

	private int bufferSize = DEFAULT_BUFFER_SIZE;


	public void setHandler(HttpHandler handler) {
		Assert.notNull(handler, "'handler' must not be null");
		this.handler = handler;
	}

	public void setAllocator(DataBufferAllocator allocator) {
		Assert.notNull(allocator, "'allocator' must not be null");
		this.allocator = allocator;
	}

	public void setBufferSize(int bufferSize) {
		Assert.isTrue(bufferSize > 0);
		this.bufferSize = bufferSize;
	}

	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		AsyncContext context = servletRequest.startAsync();
		ServletAsyncContextSynchronizer synchronizer = new ServletAsyncContextSynchronizer(context);

		ServletServerHttpRequest request =
				new ServletServerHttpRequest(synchronizer, this.allocator,
						this.bufferSize);

		ServletServerHttpResponse response =
				new ServletServerHttpResponse(synchronizer, this.bufferSize);

		HandlerResultSubscriber resultSubscriber =
				new HandlerResultSubscriber(synchronizer);

		this.handler.handle(request, response).subscribe(resultSubscriber);
	}

	private static class HandlerResultSubscriber implements Subscriber<Void> {

		private final ServletAsyncContextSynchronizer synchronizer;

		public HandlerResultSubscriber(ServletAsyncContextSynchronizer synchronizer) {
			this.synchronizer = synchronizer;
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
			logger.error("Error from request handling. Completing the request.", ex);
			HttpServletResponse response =
					(HttpServletResponse) this.synchronizer.getResponse();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			this.synchronizer.complete();
		}

		@Override
		public void onComplete() {
			this.synchronizer.complete();
		}
	}
}
