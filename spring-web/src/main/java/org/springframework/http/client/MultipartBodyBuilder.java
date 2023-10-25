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

package org.springframework.http.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Prepare the body of a multipart request, resulting in a
 * {@code MultiValueMap<String, HttpEntity>}. Parts may be concrete values or
 * via asynchronous types such as Reactor {@code Mono}, {@code Flux}, and
 * others registered in the
 * {@link org.springframework.core.ReactiveAdapterRegistry ReactiveAdapterRegistry}.
 *
 * <p>This builder is intended to POST {@code multipart/form-data} using the reactive
 * {@link org.springframework.web.reactive.function.client.WebClient WebClient}.
 * For multipart requests with the {@code RestTemplate}, simply create and
 * populate a {@code MultiValueMap<String, HttpEntity>} as shown in the Javadoc for
 * {@link org.springframework.http.converter.FormHttpMessageConverter FormHttpMessageConverter}
 * and in the
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#rest-template-multipart">reference docs</a>.
 *
 * <p>Below are examples of using this builder:
 * <pre class="code">
 *
 * // Add form field
 * MultipartBodyBuilder builder = new MultipartBodyBuilder();
 * builder.part("form field", "form value").header("foo", "bar");
 *
 * // Add file part
 * Resource image = new ClassPathResource("image.jpg");
 * builder.part("image", image).header("foo", "bar");
 *
 * // Add content (e.g. JSON)
 * Account account = ...
 * builder.part("account", account).header("foo", "bar");
 *
 * // Add content from Publisher
 * Mono&lt;Account&gt; accountMono = ...
 * builder.asyncPart("account", accountMono).header("foo", "bar");
 *
 * // Build and use
 * MultiValueMap&lt;String, HttpEntity&lt;?&gt;&gt; multipartBody = builder.build();
 *
 * Mono&lt;Void&gt; result = webClient.post()
 *     .uri("...")
 *     .bodyValue(multipartBody)
 *     .retrieve()
 *     .bodyToMono(Void.class)
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
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
	 * Add a part where the Object may be:
	 * <ul>
	 * <li>String -- form field
	 * <li>{@link org.springframework.core.io.Resource Resource} -- file part
	 * <li>Object -- content to be encoded (e.g. to JSON)
	 * <li>{@link HttpEntity} -- part content and headers although generally it's
	 * easier to add headers through the returned builder
	 * <li>{@link Part} -- a part from a server request
	 * </ul>
	 * @param name the name of the part to add
	 * @param part the part data
	 * @return builder that allows for further customization of part headers
	 */
	public PartBuilder part(String name, Object part) {
		return part(name, part, null);
	}

	/**
	 * Variant of {@link #part(String, Object)} that also accepts a MediaType.
	 * @param name the name of the part to add
	 * @param part the part data
	 * @param contentType the media type to help with encoding the part
	 * @return builder that allows for further customization of part headers
	 */
	public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(part, "'part' must not be null");

		if (part instanceof Part partObject) {
			PartBuilder builder = asyncPart(name, partObject.content(), DataBuffer.class);
			if (!partObject.headers().isEmpty()) {
				builder.headers(headers -> {
					headers.putAll(partObject.headers());
					String filename = headers.getContentDisposition().getFilename();
					// reset to parameter name
					headers.setContentDispositionFormData(name, filename);
				});
			}
			if (contentType != null) {
				builder.contentType(contentType);
			}
			return builder;
		}

		if (part instanceof PublisherEntity<?,?> publisherEntity) {
			PublisherPartBuilder<?, ?> builder = new PublisherPartBuilder<>(name, publisherEntity);
			if (contentType != null) {
				builder.contentType(contentType);
			}
			this.parts.add(name, builder);
			return builder;
		}

		Object partBody;
		HttpHeaders partHeaders = null;
		if (part instanceof HttpEntity<?> httpEntity) {
			partBody = httpEntity.getBody();
			partHeaders = new HttpHeaders();
			partHeaders.putAll(httpEntity.getHeaders());
		}
		else {
			partBody = part;
		}

		if (partBody instanceof Publisher) {
			throw new IllegalArgumentException("""
					Use asyncPart(String, Publisher, Class) \
					or asyncPart(String, Publisher, ParameterizedTypeReference) \
					or MultipartBodyBuilder.PublisherEntity""");
		}

		DefaultPartBuilder builder = new DefaultPartBuilder(name, partHeaders, partBody);
		if (contentType != null) {
			builder.contentType(contentType);
		}
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * Add a part from {@link Publisher} content.
	 * @param name the name of the part to add
	 * @param publisher a Publisher of content for the part
	 * @param elementClass the type of elements contained in the publisher
	 * @return builder that allows for further customization of part headers
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, Class<T> elementClass) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");

		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, elementClass);
		this.parts.add(name, builder);
		return builder;
	}

	/**
	 * Variant of {@link #asyncPart(String, Publisher, Class)} with a
	 * {@link ParameterizedTypeReference} for the element type information.
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

		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, typeReference);
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
		 * Set the {@linkplain MediaType media type} of the part.
		 * @param contentType the content type
		 * @since 5.2
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		PartBuilder contentType(MediaType contentType);

		/**
		 * Set the filename parameter for a file part. This should not be
		 * necessary with {@link org.springframework.core.io.Resource Resource}
		 * based parts that expose a filename but may be useful for
		 * {@link Publisher} parts.
		 * @param filename the filename to set on the Content-Disposition
		 * @since 5.2
		 */
		PartBuilder filename(String filename);

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

		private final String name;

		@Nullable
		protected HttpHeaders headers;

		@Nullable
		protected final Object body;

		public DefaultPartBuilder(String name, @Nullable HttpHeaders headers, @Nullable Object body) {
			this.name = name;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public PartBuilder contentType(MediaType contentType) {
			initHeadersIfNecessary().setContentType(contentType);
			return this;
		}

		@Override
		public PartBuilder filename(String filename) {
			initHeadersIfNecessary().setContentDispositionFormData(this.name, filename);
			return this;
		}

		@Override
		public PartBuilder header(String headerName, String... headerValues) {
			initHeadersIfNecessary().addAll(headerName, Arrays.asList(headerValues));
			return this;
		}

		@Override
		public PartBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(initHeadersIfNecessary());
			return this;
		}

		private HttpHeaders initHeadersIfNecessary() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
			}
			return this.headers;
		}

		public HttpEntity<?> build() {
			return new HttpEntity<>(this.body, this.headers);
		}
	}


	private static class PublisherPartBuilder<S, P extends Publisher<S>> extends DefaultPartBuilder {

		private final ResolvableType resolvableType;

		public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body, Class<S> elementClass) {
			super(name, headers, body);
			this.resolvableType = ResolvableType.forClass(elementClass);
		}

		public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body,
				ParameterizedTypeReference<S> typeRef) {

			super(name, headers, body);
			this.resolvableType = ResolvableType.forType(typeRef);
		}

		public PublisherPartBuilder(String name, PublisherEntity<S, P> other) {
			super(name, other.getHeaders(), other.getBody());
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
	 * @param <T> the type contained in the publisher
	 * @param <P> the publisher
	 */
	static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P>
			implements ResolvableTypeProvider {

		private final ResolvableType resolvableType;

		PublisherEntity(
				@Nullable MultiValueMap<String, String> headers, P publisher, ResolvableType resolvableType) {

			super(publisher, headers);
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(resolvableType, "'resolvableType' must not be null");
			this.resolvableType = resolvableType;
		}

		/**
		 * Return the element type for the {@code Publisher} body.
		 */
		@Override
		@NonNull
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}
	}

}
