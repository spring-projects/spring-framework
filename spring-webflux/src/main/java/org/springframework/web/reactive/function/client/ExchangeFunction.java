/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import reactor.core.publisher.Mono;

/**
 * Represents a function that exchanges a {@linkplain ClientRequest request} for a (delayed)
 * {@linkplain ClientResponse}. Can be used as an alternative to {@link WebClient}.
 *
 * <p>For example:
 * <pre class="code">
 * ExchangeFunction exchangeFunction =
 *         ExchangeFunctions.create(new ReactorClientHttpConnector());
 *
 * URI url = URI.create("https://example.com/resource");
 * ClientRequest request = ClientRequest.create(HttpMethod.GET, url).build();
 *
 * Mono&lt;String&gt; bodyMono = exchangeFunction
 *     .exchange(request)
 *     .flatMap(response -&gt; response.bodyToMono(String.class));
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
@FunctionalInterface
public interface ExchangeFunction {

	/**
	 * Exchange the given request for a {@link ClientResponse} promise.
	 * <p><strong>Note:</strong> When calling this method from an
	 * {@link ExchangeFilterFunction} that handles the response in some way,
	 * extra care must be taken to always consume its content or otherwise
	 * propagate it downstream for further handling, for example by the
	 * {@link WebClient}. Please see the reference documentation for more
	 * details on this.
	 * @param request the request to exchange
	 * @return the delayed response
	 */
	Mono<ClientResponse> exchange(ClientRequest request);

	/**
	 * Filter the exchange function with the given {@code ExchangeFilterFunction},
	 * resulting in a filtered {@code ExchangeFunction}.
	 * @param filter the filter to apply to this exchange
	 * @return the filtered exchange
	 * @see ExchangeFilterFunction#apply(ExchangeFunction)
	 */
	default ExchangeFunction filter(ExchangeFilterFunction filter) {
		return filter.apply(this);
	}

}
