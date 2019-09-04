/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilder;

/**
 * Represents a server-side HTTP request, as handled by a {@code HandlerFunction}.
 * Access to headers and body is offered by {@link Headers} and
 * {@link #body(Class)}, respectively.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public interface ServerRequest {

	/**
	 * Get the HTTP method.
	 * @return the HTTP method as an HttpMethod enum value, or {@code null}
	 * if not resolvable (e.g. in case of a non-standard HTTP method)
	 */
	@Nullable
	default HttpMethod method() {
		return HttpMethod.resolve(methodName());
	}

	/**
	 * Get the name of the HTTP method.
	 * @return the HTTP method as a String
	 */
	String methodName();

	/**
	 * Get the request URI.
	 */
	URI uri();

	/**
	 * Get a {@code UriBuilderComponents} from the URI associated with this
	 * {@code ServerRequest}.
	 *
	 * @return a URI builder
	 */
	UriBuilder uriBuilder();

	/**
	 * Get the request path.
	 */
	default String path() {
		return uri().getRawPath();
	}

	/**
	 * Get the request path as a {@code PathContainer}.
	 */
	default PathContainer pathContainer() {
		return PathContainer.parsePath(path());
	}

	/**
	 * Get the headers of this request.
	 */
	Headers headers();

	/**
	 * Get the cookies of this request.
	 */
	MultiValueMap<String, Cookie> cookies();

	/**
	 * Get the remote address to which this request is connected, if available.
	 */
	Optional<InetSocketAddress> remoteAddress();

	/**
	 * Get the readers used to convert the body of this request.
	 */
	List<HttpMessageConverter<?>> messageConverters();

	/**
	 * Extract the body as an object of the given type.
	 * @param bodyType the type of return value
	 * @param <T> the body type
	 * @return the body
	 */
	<T> T body(Class<T> bodyType) throws ServletException, IOException;

	/**
	 * Extract the body as an object of the given type.
	 * @param bodyType the type of return value
	 * @param <T> the body type
	 * @return the body
	 */
	<T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException;

	/**
	 * Get the request attribute value if present.
	 * @param name the attribute name
	 * @return the attribute value
	 */
	default Optional<Object> attribute(String name) {
		Map<String, Object> attributes = attributes();
		if (attributes.containsKey(name)) {
			return Optional.of(attributes.get(name));
		}
		else {
			return Optional.empty();
		}
	}

	/**
	 * Get a mutable map of request attributes.
	 * @return the request attributes
	 */
	Map<String, Object> attributes();

	/**
	 * Get the first parameter with the given name, if present.
	 * @param name the parameter name
	 * @return the parameter value
	 * @see HttpServletRequest#getParameter(String)
	 */
	default Optional<String> param(String name) {
		List<String> paramValues = params().get(name);
		if (CollectionUtils.isEmpty(paramValues)) {
			return Optional.empty();
		}
		else {
			String value = paramValues.get(0);
			if (value == null) {
				value = "";
			}
			return Optional.of(value);
		}
	}

	/**
	 * Get all parameters for this request.
	 * @see HttpServletRequest#getParameterMap()
	 */
	MultiValueMap<String, String> params();

	/**
	 * Get the path variable with the given name, if present.
	 * @param name the variable name
	 * @return the variable value
	 * @throws IllegalArgumentException if there is no path variable with the given name
	 */
	default String pathVariable(String name) {
		Map<String, String> pathVariables = pathVariables();
		if (pathVariables.containsKey(name)) {
			return pathVariables().get(name);
		}
		else {
			throw new IllegalArgumentException("No path variable with name \"" + name + "\" available");
		}
	}

	/**
	 * Get all path variables for this request.
	 */
	Map<String, String> pathVariables();

	/**
	 * Get the web session for this request. Always guaranteed to
	 * return an instance either matching to the session id requested by the
	 * client, or with a new session id either because the client did not
	 * specify one or because the underlying session had expired. Use of this
	 * method does not automatically create a session.
	 */
	HttpSession session();

	/**
	 * Get the authenticated user for the request, if any.
	 */
	Optional<Principal> principal();

	/**
	 * Get the servlet request that this request is based on.
	 */
	HttpServletRequest servletRequest();


	// Static methods

	/**
	 * Create a new {@code ServerRequest} based on the given {@code HttpServletRequest} and
	 * message converters.
	 * @param servletRequest the request
	 * @param messageReaders the message readers
	 * @return the created {@code ServerRequest}
	 */
	static ServerRequest create(HttpServletRequest servletRequest, List<HttpMessageConverter<?>> messageReaders) {
		return new DefaultServerRequest(servletRequest, messageReaders);
	}

	/**
	 * Create a builder with the status, headers, and cookies of the given request.
	 * @param other the response to copy the status, headers, and cookies from
	 * @return the created builder
	 */
	static Builder from(ServerRequest other) {
		return new DefaultServerRequestBuilder(other);
	}


	/**
	 * Represents the headers of the HTTP request.
	 * @see ServerRequest#headers()
	 */
	interface Headers {

		/**
		 * Get the list of acceptable media types, as specified by the {@code Accept}
		 * header.
		 * <p>Returns an empty list if the acceptable media types are unspecified.
		 */
		List<MediaType> accept();

		/**
		 * Get the list of acceptable charsets, as specified by the
		 * {@code Accept-Charset} header.
		 */
		List<Charset> acceptCharset();

		/**
		 * Get the list of acceptable languages, as specified by the
		 * {@code Accept-Language} header.
		 */
		List<Locale.LanguageRange> acceptLanguage();

		/**
		 * Get the length of the body in bytes, as specified by the
		 * {@code Content-Length} header.
		 */
		OptionalLong contentLength();

		/**
		 * Get the media type of the body, as specified by the
		 * {@code Content-Type} header.
		 */
		Optional<MediaType> contentType();

		/**
		 * Get the value of the {@code Host} header, if available.
		 * <p>If the header value does not contain a port, the
		 * {@linkplain InetSocketAddress#getPort() port} in the returned address will
		 * be {@code 0}.
		 */
		@Nullable
		InetSocketAddress host();

		/**
		 * Get the value of the {@code Range} header.
		 * <p>Returns an empty list when the range is unknown.
		 */
		List<HttpRange> range();

		/**
		 * Get the header value(s), if any, for the header of the given name.
		 * <p>Returns an empty list if no header values are found.
		 * @param headerName the header name
		 */
		List<String> header(String headerName);

		/**
		 * Get the headers as an instance of {@link HttpHeaders}.
		 */
		HttpHeaders asHttpHeaders();
	}


	/**
	 * Defines a builder for a request.
	 */
	interface Builder {

		/**
		 * Set the method of the request.
		 * @param method the new method
		 * @return this builder
		 */
		Builder method(HttpMethod method);

		/**
		 * Set the URI of the request.
		 * @param uri the new URI
		 * @return this builder
		 */
		Builder uri(URI uri);

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * Manipulate this request's headers with the given consumer.
		 * <p>The headers provided to the consumer are "live", so that the consumer can be used to
		 * {@linkplain HttpHeaders#set(String, String) overwrite} existing header values,
		 * {@linkplain HttpHeaders#remove(Object) remove} values, or use any of the other
		 * {@link HttpHeaders} methods.
		 * @param headersConsumer a function that consumes the {@code HttpHeaders}
		 * @return this builder
		 */
		Builder headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * Add a cookie with the given name and value(s).
		 * @param name the cookie name
		 * @param values the cookie value(s)
		 * @return this builder
		 */
		Builder cookie(String name, String... values);

		/**
		 * Manipulate this request's cookies with the given consumer.
		 * <p>The map provided to the consumer is "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing cookies,
		 * {@linkplain MultiValueMap#remove(Object) remove} cookies, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		Builder cookies(Consumer<MultiValueMap<String, Cookie>> cookiesConsumer);

		/**
		 * Set the body of the request.
		 * <p>Calling this methods will
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) release}
		 * the existing body of the builder.
		 * @param body the new body
		 * @return this builder
		 */
		Builder body(byte[] body);

		/**
		 * Set the body of the request to the UTF-8 encoded bytes of the given string.
		 * <p>Calling this methods will
		 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) release}
		 * the existing body of the builder.
		 * @param body the new body
		 * @return this builder
		 */
		Builder body(String body);

		/**
		 * Add an attribute with the given name and value.
		 * @param name  the attribute name
		 * @param value the attribute value
		 * @return this builder
		 */
		Builder attribute(String name, Object value);

		/**
		 * Manipulate this request's attributes with the given consumer.
		 * <p>The map provided to the consumer is "live", so that the consumer can be used
		 * to {@linkplain Map#put(Object, Object) overwrite} existing attributes,
		 * {@linkplain Map#remove(Object) remove} attributes, or use any of the other
		 * {@link Map} methods.
		 * @param attributesConsumer a function that consumes the attributes map
		 * @return this builder
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * Build the request.
		 * @return the built request
		 */
		ServerRequest build();
	}

}
