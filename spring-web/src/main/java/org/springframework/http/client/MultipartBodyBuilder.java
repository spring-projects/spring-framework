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

package org.springframework.http.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A mutable builder for multipart form bodies. For example:
 * <pre class="code">
 *
 * MultipartBodyBuilder builder = new MultipartBodyBuilder();
 * builder.part("form field", "form value");
 *
 * Resource image = new ClassPathResource("image.jpg");
 * builder.part("image", image).header("Baz", "Qux");
 *
 * MultiValueMap<String, HttpEntity<?>> multipartBody = builder.build();
 * // use multipartBody with RestTemplate or WebClient
 * </pre>

 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0.2
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>
 */
public final class MultipartBodyBuilder {

	private final LinkedMultiValueMap<String, DefaultPartBuilder> parts = new LinkedMultiValueMap<>();


	/**
	 * Creates a new, empty instance of the {@code MultipartBodyBuilder}.
	 */
	public MultipartBodyBuilder() {
	}


	/**
	 * Add a part from an Object.
	 * @param name the name of the part to add
	 * @param part the part data
	 * @return builder that allows for further customization of part headers
	 */
	public PartBuilder part(String name, Object part) {
		return part(name, part, null);
	}

	/**
	 * Variant of {@link #part(String, Object)} that also accepts a MediaType
	 * which is used to determine how to encode the part.
	 * @param name the name of the part to add
	 * @param part the part data
	 * @param contentType the media type for the part
	 * @return builder that allows for further customization of part headers
	 */
	public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(part, "'part' must not be null");

		if (part instanceof Publisher) {
			throw new IllegalArgumentException("Use publisher(String, Publisher, Class) or " +
				"publisher(String, Publisher, ParameterizedTypeReference) for adding Publisher parts");
		}

		if (part instanceof PublisherEntity<?,?>) {
			PublisherPartBuilder<?, ?> builder = new PublisherPartBuilder<>((PublisherEntity<?, ?>) part);
			this.parts.add(name, builder);
			return builder;
		}

		Object partBody;
		HttpHeaders partHeaders = new HttpHeaders();

		if (part instanceof HttpEntity) {
			HttpEntity<?> other = (HttpEntity<?>) part;
			partBody = other.getBody();
			partHeaders.addAll(other.getHeaders());
		}
		else {
			partBody = part;
		}

		if (contentType != null) {
			partHeaders.setContentType(contentType);
		}

		DefaultPartBuilder builder = new DefaultPartBuilder(partHeaders, partBody);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * Add an asynchronous part with {@link Publisher}-based content.
	 * @param name the name of the part to add
	 * @param publisher the part contents
	 * @param elementClass the type of elements contained in the publisher
	 * @return builder that allows for further customization of part headers
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, Class<T> elementClass) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");

		HttpHeaders headers = new HttpHeaders();
		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(headers, publisher, elementClass);
		this.parts.add(name, builder);
		return builder;

	}

	/**
	 * Variant of {@link #asyncPart(String, Publisher, Class)} that accepts a
	 * {@link ParameterizedTypeReference} for the element type, which allows
	 * specifying generic type information.
	 * @param name the name of the part to add
	 * @param publisher the part contents
	 * @param typeReference the type of elements contained in the publisher
	 * @return builder that allows for further customization of part headers
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(
			String name, P publisher, ParameterizedTypeReference<T> typeReference) {

		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(typeReference, "'typeReference' must not be null");

		HttpHeaders headers = new HttpHeaders();
		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(headers, publisher, typeReference);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * Return a {@code MultiValueMap} with the configured parts.
	 */
	public MultiValueMap<String, HttpEntity<?>> build() {
		MultiValueMap<String, HttpEntity<?>> result = new LinkedMultiValueMap<>(this.parts.size());
		for (Map.Entry<String, List<DefaultPartBuilder>> entry : this.parts.entrySet()) {
			for (DefaultPartBuilder builder : entry.getValue()) {
				HttpEntity<?> entity = builder.build();
				result.add(entry.getKey(), entity);
			}
		}
		return result;
	}


	/**
	 * Builder that allows for further customization of part headers.
	 */
	public interface PartBuilder {

		/**
		 * Add part header values.
		 * @param headerName the part header name
		 * @param headerValues the part header value(s)
		 * @return this builder
		 * @see HttpHeaders#addAll(String, List)
		 */
		PartBuilder header(String headerName, String... headerValues);

		/**
		 * Manipulate the part headers through the given consumer.
		 * @param headersConsumer consumer to manipulate the part headers with
		 * @return this builder
		 */
		PartBuilder headers(Consumer<HttpHeaders> headersConsumer);
	}


	private static class DefaultPartBuilder implements PartBuilder {

		protected final HttpHeaders headers;

		@Nullable
		protected final Object body;

		public DefaultPartBuilder(HttpHeaders headers, @Nullable Object body) {
			this.headers = headers;
			this.body = body;
		}

		@Override
		public PartBuilder header(String headerName, String... headerValues) {
			this.headers.addAll(headerName, Arrays.asList(headerValues));
			return this;
		}

		@Override
		public PartBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(this.headers);
			return this;
		}

		public HttpEntity<?> build() {
			return new HttpEntity<>(this.body, this.headers);
		}
	}


	private static class PublisherPartBuilder<S, P extends Publisher<S>> extends DefaultPartBuilder {

		private final ResolvableType resolvableType;

		public PublisherPartBuilder(HttpHeaders headers, P body, Class<S> elementClass) {
			super(headers, body);
			this.resolvableType = ResolvableType.forClass(elementClass);
		}

		public PublisherPartBuilder(HttpHeaders headers, P body, ParameterizedTypeReference<S> typeReference) {
			super(headers, body);
			this.resolvableType = ResolvableType.forType(typeReference);
		}

		public PublisherPartBuilder(PublisherEntity<S, P> other) {
			super(other.getHeaders(), other.getBody());
			this.resolvableType = other.getResolvableType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public HttpEntity<?> build() {
			P publisher = (P) this.body;
			Assert.state(publisher != null, "Publisher must not be null");
			return new PublisherEntity<>(this.headers, publisher, this.resolvableType);
		}
	}


	/**
	 * Specialization of {@link HttpEntity} for use with a
	 * {@link Publisher}-based body, for which we also need to keep track of
	 * the element type.
	 * @param <T> The type contained in the publisher
	 * @param <P> The publisher
	 */
	public static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P> {

		private final ResolvableType resolvableType;


		private PublisherEntity(@Nullable MultiValueMap<String, String> headers, P publisher,
				ResolvableType resolvableType) {

			super(publisher, headers);
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(resolvableType, "'resolvableType' must not be null");
			this.resolvableType = resolvableType;
		}

		/**
		 * Return the element type for the {@code Publisher} body.
		 */
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}
	}

}
