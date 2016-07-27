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

package org.springframework.web.reactive.function;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * @author Arjen Poutsma
 */
abstract class AbstractResponse<T> implements Response<T> {

	private final int statusCode;

	private final HttpHeaders headers;

	protected AbstractResponse(
			int statusCode, HttpHeaders headers) {
		this.statusCode = statusCode;
		this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
	}

	@Override
	public HttpStatus statusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	@Override
	public HttpHeaders headers() {
		return this.headers;
	}

	protected void writeStatusAndHeaders(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.valueOf(this.statusCode));
		HttpHeaders responseHeaders = response.getHeaders();

		if (!this.headers.isEmpty()) {
			this.headers.entrySet().stream()
						.filter(entry -> !responseHeaders.containsKey(entry.getKey()))
						.forEach(entry -> responseHeaders
								.put(entry.getKey(), entry.getValue()));
		}
	}
}
