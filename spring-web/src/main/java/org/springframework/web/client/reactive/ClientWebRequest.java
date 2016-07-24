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

package org.springframework.web.client.reactive;

import java.net.URI;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

/**
 * Simple container for application-level information required to perform an
 * HTTP client request.
 *
 * <p>The request body is provided through a {@code Publisher<Object>} where the
 * type of each Object is indicated through a {@link ResolvableType} which
 * subsequently is used to correctly serialize into the
 * {@code Publisher<DataBuffer>} actually written to request body.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class ClientWebRequest {

	protected final HttpMethod httpMethod;

	protected final URI url;

	protected HttpHeaders httpHeaders;

	private MultiValueMap<String, HttpCookie> cookies;

	protected Publisher<?> body;

	protected ResolvableType elementType;


	public ClientWebRequest(HttpMethod httpMethod, URI url) {
		this.httpMethod = httpMethod;
		this.url = url;
	}


	public HttpMethod getMethod() {
		return httpMethod;
	}

	public URI getUrl() {
		return url;
	}

	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}

	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

	public MultiValueMap<String, HttpCookie> getCookies() {
		return cookies;
	}

	public void setCookies(MultiValueMap<String, HttpCookie> cookies) {
		this.cookies = cookies;
	}

	public Publisher<?> getBody() {
		return body;
	}

	public void setBody(Publisher<?> body) {
		this.body = body;
	}

	public ResolvableType getElementType() {
		return elementType;
	}

	public void setElementType(ResolvableType elementType) {
		this.elementType = elementType;
	}
}