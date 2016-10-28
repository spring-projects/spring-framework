/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.util.Assert;

/**
 * Reactive Web client supporting the HTTP/1.1 protocol. Main entry point is through the
 * {@link #exchange(ClientRequest)} method, or through the
 * {@link #retrieveMono(ClientRequest, Class)} and {@link #retrieveFlux(ClientRequest, Class)}
 * convenience methods.
 *
 * <p>For example:
 * <pre class="code">
 * WebClient client = WebClient.create(new ReactorClientHttpConnector());
 * ClientRequest&lt;Void&gt; request = ClientRequest.GET("http://example.com/resource").build();
 *
 * Mono&lt;String&gt; result = client
 *   .exchange(request)
 *   .then(response -> response.body(BodyExtractors.toMono(String.class)));
 * </pre>
 * <p>or, by using {@link #retrieveMono(ClientRequest, Class)}:
 * <pre class="code">
 * Mono&lt;String&gt; result = client.retrieveMono(request, String.class);
 * </pre>
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface WebClient {

	/**
	 * Exchange the given request for a response mono. Invoking this method performs the actual
	 * HTTP request/response exchange.
	 * <p>Note that this method will <strong>not</strong> publish an exception if the response
	 * has a 4xx or 5xx status code; as opposed to {@link #retrieveMono(ClientRequest, Class)} and
	 * {@link #retrieveFlux(ClientRequest, Class)}.
	 * @param request the request to exchange
	 * @return the response, wrapped in a {@code Mono}
	 */
	Mono<ClientResponse> exchange(ClientRequest<?> request);

	/**
	 * Retrieve the body of the response as a {@code Mono}. A 4xx or 5xx status
	 * code in the response will result in a {@link WebClientException} published in the returned
	 * {@code Mono}.
	 * @param request the request to exchange
	 * @param elementClass the class of element in the {@code Mono}
	 * @param <T> the element type
	 * @return the response body as a mono
	 * @see ExchangeFilterFunctions#clientOrServerError()
	 */
	<T> Mono<T> retrieveMono(ClientRequest<?> request, Class<? extends T> elementClass);

	/**
	 * Retrieve the body of the response as a {@code Flux}. A 4xx or 5xx status
	 * code in the response will result in a {@link WebClientException} published in the returned
	 * {@code Flux}.
	 * @param request the request to exchange
	 * @param elementClass the class of element in the {@code Flux}
	 * @param <T> the element type
	 * @return the response body as a flux
	 * @see ExchangeFilterFunctions#clientOrServerError()
	 */
	<T> Flux<T> retrieveFlux(ClientRequest<?> request, Class<? extends T> elementClass);


	/**
	 * Create a new instance of {@code WebClient} with the given connector. This method uses
	 * {@linkplain WebClientStrategies#withDefaults() default strategies}.
	 * @param connector the connector to create connections
	 * @return the created client
	 */
	static WebClient create(ClientHttpConnector connector) {
		return builder(connector).build();
	}

	/**
	 * Return a builder for a {@code WebClient}.
	 * @param connector the connector to create connections
	 * @return a web client builder
	 */
	static Builder builder(ClientHttpConnector connector) {
		Assert.notNull(connector, "'connector' must not be null");
		return new DefaultWebClientBuilder(connector);
	}

	/**
	 * A mutable builder for a {@link WebClient}.
	 */
	interface Builder {

		/**
		 * Replaces the default strategies with the ones provided by the given
		 * {@code WebClientStrategies}.
		 * @param strategies the strategies to use
		 * @return this builder
		 */
		Builder strategies(WebClientStrategies strategies);

		/**
		 * Adds a filter function <strong>before</strong> the currently registered filters (if any).
		 * @param filter the filter to add
		 * @return this builder
		 */
		Builder filter(ExchangeFilterFunction filter);

		/**
		 * Builds the {@code WebClient}.
		 * @return the built client
		 */
		WebClient build();
	}


}
