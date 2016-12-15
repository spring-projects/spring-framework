/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
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
	<T> Optional<T> getAttribute(String name);

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
	 * {@code "application/x-www-form-urlencoded"} or an empty map.
	 */
	Mono<MultiValueMap<String, String>> getFormData();

	/**
	 * Return a combined map that represents both
	 * {@link ServerHttpRequest#getQueryParams()} and {@link #getFormData()}
	 * or an empty map.
	 */
	Mono<MultiValueMap<String, String>> getRequestParams();

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
	 *
	 * <p><strong>Note:</strong> The HTTP specification recommends setting both
	 * ETag and Last-Modified values, but you can also use
	 * {@code #checkNotModified(String)} or
	 * {@link #checkNotModified(Instant)}.
	 *
	 * @param etag the entity tag that the application determined for the
	 * underlying resource. This parameter will be padded with quotes (")
	 * if necessary.
	 * @param lastModified the last-modified timestamp that the application
	 * determined for the underlying resource
	 * @return true if the request does not require further processing.
	 */
	boolean checkNotModified(String etag, Instant lastModified);


	/**
	 * Return a builder to mutate properties of this exchange. The resulting
	 * new exchange is an immutable {@link ServerWebExchangeDecorator decorator}
	 * around the current exchange instance returning mutated values.
	 */
	default Builder mutate() {
		return new DefaultServerWebExchangeBuilder(this);
	}


	/**
	 * Builder for mutating the properties of a {@link ServerWebExchange}.
	 */
	interface Builder {

		/**
		 * Set the request to use.
		 */
		Builder request(ServerHttpRequest request);

		/**
		 * Set the response to use.
		 */
		Builder response(ServerHttpResponse response);

		/**
		 * Set the principal to use.
		 */
		Builder principal(Mono<Principal> user);

		/**
		 * Set the session to use.
		 */
		Builder session(Mono<WebSession> session);

		/**
		 * Set the form data.
		 */
		Builder formData(Mono<MultiValueMap<String, String>> formData);

		/**
		 * Build an immutable wrapper that returning the mutated properties.
		 */
		ServerWebExchange build();

	}

}
