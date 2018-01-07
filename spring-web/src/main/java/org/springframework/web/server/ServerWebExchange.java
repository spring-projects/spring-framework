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

package org.springframework.web.server;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

/**
 * Contract for an HTTP request-response interaction. Provides access to the HTTP
 * request and response and also exposes additional server-side processing
 * related properties and features such as request attributes.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ServerWebExchange {

	/**
	 * Return the current HTTP request.
	 */
	ServerHttpRequest getRequest();

	/**
	 * Return the current HTTP response.
	 */
	ServerHttpResponse getResponse();

	/**
	 * Return a mutable map of request attributes for the current exchange.
	 */
	Map<String, Object> getAttributes();

	/**
	 * Return the request attribute value if present.
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T getAttribute(String name) {
		return (T) getAttributes().get(name);
	}

	/**
	 * Return the request attribute value or if not present raise an
	 * {@link IllegalArgumentException}.
	 * @param name the attribute name
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	default <T> T getRequiredAttribute(String name) {
		T value = getAttribute(name);
		Assert.notNull(value, "Required attribute '" + name + "' is missing.");
		return value;
	}

	/**
	 * Return the request attribute value, or a default, fallback value.
	 * @param name the attribute name
	 * @param defaultValue a default value to return instead
	 * @param <T> the attribute type
	 * @return the attribute value
	 */
	@SuppressWarnings("unchecked")
	default <T> T getAttributeOrDefault(String name, T defaultValue) {
		return (T) getAttributes().getOrDefault(name, defaultValue);
	}

	/**
	 * Return the web session for the current request. Always guaranteed  to
	 * return an instance either matching to the session id requested by the
	 * client, or with a new session id either because the client did not
	 * specify one or because the underlying session had expired. Use of this
	 * method does not automatically create a session. See {@link WebSession}
	 * for more details.
	 */
	Mono<WebSession> getSession();

	/**
	 * Return the authenticated user for the request, if any.
	 */
	<T extends Principal> Mono<T> getPrincipal();

	/**
	 * Return the form data from the body of the request if the Content-Type is
	 * {@code "application/x-www-form-urlencoded"} or an empty map otherwise.
	 *
	 * <p><strong>Note:</strong> calling this method causes the request body to
	 * be read and parsed in full and the resulting {@code MultiValueMap} is
	 * cached so that this method is safe to call more than once.
	 */
	Mono<MultiValueMap<String, String>> getFormData();

	/**
	 * Return the parts of a multipart request if the Content-Type is
	 * {@code "multipart/form-data"} or an empty map otherwise.
	 *
	 * <p><strong>Note:</strong> calling this method causes the request body to
	 * be read and parsed in full and the resulting {@code MultiValueMap} is
	 * cached so that this method is safe to call more than once.
	 */
	Mono<MultiValueMap<String, Part>> getMultipartData();

	/**
	 * Return the {@link LocaleContext} using the configured
	 * {@link org.springframework.web.server.i18n.LocaleContextResolver}.
	 */
	LocaleContext getLocaleContext();

	/**
	 * Return the {@link ApplicationContext} associated with the web application,
	 * if it was initialized with one via
	 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext
	 * WebHttpHandlerBuilder#applicationContext}.
	 * @since 5.0.3
	 * @see org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)
	 */
	@Nullable
	ApplicationContext getApplicationContext();

	/**
	 * Returns {@code true} if the one of the {@code checkNotModified} methods
	 * in this contract were used and they returned true.
	 */
	boolean isNotModified();

	/**
	 * An overloaded variant of {@link #checkNotModified(String, Instant)} with
	 * a last-modified timestamp only.
	 * @param lastModified the last-modified time
	 * @return whether the request qualifies as not modified
	 */
	boolean checkNotModified(Instant lastModified);

	/**
	 * An overloaded variant of {@link #checkNotModified(String, Instant)} with
	 * an {@code ETag} (entity tag) value only.
	 * @param etag the entity tag for the underlying resource.
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(String etag);

	/**
	 * Check whether the requested resource has been modified given the supplied
	 * {@code ETag} (entity tag) and last-modified timestamp as determined by
	 * the application. Also transparently prepares the response, setting HTTP
	 * status, and adding "ETag" and "Last-Modified" headers when applicable.
	 * This method works with conditional GET/HEAD requests as well as with
	 * conditional POST/PUT/DELETE requests.
	 * <p><strong>Note:</strong> The HTTP specification recommends setting both
	 * ETag and Last-Modified values, but you can also use
	 * {@code #checkNotModified(String)} or
	 * {@link #checkNotModified(Instant)}.
	 * @param etag the entity tag that the application determined for the
	 * underlying resource. This parameter will be padded with quotes (")
	 * if necessary.
	 * @param lastModified the last-modified timestamp that the application
	 * determined for the underlying resource
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(@Nullable String etag, Instant lastModified);

	/**
	 * Transform the given url according to the registered transformation function(s).
	 * By default, this method returns the given {@code url}, though additional
	 * transformation functions can by registered with {@link #addUrlTransformer}
	 * @param url the URL to transform
	 * @return the transformed URL
	 */
	String transformUrl(String url);

	/**
	 * Register an additional URL transformation function for use with {@link #transformUrl}.
	 * The given function can be used to insert an id for authentication, a nonce for CSRF
	 * protection, etc.
	 * <p>Note that the given function is applied after any previously registered functions.
	 * @param transformer a URL transformation function to add
	 */
	void addUrlTransformer(Function<String, String> transformer);

	/**
	 * Return a builder to mutate properties of this exchange by wrapping it
	 * with {@link ServerWebExchangeDecorator} and returning either mutated
	 * values or delegating back to this instance.
	 */
	default Builder mutate() {
		return new DefaultServerWebExchangeBuilder(this);
	}


	/**
	 * Builder for mutating an existing {@link ServerWebExchange}.
	 * Removes the need
	 */
	interface Builder {

		/**
		 * Configure a consumer to modify the current request using a builder.
		 * <p>Effectively this:
		 * <pre>
		 * exchange.mutate().request(builder-> builder.method(HttpMethod.PUT));
		 *
		 * // vs...
		 *
		 * ServerHttpRequest request = exchange.getRequest().mutate()
		 *     .method(HttpMethod.PUT)
		 *     .build();
		 *
		 * exchange.mutate().request(request);
		 * </pre>
		 * @see ServerHttpRequest#mutate()
		 */
		Builder request(Consumer<ServerHttpRequest.Builder> requestBuilderConsumer);

		/**
		 * Set the request to use especially when there is a need to override
		 * {@link ServerHttpRequest} methods. To simply mutate request properties
		 * see {@link #request(Consumer)} instead.
		 * @see org.springframework.http.server.reactive.ServerHttpRequestDecorator
		 */
		Builder request(ServerHttpRequest request);

		/**
		 * Set the response to use.
		 * @see org.springframework.http.server.reactive.ServerHttpResponseDecorator
		 */
		Builder response(ServerHttpResponse response);

		/**
		 * Set the {@code Mono<Principal>} to return for this exchange.
		 */
		Builder principal(Mono<Principal> principalMono);

		/**
		 * Build a {@link ServerWebExchange} decorator with the mutated properties.
		 */
		ServerWebExchange build();
	}

}
