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
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServletServerHttpResponse extends AbstractListenerServerHttpResponse {

	private final HttpServletResponse response;

	private final int bufferSize;

	private volatile ResponseBodyFlushProcessor bodyFlushProcessor;

	private volatile ResponseBodyProcessor bodyProcessor;

	private volatile boolean flushOnNext;


	public ServletServerHttpResponse(HttpServletResponse response, AsyncContext asyncContext,
			DataBufferFactory bufferFactory, int bufferSize) throws IOException {

		super(bufferFactory);

		Assert.notNull(response, "HttpServletResponse must not be null");
		Assert.notNull(bufferFactory, "DataBufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be greater than 0");

		this.response = response;
		this.bufferSize = bufferSize;

		asyncContext.addListener(new ResponseAsyncListener());

		// Tomcat expects WriteListener registration on initial thread
		response.getOutputStream().setWriteListener(new ResponseBodyWriteListener());
	}


	public HttpServletResponse getServletResponse() {
		return this.response;
	}

	@Override
	protected void applyStatusCode() {
		HttpStatus statusCode = this.getStatusCode();
		if (statusCode != null) {
			getServletResponse().setStatus(statusCode.value());
		}
	}

	@Override
	protected void applyHeaders() {
		for (Map.Entry<String, List<String>> entry : getHeaders().entrySet()) {
			String headerName = entry.getKey();
			for (String headerValue : entry.getValue()) {
				this.response.addHeader(headerName, headerValue);
			}
		}
		MediaType contentType = getHeaders().getContentType();
		if (this.response.getContentType() == null && contentType != null) {
			this.response.setContentType(contentType.toString());
		}
		Charset charset = (contentType != null ? contentType.getCharset() : null);
		if (this.response.getCharacterEncoding() == null && charset != null) {
			this.response.setCharacterEncoding(charset.name());
		}
	}

	@Override
	protected void applyCookies() {
		for (String name : getCookies().keySet()) {
			for (ResponseCookie httpCookie : getCookies().get(name)) {
				Cookie cookie = new Cookie(name, httpCookie.getValue());
				if (!httpCookie.getMaxAge().isNegative()) {
					cookie.setMaxAge((int) httpCookie.getMaxAge().getSeconds());
				}
				httpCookie.getDomain().ifPresent(cookie::setDomain);
				httpCookie.getPath().ifPresent(cookie::setPath);
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.response.addCookie(cookie);
			}
		}
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
		ServletOutputStream outputStream = response.getOutputStream();
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
		ServletOutputStream outputStream = this.response.getOutputStream();
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

		void handleError(Throwable ex) {
			if (bodyFlushProcessor != null) {
				bodyFlushProcessor.cancel();
				bodyFlushProcessor.onError(ex);
			}
			if (bodyProcessor != null) {
				bodyProcessor.cancel();
				bodyProcessor.onError(ex);
			}
		}

		@Override
		public void onComplete(AsyncEvent event) {
			if (bodyFlushProcessor != null) {
				bodyFlushProcessor.cancel();
				bodyFlushProcessor.onComplete();
			}
			if (bodyProcessor != null) {
				bodyProcessor.cancel();
				bodyProcessor.onComplete();
			}
		}
	}

	private class ResponseBodyWriteListener implements WriteListener {

		@Override
		public void onWritePossible() throws IOException {
			if (bodyProcessor != null) {
				bodyProcessor.onWritePossible();
			}
		}

		@Override
		public void onError(Throwable ex) {
			if (bodyProcessor != null) {
				bodyProcessor.cancel();
				bodyProcessor.onError(ex);
			}
		}
	}

	private class ResponseBodyFlushProcessor extends AbstractListenerWriteFlushProcessor<DataBuffer> {

		@Override
		protected Processor<? super DataBuffer, Void> createWriteProcessor() {
			try {
				ServletOutputStream outputStream = response.getOutputStream();
				bodyProcessor = new ResponseBodyProcessor(outputStream);
				return bodyProcessor;
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

		@Override
		protected void flush() throws IOException {
			if (logger.isTraceEnabled()) {
				logger.trace("flush");
			}
			ServletServerHttpResponse.this.flush();
		}
	}

	private class ResponseBodyProcessor extends AbstractListenerWriteProcessor<DataBuffer> {

		private final ServletOutputStream outputStream;


		public ResponseBodyProcessor(ServletOutputStream outputStream) {
			this.outputStream = outputStream;
		}

		@Override
		protected boolean isWritePossible() {
			return this.outputStream.isReady();
		}

		@Override
		protected void releaseData() {
			if (logger.isTraceEnabled()) {
				logger.trace("releaseBuffer: " + this.currentData);
			}
			DataBufferUtils.release(this.currentData);
			this.currentData = null;
		}

		@Override
		protected boolean isDataEmpty(DataBuffer dataBuffer) {
			return dataBuffer.readableByteCount() == 0;
		}

		@Override
		protected boolean write(DataBuffer dataBuffer) throws IOException {
			if (ServletServerHttpResponse.this.flushOnNext) {
				if (logger.isTraceEnabled()) {
					logger.trace("flush");
				}
				flush();
			}
			boolean ready = this.outputStream.isReady();
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("write: " + dataBuffer + " ready: " + ready);
			}
			int remaining = dataBuffer.readableByteCount();
			if (ready && remaining > 0) {
				int written = writeToOutputStream(dataBuffer);
				if (this.logger.isTraceEnabled()) {
					this.logger.trace("written: " + written + " total: " + remaining);
				}
				return written == remaining;
			}
			else {
				return false;
			}
		}
	}

}
