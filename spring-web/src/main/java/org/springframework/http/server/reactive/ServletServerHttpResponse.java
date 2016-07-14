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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse extends AbstractServerHttpResponse {

	private final Object bodyProcessorMonitor = new Object();

	private volatile ResponseBodyProcessor bodyProcessor;

	private final HttpServletResponse response;

	private final int bufferSize;

	public ServletServerHttpResponse(HttpServletResponse response,
			DataBufferFactory dataBufferFactory, int bufferSize) throws IOException {
		super(dataBufferFactory);
		Assert.notNull(response, "'response' must not be null");
		Assert.notNull(dataBufferFactory, "'dataBufferFactory' must not be null");
		Assert.isTrue(bufferSize > 0);

		this.response = response;
		this.bufferSize = bufferSize;
	}

	public HttpServletResponse getServletResponse() {
		return this.response;
	}

	@Override
	protected void writeStatusCode() {
		HttpStatus statusCode = this.getStatusCode();
		if (statusCode != null) {
			getServletResponse().setStatus(statusCode.value());
		}
	}

	@Override
	protected Mono<Void> writeWithInternal(Publisher<DataBuffer> publisher) {
		Assert.state(this.bodyProcessor == null,
				"Response body publisher is already provided");
		try {
			synchronized (this.bodyProcessorMonitor) {
				if (this.bodyProcessor == null) {
					this.bodyProcessor = createBodyProcessor();
				}
				else {
					throw new IllegalStateException(
							"Response body publisher is already provided");
				}
			}
			return Mono.from(subscriber -> {
				publisher.subscribe(this.bodyProcessor);
				this.bodyProcessor.subscribe(subscriber);
			});
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
	}

	private ResponseBodyProcessor createBodyProcessor() throws IOException {
		ResponseBodyProcessor bodyProcessor =
				new ResponseBodyProcessor(this.response.getOutputStream(),
						this.bufferSize);
		bodyProcessor.registerListener();
		return bodyProcessor;
	}

	@Override
	protected void writeHeaders() {
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
	protected void writeCookies() {
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
				this.outputStream.flush();
				this.flushOnNext = false;
				return;
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
				ResponseBodyProcessor.this.onError(ex);
			}
		}
	}
}