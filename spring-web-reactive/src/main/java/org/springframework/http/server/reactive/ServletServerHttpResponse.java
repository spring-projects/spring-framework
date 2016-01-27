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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * Adapt {@link ServerHttpResponse} to the Servlet {@link HttpServletResponse}.
 *
 * @author Rossen Stoyanchev
 */
public class ServletServerHttpResponse extends AbstractServerHttpResponse {

	private final HttpServletResponse response;

	private final Function<Publisher<DataBuffer>, Mono<Void>> responseBodyWriter;


	public ServletServerHttpResponse(HttpServletResponse response,
			Function<Publisher<DataBuffer>, Mono<Void>> responseBodyWriter) {

		Assert.notNull(response, "'response' must not be null");
		Assert.notNull(responseBodyWriter, "'responseBodyWriter' must not be null");
		this.response = response;
		this.responseBodyWriter = responseBodyWriter;
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
		return this.responseBodyWriter.apply(publisher);
	}

	@Override
	protected void writeHeadersInternal() {
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
		for (String name : getHeaders().getCookies().keySet()) {
			for (HttpCookie httpCookie : getHeaders().getCookies().get(name)) {
				Cookie cookie = new Cookie(name, httpCookie.getValue());
				if (httpCookie.getMaxAge() > -1) {
					cookie.setMaxAge(httpCookie.getMaxAge());
				}
				if (httpCookie.getDomain() != null) {
					cookie.setDomain(httpCookie.getDomain());
				}
				if (httpCookie.getPath() != null) {
					cookie.setPath(httpCookie.getPath());
				}
				cookie.setSecure(httpCookie.isSecure());
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				this.response.addCookie(cookie);
			}
		}
	}

}