/*
 * Copyright 2002-2015 the original author or authors.
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
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.Publishers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse implements ServerHttpResponse {

	private static final Log logger = LogFactory.getLog(ServletServerHttpResponse.class);


	private final HttpServletResponse response;

	private final HttpHeaders headers;

	private final ResponseBodySubscriber subscriber;

	private boolean headersWritten = false;


	public ServletServerHttpResponse(HttpServletResponse response, ServletAsyncContextSynchronizer synchronizer) {
		Assert.notNull(response, "'response' must not be null");
		this.response = response;
		this.headers = new HttpHeaders();
		this.subscriber = new ResponseBodySubscriber(synchronizer);
	}


	@Override
	public void setStatusCode(HttpStatus status) {
		this.response.setStatus(status.value());
	}

	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	WriteListener getWriteListener() {
		return this.subscriber;
	}

	@Override
	public Publisher<Void> writeHeaders() {
		applyHeaders();
		return Publishers.empty();
	}

	@Override
	public Publisher<Void> setBody(final Publisher<ByteBuffer> publisher) {
		return Publishers.lift(publisher, new WriteWithOperator<>(writePublisher -> {
			applyHeaders();
			return (s -> writePublisher.subscribe(subscriber));
		}));
	}

	private void applyHeaders() {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				String headerName = entry.getKey();
				for (String headerValue : entry.getValue()) {
					this.response.addHeader(headerName, headerValue);
				}
			}
			MediaType contentType = this.headers.getContentType();
			if (this.response.getContentType() == null && contentType != null) {
				this.response.setContentType(contentType.toString());
			}
			Charset charset = (contentType != null ? contentType.getCharSet() : null);
			if (this.response.getCharacterEncoding() == null && charset != null) {
				this.response.setCharacterEncoding(charset.name());
			}
			this.headersWritten = true;
		}
	}


	private static class ResponseBodySubscriber implements WriteListener, Subscriber<ByteBuffer> {

		private final ServletAsyncContextSynchronizer synchronizer;

		private Subscription subscription;

		private ByteBuffer buffer;

		private volatile boolean subscriberComplete = false;


		public ResponseBodySubscriber(ServletAsyncContextSynchronizer synchronizer) {
			this.synchronizer = synchronizer;
		}


		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
			this.subscription.request(1);
		}

		@Override
		public void onNext(ByteBuffer bytes) {

			Assert.isNull(buffer);

			this.buffer = bytes;
			try {
				onWritePossible();
			}
			catch (IOException e) {
				onError(e);
			}
		}

		@Override
		public void onComplete() {
			logger.debug("Complete buffer: " + (buffer == null));

			this.subscriberComplete = true;

			if (buffer == null) {
				this.synchronizer.writeComplete();
			}
		}

		@Override
		public void onWritePossible() throws IOException {
			ServletOutputStream output = this.synchronizer.getOutputStream();

			boolean ready = output.isReady();
			logger.debug("Output: " + ready + " buffer: " + (buffer == null));

			if (ready) {
				if (this.buffer != null) {
					byte[] bytes = new byte[this.buffer.remaining()];
					this.buffer.get(bytes);
					this.buffer = null;
					output.write(bytes);
					if (!subscriberComplete) {
						this.subscription.request(1);
					}
					else {
						this.synchronizer.writeComplete();
					}
				}
				else {
					this.subscription.request(1);
				}
			}
		}

		@Override
		public void onError(Throwable t) {
			logger.error("ResponseBodySubscriber error", t);
		}
	}

}
