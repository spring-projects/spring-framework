/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Easy builder for {@link RequestEntity}
 * @param <T> Type of body object
 * @author Andre Bonna
 */
public class RequestEntityBuilder<T> {

	private String url;

	private T body;

	private HttpMethod method;

	private Map<String, String> headers;

	private Map<String, String> queryParams;

	private Map<String, String> urlParams;

	public RequestEntityBuilder withUrl(String url) {
		this.url = url;
		return this;
	}

	public RequestEntityBuilder withBody(T body) {
		this.body = body;
		return this;
	}

	public RequestEntityBuilder withMethod(HttpMethod method) {
		this.method = method;
		return this;
	}

	public RequestEntityBuilder withHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public RequestEntityBuilder withQueryParams(Map<String, String> queryParams) {
		this.queryParams = queryParams;
		return this;
	}

	public RequestEntityBuilder withUrlParams(Map<String, String> urlParams) {
		this.urlParams = urlParams;
		return this;
	}

	/**
	 * Build {@link RequestEntity} based on internal parameters
	 * provided through chained builder methods
	 *
	 * @return the RequestEntity
	 */
	public RequestEntity<T> build() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

		// Enrich builder with queryParams
		if (queryParams != null) {
			for (Map.Entry<String, String> entry : queryParams.entrySet()) {
				builder = builder.queryParam(entry.getKey(), entry.getValue());
			}
		}

		URI uri;

		// Build with urlParams or not
		if (urlParams != null) {
			uri = builder.buildAndExpand(urlParams).toUri();
		}
		else {
			uri = builder.build().toUri();
		}

		HttpHeaders httpHeaders = new HttpHeaders();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			httpHeaders.set(entry.getKey(), entry.getValue());
		}

		return new RequestEntity<>(body, httpHeaders, method, uri);
	}
}
