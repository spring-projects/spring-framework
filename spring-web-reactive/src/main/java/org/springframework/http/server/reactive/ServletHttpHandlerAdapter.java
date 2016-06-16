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
import reactor.core.util.BackpressureUtils;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.FlushingDataBuffer;
import org.springframework.core.io.buffer.support.DataBufferUtils;
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
				new RequestBodyPublisher(synchronizer, dataBufferFactory, bufferSize);
		requestBody.registerListener();
		ServletServerHttpRequest request =
				new ServletServerHttpRequest(servletRequest, requestBody);

		ResponseBodySubscriber responseBody =
				new ResponseBodySubscriber(synchronizer, bufferSize);
		responseBody.registerListener();
		ServletServerHttpResponse response =
				new ServletServerHttpResponse(servletResponse, dataBufferFactory,
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

		private static final Log logger = LogFactory.getLog(RequestBodyPublisher.class);

		private final RequestBodyReadListener readListener =
				new RequestBodyReadListener();

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
			this.synchronizer.getRequest().getInputStream().setReadListener(readListener);
		}

		@Override
		protected void noLongerStalled() {
			try {
				readListener.onDataAvailable();
			}
			catch (IOException ex) {
				readListener.onError(ex);
			}
		}

		private class RequestBodyReadListener implements ReadListener {

			@Override
			public void onDataAvailable() throws IOException {
				if (isSubscriptionCancelled()) {
					return;
				}
				logger.trace("onDataAvailable");
				ServletInputStream input = synchronizer.getRequest().getInputStream();

				while (true) {
					if (!checkSubscriptionForDemand()) {
						break;
					}

					boolean ready = input.isReady();
					logger.trace(
							"Input ready: " + ready + " finished: " + input.isFinished());

					if (!ready) {
						break;
					}

					int read = input.read(buffer);
					logger.trace("Input read:" + read);

					if (read == -1) {
						break;
					}
					else if (read > 0) {
						DataBuffer dataBuffer = dataBufferFactory.allocateBuffer(read);
						dataBuffer.write(buffer, 0, read);

						publishOnNext(dataBuffer);
					}
				}
			}

			@Override
			public void onAllDataRead() throws IOException {
				logger.trace("All data read");
				synchronizer.readComplete();

				publishOnComplete();
			}

			@Override
			public void onError(Throwable t) {
				logger.trace("RequestBodyReadListener Error", t);
				synchronizer.readComplete();

				publishOnError(t);
			}
		}

	}

	private static class ResponseBodySubscriber implements Subscriber<DataBuffer> {

		private static final Log logger = LogFactory.getLog(ResponseBodySubscriber.class);

		private final ResponseBodyWriteListener writeListener =
				new ResponseBodyWriteListener();

		private final ServletAsyncContextSynchronizer synchronizer;

		private final int bufferSize;

		private volatile DataBuffer dataBuffer;

		private volatile boolean completed = false;

		private volatile boolean flushOnNext = false;

		private Subscription subscription;


		public ResponseBodySubscriber(ServletAsyncContextSynchronizer synchronizer,
				int bufferSize) {
			this.synchronizer = synchronizer;
			this.bufferSize = bufferSize;
		}

		public void registerListener() throws IOException {
			synchronizer.getResponse().getOutputStream().setWriteListener(writeListener);
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			logger.trace("onSubscribe. Subscription: " + subscription);
			if (BackpressureUtils.validate(this.subscription, subscription)) {
				this.subscription = subscription;
				this.subscription.request(1);
			}
		}

		@Override
		public void onNext(DataBuffer dataBuffer) {
			Assert.state(this.dataBuffer == null);

			logger.trace("onNext. buffer: " + dataBuffer);

			this.dataBuffer = dataBuffer;
			try {
				this.writeListener.onWritePossible();
			}
			catch (IOException e) {
				onError(e);
			}
		}

		@Override
		public void onError(Throwable t) {
			logger.error("onError", t);
			HttpServletResponse response =
					(HttpServletResponse) this.synchronizer.getResponse();
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			this.synchronizer.complete();

		}

		@Override
		public void onComplete() {
			logger.trace("onComplete. buffer: " + this.dataBuffer);

			this.completed = true;

			if (this.dataBuffer != null) {
				try {
					this.writeListener.onWritePossible();
				}
				catch (IOException ex) {
					onError(ex);
				}
			}

			if (this.dataBuffer == null) {
				this.synchronizer.writeComplete();
			}
		}

		private class ResponseBodyWriteListener implements WriteListener {

			@Override
			public void onWritePossible() throws IOException {
				logger.trace("onWritePossible");
				ServletOutputStream output = synchronizer.getResponse().getOutputStream();

				boolean ready = output.isReady();

				if (flushOnNext) {
					flush(output);
					ready = output.isReady();
				}

				logger.trace("ready: " + ready + " buffer: " + dataBuffer);

				if (ready) {
					if (dataBuffer != null) {

						int total = dataBuffer.readableByteCount();
						int written = writeDataBuffer();

						logger.trace("written: " + written + " total: " + total);
						if (written == total) {
							if (dataBuffer instanceof FlushingDataBuffer) {
								flush(output);
							}
							releaseBuffer();
							if (!completed) {
								subscription.request(1);
							}
							else {
								synchronizer.writeComplete();
							}
						}
					}
					else if (subscription != null) {
						subscription.request(1);
					}
				}
			}

			private int writeDataBuffer() throws IOException {
				InputStream input = dataBuffer.asInputStream();
				ServletOutputStream output = synchronizer.getResponse().getOutputStream();

				int bytesWritten = 0;
				byte[] buffer = new byte[bufferSize];
				int bytesRead = -1;

				while (output.isReady() && (bytesRead = input.read(buffer)) != -1) {
					output.write(buffer, 0, bytesRead);
					bytesWritten += bytesRead;
				}

				return bytesWritten;
			}

			private void flush(ServletOutputStream output) {
				if (output.isReady()) {
					logger.trace("Flushing");
					try {
						output.flush();
						flushOnNext = false;
					}
					catch (IOException ignored) {
					}
				} else {
					flushOnNext = true;
				}
			}

			private void releaseBuffer() {
				DataBufferUtils.release(dataBuffer);
				dataBuffer = null;
			}

			@Override
			public void onError(Throwable ex) {
				logger.error("ResponseBodyWriteListener error", ex);
			}
		}
	}

}