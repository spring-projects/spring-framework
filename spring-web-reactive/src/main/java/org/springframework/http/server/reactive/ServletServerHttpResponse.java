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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;
import reactor.core.util.BackpressureUtils;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse extends AbstractServerHttpResponse {

	private static final Log logger = LogFactory.getLog(ServletServerHttpResponse.class);

	private final HttpServletResponse response;

	private final ResponseBodySubscriber responseBodySubscriber;

	public ServletServerHttpResponse(ServletAsyncContextSynchronizer synchronizer,
			int bufferSize, DataBufferAllocator allocator) throws IOException {
		super(allocator);
		Assert.notNull(synchronizer, "'synchronizer' must not be null");

		this.response = (HttpServletResponse) synchronizer.getResponse();
		this.responseBodySubscriber =
				new ResponseBodySubscriber(synchronizer, bufferSize);
	}

	public HttpServletResponse getServletResponse() {
		return this.response;
	}

	@Override
	public void setStatusCode(HttpStatus status) {
		getServletResponse().setStatus(status.value());
	}

	@Override
	protected Mono<Void> setBodyInternal(Publisher<DataBuffer> publisher) {
		return Mono.from((Publisher<Void>) subscriber -> publisher
				.subscribe(this.responseBodySubscriber));
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
		Charset charset = (contentType != null ? contentType.getCharSet() : null);
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
				if (httpCookie.getDomain().isPresent()) {
					cookie.setDomain(httpCookie.getDomain().get());
				}
				if (httpCookie.getPath().isPresent()) {
					cookie.setPath(httpCookie.getPath().get());
				}
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.response.addCookie(cookie);
			}
		}
	}

	private static class ResponseBodySubscriber implements Subscriber<DataBuffer> {

		private final ResponseBodyWriteListener writeListener =
				new ResponseBodyWriteListener();

		private final ServletAsyncContextSynchronizer synchronizer;

		private final int bufferSize;

		private volatile DataBuffer dataBuffer;

		private volatile boolean completed = false;

		private Subscription subscription;

		public ResponseBodySubscriber(ServletAsyncContextSynchronizer synchronizer,
				int bufferSize) throws IOException {
			this.synchronizer = synchronizer;
			this.bufferSize = bufferSize;
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
				logger.trace("ready: " + ready + " buffer: " + dataBuffer);

				if (ready) {
					if (dataBuffer != null) {

						int total = dataBuffer.readableByteCount();
						int written = writeDataBuffer();

						logger.trace("written: " + written + " total: " + total);
						if (written == total) {
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

			private void releaseBuffer() {
				// TODO: call PooledDataBuffer.release() when we it is introduced
				dataBuffer = null;
			}

			@Override
			public void onError(Throwable ex) {
				logger.error("ResponseBodyWriteListener error", ex);
			}
		}
	}
}