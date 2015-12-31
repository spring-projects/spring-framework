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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * Adapt {@link ServerHttpRequest} to the Servlet {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpRequest implements ServerHttpRequest {

	private static final int BUFFER_SIZE = 8192;

	private static final Log logger = LogFactory.getLog(ServletServerHttpRequest.class);


	private final HttpServletRequest request;

	private URI uri;

	private HttpHeaders headers;

	private final RequestBodyPublisher requestBodyPublisher;


	public ServletServerHttpRequest(HttpServletRequest request, ServletAsyncContextSynchronizer synchronizer) {
		Assert.notNull(request, "'request' must not be null.");
		this.request = request;
		this.requestBodyPublisher = new RequestBodyPublisher(synchronizer, BUFFER_SIZE);
	}


	public HttpServletRequest getServletRequest() {
		return this.request;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(getServletRequest().getMethod());
	}

	@Override
	public URI getURI() {
		if (this.uri == null) {
			try {
				this.uri = new URI(getServletRequest().getScheme(), null,
						getServletRequest().getServerName(),
						getServletRequest().getServerPort(),
						getServletRequest().getRequestURI(),
						getServletRequest().getQueryString(), null);
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException("Could not get HttpServletRequest URI: " + ex.getMessage(), ex);
			}
		}
		return this.uri;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (Enumeration<?> names = getServletRequest().getHeaderNames(); names.hasMoreElements(); ) {
				String headerName = (String) names.nextElement();
				for (Enumeration<?> headerValues = getServletRequest().getHeaders(headerName);
					 headerValues.hasMoreElements(); ) {
					String headerValue = (String) headerValues.nextElement();
					this.headers.add(headerName, headerValue);
				}
			}
			// HttpServletRequest exposes some headers as properties: we should include those if not already present
			MediaType contentType = this.headers.getContentType();
			if (contentType == null) {
				String requestContentType = getServletRequest().getContentType();
				if (StringUtils.hasLength(requestContentType)) {
					contentType = MediaType.parseMediaType(requestContentType);
					this.headers.setContentType(contentType);
				}
			}
			if (contentType != null && contentType.getCharSet() == null) {
				String requestEncoding = getServletRequest().getCharacterEncoding();
				if (StringUtils.hasLength(requestEncoding)) {
					Charset charSet = Charset.forName(requestEncoding);
					Map<String, String> params = new LinkedCaseInsensitiveMap<>();
					params.putAll(contentType.getParameters());
					params.put("charset", charSet.toString());
					MediaType newContentType = new MediaType(contentType.getType(), contentType.getSubtype(), params);
					this.headers.setContentType(newContentType);
				}
			}
			if (this.headers.getContentLength() == -1) {
				int requestContentLength = getServletRequest().getContentLength();
				if (requestContentLength != -1) {
					this.headers.setContentLength(requestContentLength);
				}
			}
		}
		return this.headers;
	}

	@Override
	public Publisher<ByteBuffer> getBody() {
		return this.requestBodyPublisher;
	}

	ReadListener getReadListener() {
		return this.requestBodyPublisher;
	}


	private static class RequestBodyPublisher implements ReadListener, Publisher<ByteBuffer> {

		private final ServletAsyncContextSynchronizer synchronizer;

		private final byte[] buffer;

		private final DemandCounter demand = new DemandCounter();

		private Subscriber<? super ByteBuffer> subscriber;

		private boolean stalled;

		private boolean cancelled;


		public RequestBodyPublisher(ServletAsyncContextSynchronizer synchronizer, int bufferSize) {
			this.synchronizer = synchronizer;
			this.buffer = new byte[bufferSize];
		}


		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			if (subscriber == null) {
				throw new NullPointerException();
			}
			else if (this.subscriber != null) {
				subscriber.onError(new IllegalStateException("Only one subscriber allowed"));
			}
			this.subscriber = subscriber;
			this.subscriber.onSubscribe(new RequestBodySubscription());
		}

		@Override
		public void onDataAvailable() throws IOException {
			if (cancelled) {
				return;
			}
			ServletInputStream input = this.synchronizer.getInputStream();
			logger.debug("onDataAvailable: " + input);

			while (true) {
				logger.debug("Demand: " + this.demand);

				if (!demand.hasDemand()) {
					stalled = true;
					break;
				}

				boolean ready = input.isReady();
				logger.debug("Input ready: " + ready + " finished: " + input.isFinished());

				if (!ready) {
					break;
				}

				int read = input.read(buffer);
				logger.debug("Input read:" + read);

				if (read == -1) {
					break;
				}
				else if (read > 0) {
					this.demand.decrement();
					byte[] copy = Arrays.copyOf(this.buffer, read);

//				logger.debug("Next: " + new String(copy, UTF_8));

					this.subscriber.onNext(ByteBuffer.wrap(copy));

				}
			}
		}

		@Override
		public void onAllDataRead() throws IOException {
			if (cancelled) {
				return;
			}
			logger.debug("All data read");
			this.synchronizer.readComplete();
			if (this.subscriber != null) {
				this.subscriber.onComplete();
			}
		}

		@Override
		public void onError(Throwable t) {
			if (cancelled) {
				return;
			}
			logger.error("RequestBodyPublisher Error", t);
			this.synchronizer.readComplete();
			if (this.subscriber != null) {
				this.subscriber.onError(t);
			}
		}

		private class RequestBodySubscription implements Subscription {

			@Override
			public void request(long n) {
				if (cancelled) {
					return;
				}
				logger.debug("Updating demand " + demand + " by " + n);

				demand.increase(n);

				logger.debug("Stalled: " + stalled);

				if (stalled) {
					stalled = false;
					try {
						onDataAvailable();
					}
					catch (IOException ex) {
						onError(ex);
					}
				}
			}

			@Override
			public void cancel() {
				if (cancelled) {
					return;
				}
				cancelled = true;
				synchronizer.readComplete();
				demand.reset();
			}
		}


		/**
		 * Small utility class for keeping track of Reactive Streams demand.
		 */
		private static final class DemandCounter {

			private final AtomicLong demand = new AtomicLong();

			/**
			 * Increases the demand by the given number
			 * @param n the positive number to increase demand by
			 * @return the increased demand
			 * @see org.reactivestreams.Subscription#request(long)
			 */
			public long increase(long n) {
				Assert.isTrue(n > 0, "'n' must be higher than 0");
				return demand.updateAndGet(d -> d != Long.MAX_VALUE ? d + n : Long.MAX_VALUE);
			}

			/**
			 * Decreases the demand by one.
			 * @return the decremented demand
			 */
			public long decrement() {
				return demand.updateAndGet(d -> d != Long.MAX_VALUE ? d - 1 : Long.MAX_VALUE);
			}

			/**
			 * Indicates whether this counter has demand, i.e. whether it is higher than 0.
			 * @return {@code true} if this counter has demand; {@code false} otherwise
			 */
			public boolean hasDemand() {
				return this.demand.get() > 0;
			}

			/**
			 * Resets this counter to 0.
			 * @see org.reactivestreams.Subscription#cancel()
			 */
			public void reset() {
				this.demand.set(0);
			}

			@Override
			public String toString() {
				return demand.toString();
			}
		}
	}

}
