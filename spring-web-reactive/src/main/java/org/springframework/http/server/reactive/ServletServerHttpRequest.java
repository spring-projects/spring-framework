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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.reactivestreams.Publisher;
import reactor.Flux;

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

	private final HttpServletRequest request;

	private URI uri;

	private HttpHeaders headers;

	private final Flux<ByteBuffer> requestBodyPublisher;


	public ServletServerHttpRequest(HttpServletRequest request, Publisher<ByteBuffer> body) {
		Assert.notNull(request, "'request' must not be null.");
		Assert.notNull(body, "'body' must not be null.");
		this.request = request;
		this.requestBodyPublisher = Flux.from(body);
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
	public Flux<ByteBuffer> getBody() {
		return this.requestBodyPublisher;
	}

}
