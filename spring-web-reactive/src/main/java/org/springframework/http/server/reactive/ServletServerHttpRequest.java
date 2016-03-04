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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferAllocator;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Adapt {@link ServerHttpRequest} to the Servlet {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpRequest extends AbstractServerHttpRequest {

	private static final Log logger = LogFactory.getLog(ServletServerHttpRequest.class);

	private final HttpServletRequest request;

	private final Flux<DataBuffer> requestBodyPublisher;

	public ServletServerHttpRequest(ServletAsyncContextSynchronizer synchronizer,
			DataBufferAllocator allocator, int bufferSize) throws IOException {
		Assert.notNull(synchronizer, "'synchronizer' must not be null");
		Assert.notNull(allocator, "'allocator' must not be null");

		this.request = (HttpServletRequest) synchronizer.getRequest();
		RequestBodyPublisher bodyPublisher =
				new RequestBodyPublisher(synchronizer, allocator, bufferSize);
		this.requestBodyPublisher = Flux.from(bodyPublisher);
		this.request.getInputStream().setReadListener(bodyPublisher);
	}


	public HttpServletRequest getServletRequest() {
		return this.request;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(getServletRequest().getMethod());
	}

	@Override
	protected URI initUri() throws URISyntaxException {
		StringBuffer url = this.request.getRequestURL();
		String query = this.request.getQueryString();
		if (StringUtils.hasText(query)) {
			url.append('?').append(query);
		}
		return new URI(url.toString());
	}

	@Override
	protected void initHeaders(HttpHeaders headers) {
		for (Enumeration<?> names = getServletRequest().getHeaderNames(); names.hasMoreElements(); ) {
			String name = (String) names.nextElement();
			for (Enumeration<?> values = getServletRequest().getHeaders(name); values.hasMoreElements(); ) {
				headers.add(name, (String) values.nextElement());
			}
		}
		MediaType contentType = headers.getContentType();
		if (contentType == null) {
			String requestContentType = getServletRequest().getContentType();
			if (StringUtils.hasLength(requestContentType)) {
				contentType = MediaType.parseMediaType(requestContentType);
				headers.setContentType(contentType);
			}
		}
		if (contentType != null && contentType.getCharSet() == null) {
			String encoding = getServletRequest().getCharacterEncoding();
			if (StringUtils.hasLength(encoding)) {
				Charset charset = Charset.forName(encoding);
				Map<String, String> params = new LinkedCaseInsensitiveMap<>();
				params.putAll(contentType.getParameters());
				params.put("charset", charset.toString());
				headers.setContentType(new MediaType(contentType.getType(), contentType.getSubtype(), params));
			}
		}
		if (headers.getContentLength() == -1) {
			int contentLength = getServletRequest().getContentLength();
			if (contentLength != -1) {
				headers.setContentLength(contentLength);
			}
		}
	}

	@Override
	protected void initCookies(MultiValueMap<String, HttpCookie> httpCookies) {
		Cookie[] cookies = this.request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String name = cookie.getName();
				HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
				httpCookies.add(name, httpCookie);
			}
		}
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.requestBodyPublisher;
	}

	private static class RequestBodyPublisher
			implements ReadListener, Publisher<DataBuffer> {

		private final ServletAsyncContextSynchronizer synchronizer;

		private final DataBufferAllocator allocator;

		private final byte[] buffer;

		private final DemandCounter demand = new DemandCounter();

		private Subscriber<? super DataBuffer> subscriber;

		private boolean stalled;

		private boolean cancelled;

		public RequestBodyPublisher(ServletAsyncContextSynchronizer synchronizer,
				DataBufferAllocator allocator, int bufferSize) {
			this.synchronizer = synchronizer;
			this.allocator = allocator;
			this.buffer = new byte[bufferSize];
		}

		@Override
		public void subscribe(Subscriber<? super DataBuffer> subscriber) {
			if (subscriber == null) {
				throw new NullPointerException();
			}
			else if (this.subscriber != null) {
				subscriber.onError(
						new IllegalStateException("Only one subscriber allowed"));
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
			logger.trace("onDataAvailable: " + input);

			while (true) {
				logger.trace("Demand: " + this.demand);

				if (!demand.hasDemand()) {
					stalled = true;
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
					this.demand.decrement();

					DataBuffer dataBuffer = allocator.allocateBuffer(read);
					dataBuffer.write(this.buffer, 0, read);

					this.subscriber.onNext(dataBuffer);

				}
			}
		}

		@Override
		public void onAllDataRead() throws IOException {
			if (cancelled) {
				return;
			}
			logger.trace("All data read");
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
			logger.trace("RequestBodyPublisher Error", t);
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
				logger.trace("Updating demand " + demand + " by " + n);

				demand.increase(n);

				logger.trace("Stalled: " + stalled);

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
			 * @see Subscription#request(long)
			 */
			public long increase(long n) {
				Assert.isTrue(n > 0, "'n' must be higher than 0");
				return demand
						.updateAndGet(d -> d != Long.MAX_VALUE ? d + n : Long.MAX_VALUE);
			}

			/**
			 * Decreases the demand by one.
			 * @return the decremented demand
			 */
			public long decrement() {
				return demand
						.updateAndGet(d -> d != Long.MAX_VALUE ? d - 1 : Long.MAX_VALUE);
			}

			/**
			 * Indicates whether this counter has demand, i.e. whether it is higher than
			 * 0.
			 * @return {@code true} if this counter has demand; {@code false} otherwise
			 */
			public boolean hasDemand() {
				return this.demand.get() > 0;
			}

			/**
			 * Resets this counter to 0.
			 * @see Subscription#cancel()
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
