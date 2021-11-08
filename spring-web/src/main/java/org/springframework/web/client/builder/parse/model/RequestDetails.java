/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client.builder.parse.model;

import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

public class RequestDetails {

	private final HttpMethod requestMethod;
	private final String requestPath;
	private final MediaType consumes;
	private final MediaType produces;
	private final HttpHeaders httpHeaders;

	public RequestDetails(
			final HttpMethod requestMethod,
			final String requestPath,
			final MediaType consumes,
			final MediaType produces,
			final HttpHeaders httpHeaders) {
		this.requestMethod = requestMethod;
		this.requestPath = requestPath;
		this.consumes = consumes;
		this.produces = produces;
		this.httpHeaders = httpHeaders;
	}

	public Optional<MediaType> findConsumes() {
		return Optional.ofNullable(this.consumes);
	}

	public Optional<MediaType> findProduces() {
		return Optional.ofNullable(this.produces);
	}

	public HttpMethod getRequestMethod() {
		return this.requestMethod;
	}

	public String getRequestPath() {
		return this.requestPath;
	}

	public HttpHeaders getHttpHeaders() {
		return this.httpHeaders;
	}

	@Override
	public String toString() {
		return "RequestDetails [requestMethod="
				+ this.requestMethod
				+ ", requestPath="
				+ this.requestPath
				+ ", consumes="
				+ this.consumes
				+ ", produces="
				+ this.produces
				+ ", httpHeaders="
				+ this.httpHeaders
				+ "]";
	}
}
