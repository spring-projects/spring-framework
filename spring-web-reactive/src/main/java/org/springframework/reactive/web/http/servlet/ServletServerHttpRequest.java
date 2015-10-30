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

package org.springframework.reactive.web.http.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.reactivestreams.Publisher;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ReactiveServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.StringUtils;

/**
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpRequest implements ReactiveServerHttpRequest {

	private final HttpServletRequest servletRequest;

	private final Publisher<ByteBuffer> requestBodyPublisher;

	private HttpHeaders headers;


	public ServletServerHttpRequest(HttpServletRequest servletRequest,
			Publisher<ByteBuffer> requestBodyPublisher) {

		Assert.notNull(servletRequest, "HttpServletRequest must not be null");
		this.servletRequest = servletRequest;
		this.requestBodyPublisher = requestBodyPublisher;
	}


	@Override
	public HttpMethod getMethod() {
		return HttpMethod.valueOf(this.servletRequest.getMethod());
	}

	@Override
	public URI getURI() {
		try {
			return new URI(this.servletRequest.getScheme(), null, this.servletRequest.getServerName(),
					this.servletRequest.getServerPort(), this.servletRequest.getRequestURI(),
					this.servletRequest.getQueryString(), null);
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not get HttpServletRequest URI: " + ex.getMessage(), ex);
		}
	}

	@Override
	public HttpHeaders getHeaders() {
		if (this.headers == null) {
			this.headers = new HttpHeaders();
			for (Enumeration<?> names = this.servletRequest.getHeaderNames(); names.hasMoreElements(); ) {
				String headerName = (String) names.nextElement();
				for (Enumeration<?> headerValues = this.servletRequest.getHeaders(headerName);
					 headerValues.hasMoreElements(); ) {
					String headerValue = (String) headerValues.nextElement();
					this.headers.add(headerName, headerValue);
				}
			}
			// HttpServletRequest exposes some headers as properties: we should include those if not already present
			MediaType contentType = this.headers.getContentType();
			if (contentType == null) {
				String requestContentType = this.servletRequest.getContentType();
				if (StringUtils.hasLength(requestContentType)) {
					contentType = MediaType.parseMediaType(requestContentType);
					this.headers.setContentType(contentType);
				}
			}
			if (contentType != null && contentType.getCharSet() == null) {
				String requestEncoding = this.servletRequest.getCharacterEncoding();
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
				int requestContentLength = this.servletRequest.getContentLength();
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

}
