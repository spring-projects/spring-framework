/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.net.URI;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.json.Jackson2CodecSupport;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;

/**
 * Represents a typed server-side HTTP response, as returned
 * by a {@linkplain HandlerFunction handler function} or
 * {@linkplain HandlerFilterFunction filter function}.
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface ServerResponse {

	/**
	 * Return the status code of this response.
	 * @return the status as an HttpStatusCode value
	 */
	HttpStatusCode statusCode();

	/**
	 * Return the status code of this response as integer.
	 * @return the status as an integer
	 * @since 5.2
	 * @deprecated as of 6.0, in favor of {@link #statusCode()}
	 */
	@Deprecated(since = "6.0")
	int rawStatusCode();

	/**
	 * Return the headers of this response.
	 */
	HttpHeaders headers();

	/**
	 * Return the cookies of this response.
	 */
	MultiValueMap<String, ResponseCookie> cookies();

	/**
	 * Write this response to the given web exchange.
	 * @param exchange the web exchange to write to
	 * @param context the context to use when writing
	 * @return {@code Mono<Void>} to indicate when writing is complete
	 */
	Mono<Void> writeTo(ServerWebExchange exchange, Context context);


	// Static methods

	/**
	 * Create a builder with the status code and headers of the given response.
	 * @param other the response to copy the status and headers from
	 * @return the created builder
	 */
	static BodyBuilder from(ServerResponse other) {
		return new DefaultServerResponseBuilder(other);
	}

	/**
	 * Create a {@code ServerResponse} from the given {@link ErrorResponse}.
	 * @param response the {@link ErrorResponse} to initialize from
	 * @return {@code Mono} with the built response
	 * @since 6.0
	 */
	static Mono<ServerResponse> from(ErrorResponse response) {
		return status(response.getStatusCode())
				.headers(headers -> headers.putAll(response.getHeaders()))
				.bodyValue(response.getBody());
	}

	/**
	 * Create a builder with the given HTTP status.
	 * @param status the response status
	 * @return the created builder
	 */
	static BodyBuilder status(HttpStatusCode status) {
		return new DefaultServerResponseBuilder(status);
	}

	/**
	 * Create a builder with the given HTTP status.
	 * @param status the response status
	 * @return the created builder
	 * @since 5.0.3
	 */
	static BodyBuilder status(int status) {
		return new DefaultServerResponseBuilder(HttpStatusCode.valueOf(status));
	}

	/**
	 * Create a builder with the status set to {@linkplain HttpStatus#OK 200 OK}.
	 * @return the created builder
	 */
	static BodyBuilder ok() {
		return status(HttpStatus.OK);
	}

	/**
	 * Create a new builder with a {@linkplain HttpStatus#CREATED 201 Created} status
	 * and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder created(URI location) {
		BodyBuilder builder = status(HttpStatus.CREATED);
		return builder.location(location);
	}

	/**
	 * Create a builder with an {@linkplain HttpStatus#ACCEPTED 202 Accepted} status.
	 * @return the created builder
	 */
	static BodyBuilder accepted() {
		return status(HttpStatus.ACCEPTED);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NO_CONTENT 204 No Content} status.
	 * @return the created builder
	 */
	static HeadersBuilder<?> noContent() {
		return status(HttpStatus.NO_CONTENT);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#SEE_OTHER 303 See Other}
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder seeOther(URI location) {
		BodyBuilder builder = status(HttpStatus.SEE_OTHER);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#TEMPORARY_REDIRECT 307 Temporary Redirect}
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder temporaryRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.TEMPORARY_REDIRECT);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#PERMANENT_REDIRECT 308 Permanent Redirect}
	 * status and a location header set to the given URI.
	 * @param location the location URI
	 * @return the created builder
	 */
	static BodyBuilder permanentRedirect(URI location) {
		BodyBuilder builder = status(HttpStatus.PERMANENT_REDIRECT);
		return builder.location(location);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#BAD_REQUEST 400 Bad Request} status.
	 * @return the created builder
	 */
	static BodyBuilder badRequest() {
		return status(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Create a builder with a {@linkplain HttpStatus#NOT_FOUND 404 Not Found} status.
	 * @return the created builder
	 */
	static HeadersBuilder<?> notFound() {
		return status(HttpStatus.NOT_FOUND);
	}

	/**
	 * Create a builder with an
	 * {@linkplain HttpStatus#UNPROCESSABLE_ENTITY 422 Unprocessable Entity} status.
	 * @return the created builder
	 */
	static BodyBuilder unprocessableEntity() {
		return status(HttpStatus.UNPROCESSABLE_ENTITY);
	}


	/**
	 * Defines a builder that adds headers to the response.
	 * @param <B> the builder subclass
	 */
	interface HeadersBuilder<B extends HeadersBuilder<B>> {

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName   the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * Manipulate this response's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		B headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Add the given cookie to the response.
		 * @param cookie the cookie to add
		 * @return this builder
		 */
		B cookie(ResponseCookie cookie);

		/**
		 * Manipulate this response's cookies with the given consumer. The
		 * cookies provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
		 * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies
		 * @return this builder
		 */
		B cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(HttpMethod... allowedMethods);

		/**
		 * Set the set of allowed {@link HttpMethod HTTP methods}, as specified
		 * by the {@code Allow} header.
		 * @param allowedMethods the allowed methods
		 * @return this builder
		 * @see HttpHeaders#setAllow(Set)
		 */
		B allow(Set<HttpMethod> allowedMethods);

		/**
		 * Set the entity tag of the body, as specified by the {@code ETag} header.
		 * @param eTag the new entity tag
		 * @return this builder
		 * @see HttpHeaders#setETag(String)
		 */
		B eTag(String eTag);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(ZonedDateTime lastModified);

		/**
		 * Set the time the resource was last changed, as specified by the
		 * {@code Last-Modified} header.
		 * @param lastModified the last modified date
		 * @return this builder
		 * @since 5.1.4
		 * @see HttpHeaders#setLastModified(long)
		 */
		B lastModified(Instant lastModified);

		/**
		 * Set the location of a resource, as specified by the {@code Location} header.
		 * @param location the location
		 * @return this builder
		 * @see HttpHeaders#setLocation(URI)
		 */
		B location(URI location);

		/**
		 * Set the caching directives for the resource, as specified by the HTTP 1.1
		 * {@code Cache-Control} header.
		 * <p>A {@code CacheControl} instance can be built like
		 * {@code CacheControl.maxAge(3600).cachePublic().noTransform()}.
		 * @param cacheControl a builder for cache-related HTTP response headers
		 * @return this builder
		 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2">RFC-7234 Section 5.2</a>
		 */
		B cacheControl(CacheControl cacheControl);

		/**
		 * Configure one or more request header names (e.g. "Accept-Language") to
		 * add to the "Vary" response header to inform clients that the response is
		 * subject to content negotiation and variances based on the value of the
		 * given request headers. The configured request header names are added only
		 * if not already present in the response "Vary" header.
		 * @param requestHeaders request header names
		 * @return this builder
		 */
		B varyBy(String... requestHeaders);

		/**
		 * Build the response entity with no body.
		 */
		Mono<ServerResponse> build();

		/**
		 * Build the response entity with no body.
		 * <p>The response will be committed when the given {@code voidPublisher} completes.
		 * @param voidPublisher the publisher to indicate when the response should be committed
		 */
		Mono<ServerResponse> build(Publisher<Void> voidPublisher);

		/**
		 * Build the response entity with a custom writer function.
		 * @param writeFunction the function used to write to the {@link ServerWebExchange}
		 */
		Mono<ServerResponse> build(BiFunction<ServerWebExchange, Context, Mono<Void>> writeFunction);
	}


	/**
	 * Defines a builder that adds a body to the response.
	 */
	interface BodyBuilder extends HeadersBuilder<BodyBuilder> {

		/**
		 * Set the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 * @param contentLength the content length
		 * @return this builder
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * Set the {@linkplain MediaType media type} of the body, as specified by the
		 * {@code Content-Type} header.
		 * @param contentType the content type
		 * @return this builder
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * Add a serialization hint like {@link Jackson2CodecSupport#JSON_VIEW_HINT}
		 * to customize how the body will be serialized.
		 * @param key the hint key
		 * @param value the hint value
		 */
		BodyBuilder hint(String key, Object value);

		/**
		 * Customize the serialization hints with the given consumer.
		 * @param hintsConsumer a function that consumes the hints
		 * @return this builder
		 * @since 5.1.6
		 */
		BodyBuilder hints(Consumer<Map<String, Object>> hintsConsumer);

		/**
		 * Set the body of the response to the given {@code Object} and return it.
		 * This is a shortcut for using a {@link #body(BodyInserter)} with a
		 * {@linkplain BodyInserters#fromValue value inserter}.
		 * @param body the body of the response
		 * @return the built response
		 * @throws IllegalArgumentException if {@code body} is a
		 * {@link Publisher} or producer known to {@link ReactiveAdapterRegistry}
		 * @since 5.2
		 */
		Mono<ServerResponse> bodyValue(Object body);

		/**
		 * Set the body from the given {@code Publisher}. Shortcut for
		 * {@link #body(BodyInserter)} with a
		 * {@linkplain BodyInserters#fromPublisher Publisher inserter}.
		 * @param publisher the {@code Publisher} to write to the response
		 * @param elementClass the type of elements published
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return the built response
		 */
		<T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher, Class<T> elementClass);

		/**
		 * Variant of {@link #body(Publisher, Class)} that allows using any
		 * producer that can be resolved to {@link Publisher} via
		 * {@link ReactiveAdapterRegistry}.
		 * @param publisher the {@code Publisher} to use to write the response
		 * @param elementTypeRef the type of elements produced
		 * @param <T> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return the built response
		 */
		<T, P extends Publisher<T>> Mono<ServerResponse> body(P publisher,
				ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * Variant of {@link #body(Publisher, Class)} that allows using any
		 * producer that can be resolved to {@link Publisher} via
		 * {@link ReactiveAdapterRegistry}.
		 * @param producer the producer to write to the request
		 * @param elementClass the type of elements produced
		 * @return the built response
		 * @since 5.2
		 */
		Mono<ServerResponse> body(Object producer, Class<?> elementClass);

		/**
		 * Variant of {@link #body(Publisher, ParameterizedTypeReference)} that
		 * allows using any producer that can be resolved to {@link Publisher}
		 * via {@link ReactiveAdapterRegistry}.
		 * @param producer the producer to write to the response
		 * @param elementTypeRef the type of elements produced
		 * @return the built response
		 * @since 5.2
		 */
		Mono<ServerResponse> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * Set the body of the response to the given {@code BodyInserter} and return it.
		 * @param inserter the {@code BodyInserter} that writes to the response
		 * @return the built response
		 */
		Mono<ServerResponse> body(BodyInserter<?, ? super ServerHttpResponse> inserter);

		/**
		 * Set the response body to the given {@code Object} and return it.
		 * As of 5.2 this method delegates to {@link #bodyValue(Object)}.
		 * @deprecated as of Spring Framework 5.2 in favor of {@link #bodyValue(Object)}
		 */
		@Deprecated
		Mono<ServerResponse> syncBody(Object body);

		/**
		 * Render the template with the given {@code name} using the given {@code modelAttributes}.
		 * The model attributes are mapped under a
		 * {@linkplain org.springframework.core.Conventions#getVariableName generated name}.
		 * <p><em>Note: Empty {@link Collection Collections} are not added to
		 * the model when using this method because we cannot correctly determine
		 * the true convention name.</em>
		 * @param name the name of the template to be rendered
		 * @param modelAttributes the modelAttributes used to render the template
		 * @return the built response
		 */
		Mono<ServerResponse> render(String name, Object... modelAttributes);

		/**
		 * Render the template with the given {@code name} using the given {@code model}.
		 * @param name the name of the template to be rendered
		 * @param model the model used to render the template
		 * @return the built response
		 */
		Mono<ServerResponse> render(String name, Map<String, ?> model);
	}


	/**
	 * Defines the context used during the {@link #writeTo(ServerWebExchange, Context)}.
	 */
	interface Context {

		/**
		 * Return the {@link HttpMessageWriter HttpMessageWriters} to be used for response body conversion.
		 * @return the list of message writers
		 */
		List<HttpMessageWriter<?>> messageWriters();

		/**
		 * Return the  {@link ViewResolver ViewResolvers} to be used for view name resolution.
		 * @return the list of view resolvers
		 */
		List<ViewResolver> viewResolvers();
	}

}
