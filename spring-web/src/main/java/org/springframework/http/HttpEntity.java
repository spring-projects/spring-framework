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

package org.springframework.http;

import org.reactivestreams.Publisher;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * Represents an HTTP request or response entity, consisting of headers and body.
 *
 * <p>Typically used in combination with the {@link org.springframework.web.client.RestTemplate},
 * like so:
 * <pre class="code">
 * HttpHeaders headers = new HttpHeaders();
 * headers.setContentType(MediaType.TEXT_PLAIN);
 * HttpEntity&lt;String&gt; entity = new HttpEntity&lt;String&gt;(helloWorld, headers);
 * URI location = template.postForLocation("http://example.com", entity);
 * </pre>
 * or
 * <pre class="code">
 * HttpEntity&lt;String&gt; entity = template.getForEntity("http://example.com", String.class);
 * String body = entity.getBody();
 * MediaType contentType = entity.getHeaders().getContentType();
 * </pre>
 * Can also be used in Spring MVC, as a return value from a @Controller method:
 * <pre class="code">
 * &#64;RequestMapping("/handle")
 * public HttpEntity&lt;String&gt; handle() {
 *   HttpHeaders responseHeaders = new HttpHeaders();
 *   responseHeaders.set("MyResponseHeader", "MyValue");
 *   return new HttpEntity&lt;String&gt;("Hello World", responseHeaders);
 * }
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0.2
 * @see org.springframework.web.client.RestTemplate
 * @see #getBody()
 * @see #getHeaders()
 */
public class HttpEntity<T> {

	/**
	 * The empty {@code HttpEntity}, with no body or headers.
	 */
	public static final HttpEntity<?> EMPTY = new HttpEntity<>();


	private final HttpHeaders headers;

	@Nullable
	private final T body;

	@Nullable
	private final ResolvableType bodyType;


	/**
	 * Create a new, empty {@code HttpEntity}.
	 */
	protected HttpEntity() {
		this(null, null);
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and no headers.
	 * @param body the entity body
	 */
	public HttpEntity(T body) {
		this(body, null);
	}

	/**
	 * Create a new {@code HttpEntity} with the given headers and no body.
	 * @param headers the entity headers
	 */
	public HttpEntity(MultiValueMap<String, String> headers) {
		this(null, headers);
	}

	/**
	 * Create a new {@code HttpEntity} with the given body and headers.
	 * @param body the entity body
	 * @param headers the entity headers
	 */
	public HttpEntity(@Nullable T body, @Nullable MultiValueMap<String, String> headers) {
		this(body, null, headers);
	}

	private HttpEntity(@Nullable T body, @Nullable ResolvableType bodyType,
			@Nullable MultiValueMap<String, String> headers) {
		this.body = body;

		if (bodyType == null && body != null) {
			bodyType = ResolvableType.forClass(body.getClass());
		}
		this.bodyType = bodyType ;

		HttpHeaders tempHeaders = new HttpHeaders();
		if (headers != null) {
			tempHeaders.putAll(headers);
		}
		this.headers = HttpHeaders.readOnlyHttpHeaders(tempHeaders);
	}


	/**
	 * Returns the headers of this entity.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Returns the body of this entity.
	 */
	@Nullable
	public T getBody() {
		return this.body;
	}

	/**
	 * Indicates whether this entity has a body.
	 */
	public boolean hasBody() {
		return (this.body != null);
	}

	/**
	 * Returns the type of the body.
	 */
	@Nullable
	public ResolvableType getBodyType() {
		return this.bodyType;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		HttpEntity<?> otherEntity = (HttpEntity<?>) other;
		return (ObjectUtils.nullSafeEquals(this.headers, otherEntity.headers) &&
				ObjectUtils.nullSafeEquals(this.body, otherEntity.body));
	}

	@Override
	public int hashCode() {
		return (ObjectUtils.nullSafeHashCode(this.headers) * 29 + ObjectUtils.nullSafeHashCode(this.body));
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("<");
		if (this.body != null) {
			builder.append(this.body);
			builder.append(',');
		}
		builder.append(this.headers);
		builder.append('>');
		return builder.toString();
	}


	// Static builder methods

	/**
	 * Create a new {@code HttpEntity} with the given {@link Publisher} as body, class contained in
	 * {@code publisher}, and headers.
	 * @param publisher the publisher to use as body
	 * @param elementClass the class of elements contained in the publisher
	 * @param headers the entity headers
	 * @param <S> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return the created entity
	 */
	public static <S, P extends Publisher<S>> HttpEntity<P> fromPublisher(P publisher,
			Class<S> elementClass, @Nullable MultiValueMap<String, String> headers) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return new HttpEntity<>(publisher, ResolvableType.forClass(elementClass), headers);
	}

	/**
	 * Create a new {@code HttpEntity} with the given {@link Publisher} as body, type contained in
	 * {@code publisher}, and headers.
	 * @param publisher the publisher to use as body
	 * @param typeReference the type of elements contained in the publisher
	 * @param headers the entity headers
	 * @param <S> the type of the elements contained in the publisher
	 * @param <P> the type of the {@code Publisher}
	 * @return the created entity
	 */
	public static <S, P extends Publisher<S>> HttpEntity<P> fromPublisher(P publisher,
			ParameterizedTypeReference<S> typeReference,
			@Nullable MultiValueMap<String, String> headers) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(typeReference, "'typeReference' must not be null");
		return new HttpEntity<>(publisher, ResolvableType.forType(typeReference), headers);
	}

}
