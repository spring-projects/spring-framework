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
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static class RequestBodyPublisher extends AbstractResponseBodyPublisher {

		private final RequestBodyReadListener readListener =
				new RequestBodyReadListener();

		private final ServletAsyncContextSynchronizer synchronizer;

		private final DataBufferAllocator allocator;

		private final byte[] buffer;

		public RequestBodyPublisher(ServletAsyncContextSynchronizer synchronizer,
				DataBufferAllocator allocator, int bufferSize) throws IOException {
			this.synchronizer = synchronizer;
			this.allocator = allocator;
			this.buffer = new byte[bufferSize];
			synchronizer.getRequest().getInputStream().setReadListener(readListener);
		}

		@Override
		protected void noLongerStalled() {
			try {
				readListener.onDataAvailable();
			}
			catch (IOException ex) {
				readListener.onError(ex);
			}
		}

		private class RequestBodyReadListener implements ReadListener {

			@Override
			public void onDataAvailable() throws IOException {
				if (isSubscriptionCancelled()) {
					return;
				}
				logger.trace("onDataAvailable");
				ServletInputStream input = synchronizer.getRequest().getInputStream();

				while (true) {
					if (!checkSubscriptionForDemand()) {
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
						DataBuffer dataBuffer = allocator.allocateBuffer(read);
						dataBuffer.write(buffer, 0, read);

						publishOnNext(dataBuffer);
					}
				}
			}

			@Override
			public void onAllDataRead() throws IOException {
				logger.trace("All data read");
				synchronizer.readComplete();

				publishOnComplete();
			}

			@Override
			public void onError(Throwable t) {
				logger.trace("RequestBodyReadListener Error", t);
				synchronizer.readComplete();

				publishOnError(t);
			}
		}

	}
}
