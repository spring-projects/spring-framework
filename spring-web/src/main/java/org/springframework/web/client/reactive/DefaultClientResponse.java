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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.BodyExtractor;
import org.springframework.http.codec.HttpMessageReader;

/**
 * Default implementation of {@link ClientResponse}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultClientResponse implements ClientResponse {

	private final ClientHttpResponse response;

	private final Headers headers;

	private final WebClientStrategies strategies;


	public DefaultClientResponse(ClientHttpResponse response, WebClientStrategies strategies) {
		this.response = response;
		this.strategies = strategies;
		this.headers = new DefaultHeaders();
	}

	@Override
	public HttpStatus statusCode() {
		return this.response.getStatusCode();
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		return extractor.extract(this.response, new BodyExtractor.Context() {
			@Override
			public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
				return strategies.messageReaders();
			}
		});
	}

	public ClientHttpResponse clientHttpResponse() {
		return this.response;
	}

	private class DefaultHeaders implements Headers {

		private HttpHeaders delegate() {
			return response.getHeaders();
		}

		@Override
		public OptionalLong contentLength() {
			return toOptionalLong(delegate().getContentLength());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(delegate().getContentType());
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = delegate().get(headerName);
			return headerValues != null ? headerValues : Collections.emptyList();
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return HttpHeaders.readOnlyHttpHeaders(delegate());
		}

		private OptionalLong toOptionalLong(long value) {
			return value != -1 ? OptionalLong.of(value) : OptionalLong.empty();
		}

	}
}
