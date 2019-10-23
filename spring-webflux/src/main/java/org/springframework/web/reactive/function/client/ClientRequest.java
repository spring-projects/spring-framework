/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;

/**
 * Represents a typed, immutable, client-side HTTP request, as executed by the
 * {@link ExchangeFunction}. Instances of this interface can be created via static
 * builder methods.
 *
 * <p>Note that applications are more likely to perform requests through
 * {@link WebClient} rather than using this directly.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ClientRequest {

	/**
	 * Name of {@link #attributes() attribute} whose value can be used to
	 * correlate log messages for this request. Use {@link #logPrefix()} to
	 * obtain a consistently formatted prefix based on this attribute.
	 * @since 5.1
	 * @see #logPrefix()
	 */
	String LOG_ID_ATTRIBUTE = ClientRequest.class.getName() + ".LOG_ID";


	/**
	 * Return the HTTP method.
	 */
	HttpMethod method();

	/**
	 * Return the request URI.
	 */
	URI url();

	/**
	 * Return the headers of this request.
	 */
	HttpHeaders headers();

	/**
	 * Return the cookies of this request.
	 */
	MultiValueMap<String, String> cookies();

	/**
	 * Return the body inserter of this request.
	 */
	BodyInserter<?, ? super ClientHttpRequest> body();

	/**
	 * Return the request attribute value if present.
	 * @param name the attribute name
	 * @return the attribute value
	 */
	default Optional<Object> attribute(String name) {
		return Optional.ofNullable(attributes().get(name));
	}

	/**
	 * Return the attributes of this request.
	 */
	Map<String, Object> attributes();

	/**
	 * Return a log message prefix to use to correlate messages for this request.
	 * The prefix is based on the value of the attribute {@link #LOG_ID_ATTRIBUTE}
	 * along with some extra formatting so that the prefix can be conveniently
	 * prepended with no further formatting no separators required.
	 * @return the log message prefix or an empty String if the
	 * {@link #LOG_ID_ATTRIBUTE} is not set.
	 * @since 5.1
	 */
	String logPrefix();

	/**
	 * Write this request to the given {@link ClientHttpRequest}.
	 * @param request the client http request to write to
	 * @param strategies the strategies to use when writing
	 * @return {@code Mono<Void>} to indicate when writing is complete
	 */
	Mono<Void> writeTo(ClientHttpRequest request, ExchangeStrategies strategies);


	// Static builder methods

	/**
	 * Create a builder with the method, URI, headers, and cookies of the given request.
	 * @param other the request to copy the method, URI, headers, and cookies from
	 * @return the created builder
	 */
	static Builder from(ClientRequest other) {
		return new DefaultClientRequestBuilder(other);
	}

	/**
	 * Create a builder with the given method and url.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the url (as a URI instance)
	 * @return the created builder
	 * @deprecated in favor of {@link #create(HttpMethod, URI)}
	 */
	@Deprecated
	static Builder method(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}

	/**
	 * Create a request builder with the given method and url.
	 * @param method the HTTP method (GET, POST, etc)
	 * @param url the url (as a URI instance)
	 * @return the created builder
	 */
	static Builder create(HttpMethod method, URI url) {
		return new DefaultClientRequestBuilder(method, url);
	}


	/**
	 * Defines a builder for a request.
	 */
	interface Builder {

		/**
		 * Set the method of the request.
		 * @param method the new method
		 * @return this builder
		 * @since 5.0.1
		 */
		Builder method(HttpMethod method);

		/**
		 * Set the url of the request.
		 * @param url the new url
		 * @return this builder
		 * @since 5.0.1
		 */
		Builder url(URI url);

		/**
		 * Add the given header value(s) under the given name.
		 * @param headerName  the header name
		 * @param headerValues the header value(s)
		 * @return this builder
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * Manipulate this request's headers with the given consumer. The
		 * headers provided to the consumer are "live", so that the consumer can be used to
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
		 * Manipulate this request's cookies with the given consumer. The
		 * map provided to the consumer is "live", so that the consumer can be used to
		 * {@linkplain MultiValueMap#set(Object, Object) overwrite} existing header values,
		 * {@linkplain MultiValueMap#remove(Object) remove} values, or use any of the other
		 * {@link MultiValueMap} methods.
		 * @param cookiesConsumer a function that consumes the cookies map
		 * @return this builder
		 */
		Builder cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * Set the body of the request to the given {@code BodyInserter}.
		 * @param inserter the {@code BodyInserter} that writes to the request
		 * @return this builder
		 */
		Builder body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * Set the body of the request to the given {@code Publisher} and return it.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param elementClass the class of elements contained in the publisher
		 * @param <S> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return the built request
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, Class<S> elementClass);

		/**
		 * Set the body of the request to the given {@code Publisher} and return it.
		 * @param publisher the {@code Publisher} to write to the request
		 * @param typeReference a type reference describing the elements contained in the publisher
		 * @param <S> the type of the elements contained in the publisher
		 * @param <P> the type of the {@code Publisher}
		 * @return the built request
		 */
		<S, P extends Publisher<S>> Builder body(P publisher, ParameterizedTypeReference<S> typeReference);

		/**
		 * Set the attribute with the given name to the given value.
		 * @param name the name of the attribute to add
		 * @param value the value of the attribute to add
		 * @return this builder
		 */
		Builder attribute(String name, Object value);

		/**
		 * Manipulate the request attributes with the given consumer. The attributes provided to
		 * the consumer are "live", so that the consumer can be used to inspect attributes,
		 * remove attributes, or use any of the other map-provided methods.
		 * @param attributesConsumer a function that consumes the attributes
		 * @return this builder
		 */
		Builder attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * Build the request.
		 */
		ClientRequest build();
	}

}
