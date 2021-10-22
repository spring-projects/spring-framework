/*
 * Copyright 2002-2021 the original author or authors.
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
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class ServletServerHttpResponse extends AbstractListenerServerHttpResponse {

	private final HttpServletResponse response;

	private final ServletOutputStream outputStream;

	private final int bufferSize;

	@Nullable
	private volatile ResponseBodyFlushProcessor bodyFlushProcessor;

	@Nullable
	private volatile ResponseBodyProcessor bodyProcessor;

	private volatile boolean flushOnNext;

	private final ServletServerHttpRequest request;

	private final ResponseAsyncListener asyncListener;


	public ServletServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
			DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {

		this(new HttpHeaders(), response, asyncContext, bufferFactory, bufferSize, request);
	}

	public ServletServerHttpResponse(HttpHeaders headers, HttpServletResponse response, AsyncContext asyncContext,
			DataBufferFactory bufferFactory, int bufferSize, ServletServerHttpRequest request) throws IOException {

		super(bufferFactory, headers);

		Assert.notNull(response, "HttpServletResponse must not be null");
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "Buffer size must be greater than 0");

		this.response = response;
		this.outputStream = response.getOutputStream();
		this.bufferSize = bufferSize;
		this.request = request;

		this.asyncListener = new ResponseAsyncListener();

		// Tomcat expects WriteListener registration on initial thread
		response.getOutputStream().setWriteListener(new ResponseBodyWriteListener());
	}


	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeResponse() {
		return (T) this.response;
	}

	@Override
	public HttpStatus getStatusCode() {
		HttpStatus status = super.getStatusCode();
		return (status != null ? status : HttpStatus.resolve(this.response.getStatus()));
	}

	@Override
	public Integer getRawStatusCode() {
		Integer status = super.getRawStatusCode();
		return (status != null ? status : this.response.getStatus());
	}

	@Override
	protected void applyStatusCode() {
		Integer status = super.getRawStatusCode();
		if (status != null) {
			this.response.setStatus(status);
		}
	}

	@Override
	protected void applyHeaders() {
		getHeaders().forEach((headerName, headerValues) -> {
			for (String headerValue : headerValues) {
				this.response.addHeader(headerName, headerValue);
			}
		});
		MediaType contentType = null;
		try {
			contentType = getHeaders().getContentType();
		}
		catch (Exception ex) {
			String rawContentType = getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
			this.response.setContentType(rawContentType);
		}
		if (this.response.getContentType() == null && contentType != null) {
			this.response.setContentType(contentType.toString());
		}
		Charset charset = (contentType != null ? contentType.getCharset() : null);
		if (this.response.getCharacterEncoding() == null && charset != null) {
			this.response.setCharacterEncoding(charset.name());
		}
		long contentLength = getHeaders().getContentLength();
		if (contentLength != -1) {
			this.response.setContentLengthLong(contentLength);
		}
	}

	@Override
	protected void applyCookies() {

		// Servlet Cookie doesn't support same site:
		// https://github.com/eclipse-ee4j/servlet-api/issues/175

		// For Jetty, starting 9.4.21+ we could adapt to HttpCookie:
		// https://github.com/eclipse/jetty.project/issues/3040

		// For Tomcat it seems to be a global option only:
		// https://tomcat.apache.org/tomcat-8.5-doc/config/cookie-processor.html

		for (List<ResponseCookie> cookies : getCookies().values()) {
			for (ResponseCookie cookie : cookies) {
				this.response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}
	}

	/**
	 * Return an {@link ResponseAsyncListener} that notifies the response
	 * body Publisher and Subscriber of Servlet container events. The listener
	 * is not actually registered but is rather exposed for
	 * {@link ServletHttpHandlerAdapter} to ensure events are delegated.
	 */
	AsyncListener getAsyncListener() {
		return this.asyncListener;
	}

	@Override
	protected Processor<? super Publisher<? extends DataBuffer>, Void> createBodyFlushProcessor() {
		ResponseBodyFlushProcessor processor = new ResponseBodyFlushProcessor();
		this.bodyFlushProcessor = processor;
		return processor;
	}

	/**
	 * Write the DataBuffer to the response body OutputStream.
	 * Invoked only when {@link ServletOutputStream#isReady()} returns "true"
	 * and the readable bytes in the DataBuffer is greater than 0.
	 * @return the number of bytes written
	 */
	protected int writeToOutputStream(DataBuffer dataBuffer) throws IOException {
		ServletOutputStream outputStream = this.outputStream;
		InputStream input = dataBuffer.asInputStream();
		int bytesWritten = 0;
		byte[] buffer = new byte[this.bufferSize];
		int bytesRead;
		while (outputStream.isReady() && (bytesRead = input.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
			bytesWritten += bytesRead;
		}
		return bytesWritten;
	}

	private void flush() throws IOException {
		ServletOutputStream outputStream = this.outputStream;
		if (outputStream.isReady()) {
			try {
				outputStream.flush();
				this.flushOnNext = false;
			}
			catch (IOException ex) {
				this.flushOnNext = true;
				throw ex;
			}
		}
		else {
			this.flushOnNext = true;
		}
	}

	private boolean isWritePossible() {
		return this.outputStream.isReady();
	}


	private final class ResponseAsyncListener implements AsyncListener {

		@Override
		public void onStartAsync(AsyncEvent event) {}

		@Override
		public void onTimeout(AsyncEvent event) {
			Throwable ex = event.getThrowable();
			ex = (ex != null ? ex : new IllegalStateException("Async operation timeout."));
			handleError(ex);
		}

		@Override
		public void onError(AsyncEvent event) {
			handleError(event.getThrowable());
		}

		public void handleError(Throwable ex) {
			ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
			ResponseBodyProcessor processor = bodyProcessor;
			if (flushProcessor != null) {
				// Cancel the upstream source of "write" Publishers
				flushProcessor.cancel();
				// Cancel the current "write" Publisher and propagate onComplete downstream
				if (processor != null) {
					processor.cancel();
					processor.onError(ex);
				}
				// This is a no-op if processor was connected and onError propagated all the way
				flushProcessor.onError(ex);
			}
		}

		@Override
		public void onComplete(AsyncEvent event) {
			ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
			ResponseBodyProcessor processor = bodyProcessor;
			if (flushProcessor != null) {
				// Cancel the upstream source of "write" Publishers
				flushProcessor.cancel();
				// Cancel the current "write" Publisher and propagate onComplete downstream
				if (processor != null) {
					processor.cancel();
					processor.onComplete();
				}
				// This is a no-op if processor was connected and onComplete propagated all the way
				flushProcessor.onComplete();
			}
		}
	}


	private class ResponseBodyWriteListener implements WriteListener {

		@Override
		public void onWritePossible() {
			ResponseBodyProcessor processor = bodyProcessor;
			if (processor != null) {
				processor.onWritePossible();
			}
			else {
				ResponseBodyFlushProcessor flushProcessor = bodyFlushProcessor;
				if (flushProcessor != null) {
					flushProcessor.onFlushPossible();
				}
			}
		}

		@Override
		public void onError(Throwable ex) {
			ServletServerHttpResponse.this.asyncListener.handleError(ex);
		}
	}


	private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

		public ResponseBodyFlushProcessor() {
			super(request.getLogPrefix());
		}

		@Override
		protected Processor<? super DataBuffer, Void> createWriteProcessor() {
			ResponseBodyProcessor processor = new ResponseBodyProcessor();
			bodyProcessor = processor;
			return processor;
		}

		@Override
		protected void flush() throws IOException {
			if (rsWriteFlushLogger.isTraceEnabled()) {
				rsWriteFlushLogger.trace(getLogPrefix() + "flushing");
			}
			ServletServerHttpResponse.this.flush();
		}

		@Override
		protected boolean isWritePossible() {
			return ServletServerHttpResponse.this.isWritePossible();
		}

		@Override
		protected boolean isFlushPending() {
			return flushOnNext;
		}
	}


	private class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {


		public ResponseBodyProcessor() {
			super(request.getLogPrefix());
		}

		@Override
		protected boolean isWritePossible() {
			return ServletServerHttpResponse.this.isWritePossible();
		}

		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return dataBuffer.readableByteCount() == 0;
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			if (ServletServerHttpResponse.this.flushOnNext) {
				if (rsWriteLogger.isTraceEnabled()) {
					rsWriteLogger.trace(getLogPrefix() + "flushing");
				}
				flush();
			}

			boolean ready = ServletServerHttpResponse.this.isWritePossible();
			int remaining = dataBuffer.readableByteCount();
			if (ready && remaining > 0) {
				// In case of IOException, onError handling should call discardData(DataBuffer)..
				int written = writeToOutputStream(dataBuffer);
				if (rsWriteLogger.isTraceEnabled()) {
					rsWriteLogger.trace(getLogPrefix() + "Wrote " + written + " of " + remaining + " bytes");
				}
				if (written == remaining) {
					DataBufferUtils.release(dataBuffer);
					return true;
				}
			}
			else {
				if (rsWriteLogger.isTraceEnabled()) {
					rsWriteLogger.trace(getLogPrefix() + "ready: " + ready + ", remaining: " + remaining);
				}
			}

			return false;
		}

		@Override
		protected void writingComplete() {
			bodyProcessor = null;
		}

		@Override
		protected void discardData(DataBuffer dataBuffer) {
			DataBufferUtils.release(dataBuffer);
		}
	}

}
