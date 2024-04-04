/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.service.invoker;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilderFactory;

/**
 * {@link HttpRequestValues} extension for use with {@link ReactorHttpExchangeAdapter}.
 *
 * @author Rossen Stoyanchev
 * @author Olga Maciaszek-Sharma
 * @since 6.1
 */
public final class ReactiveHttpRequestValues extends HttpRequestValues {

	@Nullable
	private final Publisher<?> body;

	@Nullable
	private final ParameterizedTypeReference<?> bodyElementType;


	private ReactiveHttpRequestValues(
			@Nullable HttpMethod httpMethod,
			@Nullable URI uri, @Nullable UriBuilderFactory uriBuilderFactory,
			@Nullable String uriTemplate, Map<String, String> uriVars,
			HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
			@Nullable Object bodyValue, @Nullable Publisher<?> body, @Nullable ParameterizedTypeReference<?> elementType) {

		super(httpMethod, uri, uriBuilderFactory, uriTemplate, uriVars, headers, cookies, attributes, bodyValue);
		this.body = body;
		this.bodyElementType = elementType;
	}


	/**
	 * Return a {@link Publisher} that will produce the request body.
	 * <p>This is mutually exclusive with {@link #getBodyValue()}.
	 * Only one of the two or neither is set.
	 */
	@Nullable
	public Publisher<?> getBodyPublisher() {
		return this.body;
	}

	/**
	 * Return the element type for a {@linkplain #getBodyPublisher() body publisher}.
	 */
	@Nullable
	public ParameterizedTypeReference<?> getBodyPublisherElementType() {
		return this.bodyElementType;
	}

	/**
	 * Return the request body as a {@link Publisher}.
	 * <p>This is mutually exclusive with {@link #getBodyValue()}.
	 * Only one of the two or neither is set.
	 */
	@Override
	@SuppressWarnings("removal")
	@Nullable
	public Publisher<?> getBody() {
		return getBodyPublisher();
	}

	/**
	 * Return the element type for a {@linkplain #getBodyPublisher() body publisher}.
	 */
	@Override
	@SuppressWarnings("removal")
	@Nullable
	public ParameterizedTypeReference<?> getBodyElementType() {
		return getBodyPublisherElementType();
	}


	public static Builder builder() {
		return new Builder();
	}


	/**
	 * Builder for {@link ReactiveHttpRequestValues}.
	 */
	public static final class Builder extends HttpRequestValues.Builder {

		@Nullable
		private MultipartBodyBuilder multipartBuilder;

		@Nullable
		private Publisher<?> body;

		@Nullable
		private ParameterizedTypeReference<?> bodyElementType;

		@Override
		public Builder setHttpMethod(HttpMethod httpMethod) {
			super.setHttpMethod(httpMethod);
			return this;
		}

		@Override
		public Builder setUri(URI uri) {
			super.setUri(uri);
			return this;
		}

		@Override
		public Builder setUriBuilderFactory(@Nullable UriBuilderFactory uriBuilderFactory) {
			super.setUriBuilderFactory(uriBuilderFactory);
			return this;
		}

		@Override
		public Builder setUriTemplate(String uriTemplate) {
			super.setUriTemplate(uriTemplate);
			return this;
		}

		@Override
		public Builder setUriVariable(String name, String value) {
			super.setUriVariable(name, value);
			return this;
		}

		@Override
		public Builder setAccept(List<MediaType> acceptableMediaTypes) {
			super.setAccept(acceptableMediaTypes);
			return this;
		}

		@Override
		public Builder setContentType(MediaType contentType) {
			super.setContentType(contentType);
			return this;
		}

		@Override
		public Builder addHeader(String headerName, String... headerValues) {
			super.addHeader(headerName, headerValues);
			return this;
		}

		@Override
		public Builder addCookie(String name, String... values) {
			super.addCookie(name, values);
			return this;
		}

		@Override
		public Builder addRequestParameter(String name, String... values) {
			super.addRequestParameter(name, values);
			return this;
		}

		@Override
		public Builder addAttribute(String name, Object value) {
			super.addAttribute(name, value);
			return this;
		}

		/**
		 * Add a part to a multipart request. The part value may be as described
		 * in {@link MultipartBodyBuilder#part(String, Object)}.
		 */
		@Override
		public Builder addRequestPart(String name, Object part) {
			this.multipartBuilder = (this.multipartBuilder != null ? this.multipartBuilder : new MultipartBodyBuilder());
			this.multipartBuilder.part(name, part);
			return this;
		}

		/**
		 * Variant of {@link #addRequestPart(String, Object)} that allows the
		 * part value to be produced by a {@link Publisher}.
		 */
		public <T, P extends Publisher<T>> Builder addRequestPartPublisher(
				String name, P publisher, ParameterizedTypeReference<T> elementTye) {

			this.multipartBuilder = (this.multipartBuilder != null ? this.multipartBuilder : new MultipartBodyBuilder());
			this.multipartBuilder.asyncPart(name, publisher, elementTye);
			return this;
		}

		@SuppressWarnings("removal")
		@Override
		public <T, P extends Publisher<T>> Builder addRequestPart(String name, P publisher, ResolvableType type) {
			return addRequestPartPublisher(name, publisher, ParameterizedTypeReference.forType(type.getType()));
		}

		/**
		 * {@inheritDoc}
		 * <p>This is mutually exclusive with and resets any previously set
		 * {@linkplain #setBodyPublisher(Publisher, ParameterizedTypeReference)
		 * body publisher}.
		 */
		@Override
		public void setBodyValue(@Nullable Object bodyValue) {
			super.setBodyValue(bodyValue);
			this.body = null;
			this.bodyElementType = null;
		}

		/**
		 * Set the request body as a Reactive Streams {@link Publisher}.
		 * <p>This is mutually exclusive with and resets any previously set
		 * {@linkplain #setBodyValue(Object) body value}.
		 */
		@SuppressWarnings("DataFlowIssue")
		public <T, P extends Publisher<T>> void setBodyPublisher(P body, ParameterizedTypeReference<T> elementTye) {
			this.body = body;
			this.bodyElementType = elementTye;
			super.setBodyValue(null);
		}

		@SuppressWarnings("removal")
		@Override
		public <T, P extends Publisher<T>> void setBody(P body, ParameterizedTypeReference<T> elementTye) {
			setBodyPublisher(body, elementTye);
		}

		@Override
		public ReactiveHttpRequestValues build() {
			return (ReactiveHttpRequestValues) super.build();
		}

		@Override
		protected boolean hasParts() {
			return (this.multipartBuilder != null);
		}

		@Override
		protected boolean hasBody() {
			return (super.hasBody() || this.body != null);
		}

		@Override
		protected Object buildMultipartBody() {
			Assert.notNull(this.multipartBuilder, "`multipartBuilder` is null, was hasParts() not called?");
			return this.multipartBuilder.build();
		}

		@Override
		protected ReactiveHttpRequestValues createRequestValues(
				@Nullable HttpMethod httpMethod,
				@Nullable URI uri, @Nullable UriBuilderFactory uriBuilderFactory,
				@Nullable String uriTemplate, Map<String, String> uriVars,
				HttpHeaders headers, MultiValueMap<String, String> cookies, Map<String, Object> attributes,
				@Nullable Object bodyValue) {

			return new ReactiveHttpRequestValues(
					httpMethod, uri, uriBuilderFactory, uriTemplate, uriVars,
					headers, cookies, attributes, bodyValue, this.body, this.bodyElementType);
		}
	}

}
