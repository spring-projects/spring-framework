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
import java.io.InputStream;
import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
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
	private DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(false);

	private int bufferSize = DEFAULT_BUFFER_SIZE;


	public void setHandler(HttpHandler handler) {
		Assert.notNull(handler, "'handler' must not be null");
		this.handler = handler;
	}

	public void setDataBufferFactory(DataBufferFactory dataBufferFactory) {
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		this.dataBufferFactory = dataBufferFactory;
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

		RequestBodyPublisher requestBody =
				new RequestBodyPublisher(synchronizer, this.dataBufferFactory,
						this.bufferSize);
		requestBody.registerListener();
		ServletServerHttpRequest request =
				new ServletServerHttpRequest(servletRequest, requestBody);

		ResponseBodySubscriber responseBody =
				new ResponseBodySubscriber(synchronizer, this.bufferSize);
		responseBody.registerListener();
		ServletServerHttpResponse response =
				new ServletServerHttpResponse(servletResponse, this.dataBufferFactory,
						publisher -> Mono
								.from(subscriber -> publisher.subscribe(responseBody)));

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

	private static class RequestBodyPublisher extends AbstractRequestBodyPublisher {

		private final RequestBodyPublisher.RequestBodyReadListener readListener =
				new RequestBodyPublisher.RequestBodyReadListener();

		private final ServletAsyncContextSynchronizer synchronizer;

		private final DataBufferFactory dataBufferFactory;

		private final byte[] buffer;

		public RequestBodyPublisher(ServletAsyncContextSynchronizer synchronizer,
				DataBufferFactory dataBufferFactory, int bufferSize) {
			this.synchronizer = synchronizer;
			this.dataBufferFactory = dataBufferFactory;
			this.buffer = new byte[bufferSize];
		}

		public void registerListener() throws IOException {
			this.synchronizer.getRequest().getInputStream()
					.setReadListener(this.readListener);
		}

		@Override
		protected DataBuffer read() throws IOException {
			ServletInputStream input = this.synchronizer.getRequest().getInputStream();
			if (input.isReady()) {
				int read = input.read(this.buffer);
				if (logger.isTraceEnabled()) {
					logger.trace("read:" + read);
				}

				if (read > 0) {
					DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer(read);
					dataBuffer.write(this.buffer, 0, read);
					return dataBuffer;
				}
			}
			return null;
		}

		@Override
		protected void close() {
			this.synchronizer.readComplete();

		}

		private class RequestBodyReadListener implements ReadListener {

			@Override
			public void onDataAvailable() throws IOException {
				RequestBodyPublisher.this.onDataAvailable();
			}

			@Override
			public void onAllDataRead() throws IOException {
				RequestBodyPublisher.this.onAllDataRead();
			}

			@Override
			public void onError(Throwable throwable) {
				RequestBodyPublisher.this.onError(throwable);

			}
		}
	}


	private static class ResponseBodySubscriber extends AbstractResponseBodySubscriber {

		private final ResponseBodyWriteListener writeListener =
				new ResponseBodyWriteListener();

		private final ServletAsyncContextSynchronizer synchronizer;

		private final int bufferSize;

		private volatile boolean flushOnNext;

		public ResponseBodySubscriber(ServletAsyncContextSynchronizer synchronizer,
				int bufferSize) {
			this.synchronizer = synchronizer;
			this.bufferSize = bufferSize;
		}

		public void registerListener() throws IOException {
			outputStream().setWriteListener(this.writeListener);
		}

		private ServletOutputStream outputStream() throws IOException {
			return this.synchronizer.getResponse().getOutputStream();
		}

		@Override
		protected void checkOnWritePossible() {
			try {
				if (outputStream().isReady()) {
					onWritePossible();
				}
			}
			catch (IOException ex) {
				onError(ex);
			}
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			ServletOutputStream output = outputStream();

			if (this.flushOnNext) {
				flush();
			}

			boolean ready = output.isReady();

			if (this.logger.isTraceEnabled()) {
				this.logger.trace("write: " + dataBuffer + " ready: " + ready);
			}

			if (ready) {
				int total = dataBuffer.readableByteCount();
				int written = writeDataBuffer(dataBuffer);

				if (this.logger.isTraceEnabled()) {
					this.logger.trace("written: " + written + " total: " + total);
				}
				return written == total;
			}
			else {
				return false;
			}
		}

		@Override
		protected void writeError(Throwable t) {
			HttpServletResponse response =
					(HttpServletResponse) this.synchronizer.getResponse();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		@Override
		protected void flush() throws IOException {
			ServletOutputStream output = outputStream();
			if (output.isReady()) {
				if (logger.isTraceEnabled()) {
					this.logger.trace("flush");
				}
				try {
					output.flush();
					this.flushOnNext = false;
				}
				catch (IOException ignored) {
				}
			}
			else {
				this.flushOnNext = true;
			}

		}

		@Override
		protected void close() {
			this.synchronizer.writeComplete();
		}

		private int writeDataBuffer(DataBuffer dataBuffer) throws IOException {
			InputStream input = dataBuffer.asInputStream();
			ServletOutputStream output = outputStream();

			int bytesWritten = 0;
			byte[] buffer = new byte[this.bufferSize];
			int bytesRead = -1;

			while (output.isReady() && (bytesRead = input.read(buffer)) != -1) {
				output.write(buffer, 0, bytesRead);
				bytesWritten += bytesRead;
			}

			return bytesWritten;
		}

		private class ResponseBodyWriteListener implements WriteListener {

			@Override
			public void onWritePossible() throws IOException {
				ResponseBodySubscriber.this.onWritePossible();
			}

			@Override
			public void onError(Throwable ex) {
				// Error on writing to the HTTP stream, so any further writes will probably
				// fail. Let's log instead of calling {@link #writeError}.
				ResponseBodySubscriber.this.logger
						.error("ResponseBodyWriteListener error", ex);
			}
		}
	}


}