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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.http.HttpCookie;
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
public class ServletServerHttpRequest extends AbstractServerHttpRequest {

	private final HttpServletRequest request;

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
	protected void initCookies(Map<String, List<HttpCookie>> map) {
		for (Cookie cookie : this.request.getCookies()) {
			String name = cookie.getName();
			List<HttpCookie> list = map.get(name);
			if (list == null) {
				list = new ArrayList<>();
				map.put(name, list);
			}
			list.add(HttpCookie.clientCookie(name, cookie.getValue()));
		}
	}

	@Override
	public Flux<ByteBuffer> getBody() {
		return this.requestBodyPublisher;
	}

}
