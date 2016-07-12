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
	protected void service(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws ServletException, IOException {

		AsyncContext asyncContext = servletRequest.startAsync();

		RequestBodyPublisher requestBody =
				new RequestBodyPublisher(servletRequest.getInputStream(),
						this.dataBufferFactory, this.bufferSize);
		requestBody.registerListener();
		ServletServerHttpRequest request =
				new ServletServerHttpRequest(servletRequest, requestBody);

		ResponseBodyProcessor responseBody =
				new ResponseBodyProcessor(servletResponse.getOutputStream(),
						this.bufferSize);
		responseBody.registerListener();
		ServletServerHttpResponse response =
				new ServletServerHttpResponse(servletResponse, this.dataBufferFactory,
						publisher -> Mono.from(subscriber -> {
							publisher.subscribe(responseBody);
							responseBody.subscribe(subscriber);
						}));

		HandlerResultSubscriber resultSubscriber =
				new HandlerResultSubscriber(asyncContext);

		this.handler.handle(request, response).subscribe(resultSubscriber);
	}

	private static class HandlerResultSubscriber implements Subscriber<Void> {

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
			logger.error("Error from request handling. Completing the request.", ex);
			HttpServletResponse response =
					(HttpServletResponse) this.asyncContext.getResponse();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			this.asyncContext.complete();
		}

		@Override
		public void onComplete() {
			this.asyncContext.complete();
		}
	}

	private static class RequestBodyPublisher extends AbstractRequestBodyPublisher {

		private final RequestBodyPublisher.RequestBodyReadListener readListener =
				new RequestBodyPublisher.RequestBodyReadListener();

		private final ServletInputStream inputStream;

		private final DataBufferFactory dataBufferFactory;

		private final byte[] buffer;

		public RequestBodyPublisher(ServletInputStream inputStream,
				DataBufferFactory dataBufferFactory, int bufferSize) {
			this.inputStream = inputStream;
			this.dataBufferFactory = dataBufferFactory;
			this.buffer = new byte[bufferSize];
		}

		public void registerListener() throws IOException {
			this.inputStream.setReadListener(this.readListener);
		}

		@Override
		protected void checkOnDataAvailable() {
			if (!this.inputStream.isFinished() && this.inputStream.isReady()) {
				onDataAvailable();
			}
		}

		@Override
		protected DataBuffer read() throws IOException {
			if (this.inputStream.isReady()) {
				int read = this.inputStream.read(this.buffer);
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

	private static class ResponseBodyProcessor extends AbstractResponseBodyProcessor {

		private final ResponseBodyWriteListener writeListener =
				new ResponseBodyWriteListener();

		private final ServletOutputStream outputStream;

		private final int bufferSize;

		private volatile boolean flushOnNext;

		public ResponseBodyProcessor(ServletOutputStream outputStream, int bufferSize) {
			this.outputStream = outputStream;
			this.bufferSize = bufferSize;
		}

		public void registerListener() throws IOException {
			this.outputStream.setWriteListener(this.writeListener);
		}

		@Override
		protected boolean isWritePossible() {
			return this.outputStream.isReady();
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			if (this.flushOnNext) {
				flush();
			}

			boolean ready = this.outputStream.isReady();

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
		protected void flush() throws IOException {
			if (this.outputStream.isReady()) {
				if (logger.isTraceEnabled()) {
					logger.trace("flush");
				}
				try {
					this.outputStream.flush();
					this.flushOnNext = false;
					return;
				}
				catch (IOException ignored) {
				}
			}
			this.flushOnNext = true;

		}

		private int writeDataBuffer(DataBuffer dataBuffer) throws IOException {
			InputStream input = dataBuffer.asInputStream();

			int bytesWritten = 0;
			byte[] buffer = new byte[this.bufferSize];
			int bytesRead = -1;

			while (this.outputStream.isReady() &&
					(bytesRead = input.read(buffer)) != -1) {
				this.outputStream.write(buffer, 0, bytesRead);
				bytesWritten += bytesRead;
			}

			return bytesWritten;
		}

		private class ResponseBodyWriteListener implements WriteListener {

			@Override
			public void onWritePossible() throws IOException {
				ResponseBodyProcessor.this.onWritePossible();
			}

			@Override
			public void onError(Throwable ex) {
				// Error on writing to the HTTP stream, so any further writes will probably
				// fail. Let's log instead of calling {@link #writeError}.
				ResponseBodyProcessor.this.logger
						.error("ResponseBodyWriteListener error", ex);
			}
		}
	}


}