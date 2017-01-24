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
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import reactor.core.publisher.Flux;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * Adapt {@link ServerHttpRequest} to the Servlet {@link HttpServletRequest}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ServletServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServletRequest request;

	private final RequestBodyPublisher bodyPublisher;

	private final Object cookieLock = new Object();

	private final DataBufferFactory bufferFactory;

	private final byte[] buffer;

	protected final Log logger = LogFactory.getLog(getClass());


	public ServletServerHttpRequest(HttpServletRequest request, AsyncContext asyncContext,
			DataBufferFactory bufferFactory, int bufferSize) throws IOException {

		super(initUri(request), initHeaders(request));

		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.isTrue(bufferSize > 0, "'bufferSize' must be higher than 0");

		this.request = request;
		this.bufferFactory = bufferFactory;
		this.buffer = new byte[bufferSize];

		asyncContext.addListener(new RequestAsyncListener());

		// Tomcat expects ReadListener registration on initial thread
		ServletInputStream inputStream = request.getInputStream();
		this.bodyPublisher = new RequestBodyPublisher(inputStream);
		this.bodyPublisher.registerReadListener();
	}


	private static URI initUri(HttpServletRequest request) {
		Assert.notNull(request, "'request' must not be null");
		try {
			StringBuffer url = request.getRequestURL();
			String query = request.getQueryString();
			if (StringUtils.hasText(query)) {
				url.append('?').append(query);
			}
			return new URI(url.toString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get URI: " + ex.getMessage(), ex);
		}
	}

	private static HttpHeaders initHeaders(HttpServletRequest request) {
		HttpHeaders headers = new HttpHeaders();
		for (Enumeration<?> names = request.getHeaderNames();
			 names.hasMoreElements(); ) {
			String name = (String) names.nextElement();
			for (Enumeration<?> values = request.getHeaders(name);
				 values.hasMoreElements(); ) {
				headers.add(name, (String) values.nextElement());
			}
		}
		MediaType contentType = headers.getContentType();
		if (contentType == null) {
			String requestContentType = request.getContentType();
			if (StringUtils.hasLength(requestContentType)) {
				contentType = MediaType.parseMediaType(requestContentType);
				headers.setContentType(contentType);
			}
		}
		if (contentType != null && contentType.getCharset() == null) {
			String encoding = request.getCharacterEncoding();
			if (StringUtils.hasLength(encoding)) {
				Charset charset = Charset.forName(encoding);
				Map<String, String> params = new LinkedCaseInsensitiveMap<>();
				params.putAll(contentType.getParameters());
				params.put("charset", charset.toString());
				headers.setContentType(
						new MediaType(contentType.getType(), contentType.getSubtype(),
								params));
			}
		}
		if (headers.getContentLength() == -1) {
			int contentLength = request.getContentLength();
			if (contentLength != -1) {
				headers.setContentLength(contentLength);
			}
		}
		return headers;
	}


	public HttpServletRequest getServletRequest() {
		return this.request;
	}

	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(getServletRequest().getMethod());
	}

	@Override
	public String getContextPath() {
		return getServletRequest().getContextPath();
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
	public Flux<DataBuffer> getBody() {
		return Flux.from(this.bodyPublisher);
	}

	protected DataBuffer readDataBuffer() throws IOException {
		int read = this.request.getInputStream().read(this.buffer);
		if (logger.isTraceEnabled()) {
			logger.trace("read:" + read);
		}

		if (read > 0) {
			DataBuffer dataBuffer = this.bufferFactory.allocateBuffer(read);
			dataBuffer.write(this.buffer, 0, read);
			return dataBuffer;
		}

		return null;
	}


	private final class RequestAsyncListener implements AsyncListener {

		@Override
		public void onStartAsync(AsyncEvent event) {}

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

			this.inputStream = inputStream;
		}

		public void registerReadListener() throws IOException {
			this.inputStream.setReadListener(new RequestBodyPublisherReadListener());
		}

		@Override
		protected void checkOnDataAvailable() {
			if (!this.inputStream.isFinished() && this.inputStream.isReady()) {
				onDataAvailable();
			}
		}

		@Override
		protected DataBuffer read() throws IOException {
			if (this.inputStream.isReady()) {
				return readDataBuffer();
			}
			return null;
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
