/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

/**
 * Default {@link ServerResponse.BodyBuilder} implementation.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class DefaultServerResponseBuilder implements ServerResponse.BodyBuilder {

	private final HttpStatusCode statusCode;

	private final HttpHeaders headers = new HttpHeaders();

	private final MultiValueMap<String, Cookie> cookies = new LinkedMultiValueMap<>();


	public DefaultServerResponseBuilder(ServerResponse other) {
		Assert.notNull(other, "ServerResponse must not be null");
		this.statusCode = other.statusCode();
		this.headers.addAll(other.headers());
		this.cookies.addAll(other.cookies());
	}

	public DefaultServerResponseBuilder(HttpStatusCode status) {
		Assert.notNull(status, "HttpStatusCode must not be null");
		this.statusCode = status;
	}

	@Override
	public ServerResponse.BodyBuilder header(String headerName, String... headerValues) {
		Assert.notNull(headerName, "HeaderName must not be null");
		for (String headerValue : headerValues) {
			this.headers.add(headerName, headerValue);
		}
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder headers(Consumer<HttpHeaders> headersConsumer) {
		Assert.notNull(headersConsumer, "HeaderConsumer must not be null");
		headersConsumer.accept(this.headers);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookie(Cookie cookie) {
		Assert.notNull(cookie, "Cookie must not be null");
		this.cookies.add(cookie.getName(), cookie);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer) {
		Assert.notNull(cookiesConsumer, "CookiesConsumer must not be null");
		cookiesConsumer.accept(this.cookies);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(HttpMethod... allowedMethods) {
		Assert.notNull(allowedMethods, "Http AllowedMethods must not be null");
		this.headers.setAllow(new LinkedHashSet<>(Arrays.asList(allowedMethods)));
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder allow(Set<HttpMethod> allowedMethods) {
		Assert.notNull(allowedMethods, "Http AllowedMethods must not be null");
		this.headers.setAllow(allowedMethods);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentLength(long contentLength) {
		this.headers.setContentLength(contentLength);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder contentType(MediaType contentType) {
		Assert.notNull(contentType, "ContentType must not be null");
		this.headers.setContentType(contentType);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder eTag(String etag) {
		Assert.notNull(etag, "etag must not be null");
		if (!etag.startsWith("\"") && !etag.startsWith("W/\"")) {
			etag = "\"" + etag;
		}
		if (!etag.endsWith("\"")) {
			etag = etag + "\"";
		}
		this.headers.setETag(etag);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(ZonedDateTime lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder lastModified(Instant lastModified) {
		this.headers.setLastModified(lastModified);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder location(URI location) {
		this.headers.setLocation(location);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder cacheControl(CacheControl cacheControl) {
		this.headers.setCacheControl(cacheControl);
		return this;
	}

	@Override
	public ServerResponse.BodyBuilder varyBy(String... requestHeaders) {
		this.headers.setVary(Arrays.asList(requestHeaders));
		return this;
	}

	@Override
	public ServerResponse build() {
		return build((request, response) -> null);
	}

	@Override
	public ServerResponse build(WriteFunction writeFunction) {
		return new WriteFunctionResponse(this.statusCode, this.headers, this.cookies, writeFunction);
	}

	@Override
	public ServerResponse body(Object body) {
		return DefaultEntityResponseBuilder.fromObject(body)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.build();
	}

	@Override
	public <T> ServerResponse body(T body, ParameterizedTypeReference<T> bodyType) {
		return DefaultEntityResponseBuilder.fromObject(body, bodyType)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.build();
	}

	@Override
	public ServerResponse render(String name, Object... modelAttributes) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(modelAttributes)
				.build();
	}

	@Override
	public ServerResponse render(String name, Map<String, ?> model) {
		return new DefaultRenderingResponseBuilder(name)
				.status(this.statusCode)
				.headers(headers -> headers.putAll(this.headers))
				.cookies(cookies -> cookies.addAll(this.cookies))
				.modelAttributes(model)
				.build();
	}


	private static class WriteFunctionResponse extends AbstractServerResponse {

		private final WriteFunction writeFunction;

		public WriteFunctionResponse(HttpStatusCode statusCode, HttpHeaders headers, MultiValueMap<String, Cookie> cookies,
				WriteFunction writeFunction) {

			super(statusCode, headers, cookies);
			Assert.notNull(writeFunction, "WriteFunction must not be null");
			this.writeFunction = writeFunction;
		}

		@Override
		protected ModelAndView writeToInternal(HttpServletRequest request, HttpServletResponse response,
				Context context) throws Exception {

			return this.writeFunction.write(request, response);
		}
	}

}
