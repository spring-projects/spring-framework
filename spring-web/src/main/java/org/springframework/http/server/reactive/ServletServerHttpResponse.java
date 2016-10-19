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
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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

	private final ResponseBodyWriteListener writeListener = new ResponseBodyWriteListener();

	private final HttpServletResponse response;

	private final int bufferSize;

	private volatile boolean flushOnNext;

	private volatile ResponseBodyProcessor bodyProcessor;

	private volatile ResponseBodyFlushProcessor bodyFlushProcessor;


	public ServletServerHttpResponse(HttpServletResponse response,
			DataBufferFactory dataBufferFactory, int bufferSize) throws IOException {

		super(dataBufferFactory);
		Assert.notNull(response, "HttpServletResponse must not be null");
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		Assert.isTrue(bufferSize > 0, "Buffer size must be higher than 0");
		this.response = response;
		this.bufferSize = bufferSize;
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
	protected Processor<Publisher<DataBuffer>, Void> createBodyFlushProcessor() {
		ResponseBodyFlushProcessor processor = new ResponseBodyFlushProcessor();
		registerListener();
		bodyFlushProcessor = processor;
		return processor;
	}

	private void registerListener() {
		try {
			outputStream().setWriteListener(writeListener);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private ServletOutputStream outputStream() throws IOException {
		return this.response.getOutputStream();
	}

	private void flush() throws IOException {
		ServletOutputStream outputStream = outputStream();
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

	/** Handle a timeout/error callback from the Servlet container */
	void handleAsyncListenerError(Throwable ex) {
		if (this.bodyFlushProcessor != null) {
			this.bodyFlushProcessor.cancel();
			this.bodyFlushProcessor.onError(ex);
		}
		if (this.bodyProcessor != null) {
			this.bodyProcessor.cancel();
			this.bodyProcessor.onError(ex);
		}
	}

	/** Handle a complete callback from the Servlet container */
	void handleAsyncListenerComplete() {
		if (this.bodyFlushProcessor != null) {
			this.bodyFlushProcessor.cancel();
			this.bodyFlushProcessor.onComplete();
		}
		if (this.bodyProcessor != null) {
			this.bodyProcessor.cancel();
			this.bodyProcessor.onComplete();
		}
	}


	private class ResponseBodyProcessor extends AbstractResponseBodyProcessor {

		private final ServletOutputStream outputStream;

		private final int bufferSize;

		public ResponseBodyProcessor(ServletOutputStream outputStream, int bufferSize) {
			this.outputStream = outputStream;
			this.bufferSize = bufferSize;
		}

		@Override
		protected boolean isWritePossible() {
			return this.outputStream.isReady();
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

		private int writeDataBuffer(DataBuffer dataBuffer) throws IOException {
			InputStream input = dataBuffer.asInputStream();
			int bytesWritten = 0;
			byte[] buffer = new byte[this.bufferSize];
			int bytesRead = -1;
			while (this.outputStream.isReady() && (bytesRead = input.read(buffer)) != -1) {
				this.outputStream.write(buffer, 0, bytesRead);
				bytesWritten += bytesRead;
			}
			return bytesWritten;
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


	private class ResponseBodyFlushProcessor extends AbstractResponseBodyFlushProcessor {

		@Override
		protected Processor<DataBuffer, Void> createBodyProcessor() {
			try {
				bodyProcessor = new ResponseBodyProcessor(outputStream(), bufferSize);
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

}
