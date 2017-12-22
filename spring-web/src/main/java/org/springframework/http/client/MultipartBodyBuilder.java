/*
 * Copyright 2002-2017 the original author or authors.
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
	 * Builds the multipart body.
	 * @return the built body
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
	 * Adds a part to this builder, allowing for further header customization with the returned
	 * {@link PartBuilder}.
	 * @param name the name of the part to add (may not be empty)
	 * @param part the part to add
	 * @return a builder that allows for further header customization
	 */
	public PartBuilder part(String name, Object part) {
		return part(name, part, null);
	}

	/**
	 * Adds a part to this builder, allowing for further header customization with the returned
	 * {@link PartBuilder}.
	 * @param name the name of the part to add (may not be empty)
	 * @param part the part to add
	 * @param contentType the {@code Content-Type} header for the part (may be {@code null})
	 * @return a builder that allows for further header customization
	 */
	public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(part, "'part' must not be null");

		if (part instanceof Publisher) {
			throw new IllegalArgumentException("Use publisher(String, Publisher, Class) or " +
				"publisher(String, Publisher, ParameterizedTypeReference) for adding Publisher parts");
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
		DefaultPartBuilder builder = new DefaultPartBuilder(partBody, partHeaders);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * Adds a {@link Publisher} part to this builder, allowing for further header customization with
	 * the returned {@link PartBuilder}.
	 * @param name the name of the part to add (may not be empty)
	 * @param publisher the contents of the part to add
	 * @param elementClass the class of elements contained in the publisher
	 * @return a builder that allows for further header customization
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher,
			Class<T> elementClass) {

		Assert.notNull(elementClass, "'elementClass' must not be null");
		ResolvableType elementType = ResolvableType.forClass(elementClass);
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		HttpHeaders partHeaders = new HttpHeaders();
		PublisherPartBuilder<T, P> builder =
				new PublisherPartBuilder<>(publisher, elementClass, partHeaders);
		this.parts.add(name, builder);
		return builder;

	}

	/**
	 * Adds a {@link Publisher} part to this builder, allowing for further header customization with
	 * the returned {@link PartBuilder}.
	 * @param name the name of the part to add (may not be empty)
	 * @param publisher the contents of the part to add
	 * @param typeReference the type of elements contained in the publisher
	 * @return a builder that allows for further header customization
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher,
			ParameterizedTypeReference<T> typeReference) {

		Assert.notNull(typeReference, "'typeReference' must not be null");
		ResolvableType elementType1 = ResolvableType.forType(typeReference);
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementType1, "'typeReference' must not be null");

		HttpHeaders partHeaders = new HttpHeaders();
		PublisherPartBuilder<T, P> builder =
				new PublisherPartBuilder<>(publisher, typeReference, partHeaders);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * Builder interface that allows for customization of part headers.
	 */
	public interface PartBuilder {

		/**
		 * Add the given part-specific header values under the given name.
		 * @param headerName the part header name
		 * @param headerValues the part header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		PartBuilder header(String headerName, String... headerValues);
	}


	private static class DefaultPartBuilder implements PartBuilder {

		@Nullable
		protected final Object body;

		protected final HttpHeaders headers;

		public DefaultPartBuilder(@Nullable Object body, HttpHeaders headers) {
			this.body = body;
			this.headers = headers;
		}

		@Override
		public PartBuilder header(String headerName, String... headerValues) {
			this.headers.addAll(headerName, Arrays.asList(headerValues));
			return this;
		}

		public HttpEntity<?> build() {
			return new HttpEntity<>(this.body, this.headers);
		}
	}

	private static class PublisherPartBuilder<S, P extends Publisher<S>>
			extends DefaultPartBuilder {

		private final ResolvableType resolvableType;

		public PublisherPartBuilder(P body, Class<S> elementClass, HttpHeaders headers) {
			super(body, headers);
			this.resolvableType = ResolvableType.forClass(elementClass);
		}

		public PublisherPartBuilder(P body, ParameterizedTypeReference<S> typeReference,
				HttpHeaders headers) {

			super(body, headers);
			this.resolvableType = ResolvableType.forType(typeReference);
		}

		@Override
		@SuppressWarnings("unchecked")
		public HttpEntity<?> build() {
			P publisher = (P) this.body;
			Assert.state(publisher != null, "'publisher' must not be null");
			return new PublisherEntity<>(publisher, this.resolvableType, this.headers);
		}
	}


	/**
	 * Specific subtype of {@link HttpEntity} for containing {@link Publisher}s as body.
	 * Exposes the type contained in the publisher through {@link #getResolvableType()}.
	 * @param <T> The type contained in the publisher
	 * @param <P> The publisher
	 */
	public static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P> {

		private final ResolvableType resolvableType;


		PublisherEntity(P publisher, ResolvableType resolvableType,
				@Nullable MultiValueMap<String, String> headers) {
			super(publisher, headers);
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(resolvableType, "'resolvableType' must not be null");
			this.resolvableType = resolvableType;
		}

		/**
		 * Return the resolvable type for this entry.
		 */
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}
	}

}
