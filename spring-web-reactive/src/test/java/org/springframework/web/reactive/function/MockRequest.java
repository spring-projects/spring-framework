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

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.codec.BodyExtractor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Arjen Poutsma
 */
public class MockRequest<T> implements Request {

	private final HttpMethod method;

	private final URI uri;

	private final MockHeaders headers;

	private final T body;

	private final Map<String, Object> attributes;

	private final MultiValueMap<String, String> queryParams;

	private final Map<String, String> pathVariables;

	private MockRequest(HttpMethod method, URI uri,
			MockHeaders headers, T body, Map<String, Object> attributes,
			MultiValueMap<String, String> queryParams,
			Map<String, String> pathVariables) {
		this.method = method;
		this.uri = uri;
		this.headers = headers;
		this.body = body;
		this.attributes = attributes;
		this.queryParams = queryParams;
		this.pathVariables = pathVariables;
	}

	public static <T> Builder<T> builder() {
		return new BuilderImpl<T>();
	}

	@Override
	public HttpMethod method() {
		return this.method;
	}

	@Override
	public URI uri() {
		return this.uri;
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S> S body(BodyExtractor<S, ? super ServerHttpRequest> extractor){
		return (S) this.body;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <S> Optional<S> attribute(String name) {
		return Optional.ofNullable((S) this.attributes.get(name));
	}

	@Override
	public List<String> queryParams(String name) {
		return Collections.unmodifiableList(this.queryParams.get(name));
	}

	@Override
	public Map<String, String> pathVariables() {
		return Collections.unmodifiableMap(this.pathVariables);
	}

	public interface Builder<T> {

		Builder<T> method(HttpMethod method);

		Builder<T> uri(URI uri);

		Builder<T> header(String key, String value);

		Builder<T> headers(HttpHeaders headers);

		Builder<T> attribute(String name, Object value);

		Builder<T> attributes(Map<String, Object> attributes);

		Builder<T> queryParam(String key, String value);

		Builder<T> queryParams(MultiValueMap<String, String> queryParams);

		Builder<T> pathVariable(String key, String value);

		Builder<T> pathVariables(Map<String, String> pathVariables);

		MockRequest<T> body(T body);

		MockRequest<Void> build();

	}

	private static class BuilderImpl<T> implements Builder<T> {

		private HttpMethod method = HttpMethod.GET;

		private URI uri = URI.create("http://localhost");

		private MockHeaders headers = new MockHeaders(new HttpHeaders());

		private T body;

		private Map<String, Object> attributes = new LinkedHashMap<>();

		private MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

		private Map<String, String> pathVariables = new LinkedHashMap<>();

		@Override
		public Builder<T> method(HttpMethod method) {
			Assert.notNull(method, "'method' must not be null");
			this.method = method;
			return this;
		}

		@Override
		public Builder<T> uri(URI uri) {
			Assert.notNull(uri, "'uri' must not be null");
			this.uri = uri;
			return this;
		}

		@Override
		public Builder<T> header(String key, String value) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.headers.header(key, value);
			return this;
		}

		@Override
		public Builder<T> headers(HttpHeaders headers) {
			Assert.notNull(headers, "'headers' must not be null");
			this.headers = new MockHeaders(headers);
			return this;
		}

		@Override
		public Builder<T> attribute(String name, Object value) {
			Assert.notNull(name, "'name' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.attributes.put(name, value);
			return this;
		}

		@Override
		public Builder<T> attributes(Map<String, Object> attributes) {
			Assert.notNull(attributes, "'attributes' must not be null");
			this.attributes = attributes;
			return this;
		}

		@Override
		public Builder<T> queryParam(String key, String value) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.queryParams.add(key, value);
			return this;
		}

		@Override
		public Builder<T> queryParams(MultiValueMap<String, String> queryParams) {
			Assert.notNull(queryParams, "'queryParams' must not be null");
			this.queryParams = queryParams;
			return this;
		}

		@Override
		public Builder<T> pathVariable(String key, String value) {
			Assert.notNull(key, "'key' must not be null");
			Assert.notNull(value, "'value' must not be null");
			this.pathVariables.put(key, value);
			return this;
		}

		@Override
		public Builder<T> pathVariables(Map<String, String> pathVariables) {
			Assert.notNull(pathVariables, "'pathVariables' must not be null");
			this.pathVariables = pathVariables;
			return this;
		}

		@Override
		public MockRequest<T> body(T body) {
			this.body = body;
			return new MockRequest<T>(this.method, this.uri, this.headers, this.body,
					this.attributes, this.queryParams, this.pathVariables);
		}

		@Override
		public MockRequest<Void> build() {
			return new MockRequest<Void>(this.method, this.uri, this.headers, null,
					this.attributes, this.queryParams, this.pathVariables);
		}

	}

	private static class MockHeaders implements Headers {

		private final HttpHeaders headers;


		public MockHeaders(HttpHeaders headers) {
			this.headers = headers;
		}

		private HttpHeaders delegate() {
			return this.headers;
		}

		public void header(String key, String value) {
			this.headers.add(key, value);
		}

		@Override
		public List<MediaType> accept() {
			return delegate().getAccept();
		}

		@Override
		public List<Charset> acceptCharset() {
			return delegate().getAcceptCharset();
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
		public InetSocketAddress host() {
			return delegate().getHost();
		}

		@Override
		public List<HttpRange> range() {
			return delegate().getRange();
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

		private Optional<ZonedDateTime> toZonedDateTime(long date) {
			if (date != -1) {
				Instant instant = Instant.ofEpochMilli(date);
				return Optional.of(ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")));
			}
			else {
				return Optional.empty();
			}
		}
	}

}
