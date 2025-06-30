/*
 * Copyright 2002-present the original author or authors.
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
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Adapt {@link ServerHttpRequest} to the Servlet {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 5.0
 */
class ServletServerHttpRequest extends AbstractServerHttpRequest {

	static final DataBuffer EOF_BUFFER = DefaultDataBufferFactory.sharedInstance.allocateBuffer(0);


	private final HttpServletRequest request;

	private final ServletInputStream inputStream;

	private final RequestBodyPublisher bodyPublisher;

	private final Object cookieLock = new Object();

	private final DataBufferFactory bufferFactory;

	private final int bufferSize;

	private final AsyncListener asyncListener;


	public ServletServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
			String servletPath, DataBufferFactory bufferFactory, int bufferSize)
			throws IOException, URISyntaxException {

		this(createDefaultHttpHeaders(request), request, asyncContext, servletPath, bufferFactory, bufferSize);
	}

	public ServletServerHttpRequest(HttpHeaders headers, HttpServletRequest request,
			AsyncContext asyncContext, String servletPath, DataBufferFactory bufferFactory, int bufferSize)
			throws IOException, URISyntaxException {

		super(HttpMethod.valueOf(request.getMethod()), initUri(request),
				request.getContextPath() + servletPath, initHeaders(headers, request));

		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be greater than 0");

		this.request = request;
		this.bufferFactory = bufferFactory;
		this.bufferSize = bufferSize;

		this.asyncListener = new RequestAsyncListener();

		// Tomcat expects ReadListener registration on initial thread
		this.inputStream = request.getInputStream();
		this.bodyPublisher = new RequestBodyPublisher(this.inputStream);
		this.bodyPublisher.registerReadListener();
	}


	private static HttpHeaders createDefaultHttpHeaders(HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		for (Enumeration<?> names = request.getHeaderNames(); names.hasMoreElements(); ) {
			String name = (String) names.nextElement();
			for (Enumeration<?> values = request.getHeaders(name); values.hasMoreElements(); ) {
				headers.add(name, (String) values.nextElement());
			}
		}
		return headers;
	}

	@SuppressWarnings("JavaExistingMethodCanBeUsed")
	private static URI initUri(HttpServletRequest servletRequest) {
		Assert.notNull(servletRequest, "'request' must not be null");
		String urlString = null;
		String query = null;
		boolean hasQuery = false;
		try {
			StringBuffer requestURL = servletRequest.getRequestURL();
			query = servletRequest.getQueryString();
			hasQuery = StringUtils.hasText(query);
			if (hasQuery) {
				requestURL.append('?').append(query);
			}
			urlString = requestURL.toString();
			return new URI(urlString);
		}
		catch (URISyntaxException ex) {
			if (hasQuery) {
				try {
					// Maybe malformed query, try to parse and encode it
					query = UriComponentsBuilder.fromUriString("?" + query).build().toUri().getRawQuery();
					return new URI(servletRequest.getRequestURL().toString() + "?" + query);
				}
				catch (URISyntaxException ex2) {
					try {
						// Try leaving it out
						return new URI(servletRequest.getRequestURL().toString());
					}
					catch (URISyntaxException ex3) {
						// ignore
					}
				}
			}
			throw new IllegalStateException(
					"Could not resolve HttpServletRequest as URI: " + urlString, ex);
		}
	}

	@SuppressWarnings("NullAway") // Dataflow analysis limitation
	private static HttpHeaders initHeaders(HttpHeaders headerValues, HttpServletRequest request) {

		HttpHeaders headers = null;
		MediaType contentType = null;
		if (!StringUtils.hasLength(headerValues.getFirst(HttpHeaders.CONTENT_TYPE))) {
			String requestContentType = request.getContentType();
			if (StringUtils.hasLength(requestContentType)) {
				contentType = MediaType.parseMediaType(requestContentType);
				headers = new HttpHeaders(headerValues);
				headers.setContentType(contentType);
			}
		}
		if (contentType != null && contentType.getCharset() == null) {
			String encoding = request.getCharacterEncoding();
			if (StringUtils.hasLength(encoding)) {
				Map<String, String> params = new LinkedCaseInsensitiveMap<>();
				params.putAll(contentType.getParameters());
				params.put("charset", Charset.forName(encoding).toString());
				headers.setContentType(new MediaType(contentType, params));
			}
		}
		if (headerValues.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
			int contentLength = request.getContentLength();
			if (contentLength != -1) {
				headers = (headers != null ? headers : new HttpHeaders(headerValues));
				headers.setContentLength(contentLength);
			}
		}
		return (headers != null ? headers : headerValues);
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		MultiValueMap<String, HttpCookie> httpCookies = new LinkedMultiValueMap<>();
		Cookie[] cookies;
		synchronized (this.cookieLock) {
			cookies = this.request.getCookies();
		}
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				String name = cookie.getName();
				HttpCookie httpCookie = new HttpCookie(name, cookie.getValue());
				httpCookies.add(name, httpCookie);
			}
		}
		return httpCookies;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress(this.request.getLocalAddr(), this.request.getLocalPort());
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress(this.request.getRemoteHost(), this.request.getRemotePort());
	}

	@Override
	protected @Nullable SslInfo initSslInfo() {
		X509Certificate[] certificates = getX509Certificates();
		return (certificates != null ? new DefaultSslInfo(getSslSessionId(), certificates) : null);
	}

	private @Nullable String getSslSessionId() {
		return (String) this.request.getAttribute("jakarta.servlet.request.ssl_session_id");
	}

	private X509Certificate @Nullable [] getX509Certificates() {
		return (X509Certificate[]) this.request.getAttribute("jakarta.servlet.request.X509Certificate");
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.bodyPublisher);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getNativeRequest() {
		return (T) this.request;
	}

	/**
	 * Return an {@link RequestAsyncListener} that completes the request body
	 * Publisher when the Servlet container notifies that request input has ended.
	 * The listener is not actually registered but is rather exposed for
	 * {@link ServletHttpHandlerAdapter} to ensure events are delegated.
	 */
	AsyncListener getAsyncListener() {
		return this.asyncListener;
	}

	/**
	 * Return the {@link ServletInputStream} for the current response.
	 */
	protected final ServletInputStream getInputStream() {
		return this.inputStream;
	}

	/**
	 * Read from the request body InputStream and return a DataBuffer.
	 * Invoked only when {@link ServletInputStream#isReady()} returns "true".
	 * @return a DataBuffer with data read, or
	 * {@link AbstractListenerReadPublisher#EMPTY_BUFFER} if 0 bytes were read,
	 * or {@link #EOF_BUFFER} if the input stream returned -1.
	 */
	DataBuffer readFromInputStream() throws IOException {
		DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(this.bufferSize);
		int read = -1;
		try {
			try (DataBuffer.ByteBufferIterator iterator = dataBuffer.writableByteBuffers()) {
				Assert.state(iterator.hasNext(), "No ByteBuffer available");
				ByteBuffer byteBuffer = iterator.next();
				read = this.inputStream.read(byteBuffer);
			}
			logBytesRead(read);
			if (read > 0) {
				dataBuffer.writePosition(read);
				return dataBuffer;
			}
			else if (read == -1) {
				return EOF_BUFFER;
			}
			else {
				return AbstractListenerReadPublisher.EMPTY_BUFFER;
			}
		}
		finally {
			if (read <= 0) {
				DataBufferUtils.release(dataBuffer);
			}
		}
	}

	protected final void logBytesRead(int read) {
		Log rsReadLogger = AbstractListenerReadPublisher.rsReadLogger;
		if (rsReadLogger.isTraceEnabled()) {
			rsReadLogger.trace(getLogPrefix() + "Read " + read + (read != -1 ? " bytes" : ""));
		}
	}


	private final class RequestAsyncListener implements AsyncListener {

		@Override
		public void onStartAsync(AsyncEvent event) {
		}

		@Override
		public void onTimeout(AsyncEvent event) {
			Throwable ex = event.getThrowable();
			ex = ex != null ? ex : new IllegalStateException("Async operation timeout.");
			bodyPublisher.onError(ex);
		}

		@Override
		public void onError(AsyncEvent event) {
			bodyPublisher.onError(event.getThrowable());
		}

		@Override
		public void onComplete(AsyncEvent event) {
			bodyPublisher.onAllDataRead();
		}
	}


	private class RequestBodyPublisher extends AbstractListenerReadPublisher<DataBuffer> {

		private final ServletInputStream inputStream;

		public RequestBodyPublisher(ServletInputStream inputStream) {
			super(ServletServerHttpRequest.this.getLogPrefix());
			this.inputStream = inputStream;
		}

		public void registerReadListener() throws IOException {
			this.inputStream.setReadListener(new RequestBodyPublisherReadListener());
		}

		@Override
		protected void checkOnDataAvailable() {
			if (this.inputStream.isReady() && !this.inputStream.isFinished()) {
				onDataAvailable();
			}
		}

		@Override
		protected @Nullable DataBuffer read() throws IOException {
			if (this.inputStream.isReady()) {
				DataBuffer dataBuffer = readFromInputStream();
				if (dataBuffer == EOF_BUFFER) {
					// No need to wait for container callback...
					onAllDataRead();
					dataBuffer = null;
				}
				return dataBuffer;
			}
			return null;
		}

		@Override
		protected void readingPaused() {
			// no-op
		}

		@Override
		protected void discardData() {
			// Nothing to discard since we pass data buffers on immediately..
		}


		private class RequestBodyPublisherReadListener implements ReadListener {

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

}
