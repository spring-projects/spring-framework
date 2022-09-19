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

package org.springframework.web.observation.reactive;

import java.util.Optional;
import java.util.Set;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;


/**
 * {@link org.springframework.web.server.WebFilter} that creates {@link Observation observations}
 * for HTTP exchanges. This collects information about the execution time and
 * information gathered from the {@link HttpRequestsObservationContext}.
 * <p>Web Frameworks can fetch the current {@link HttpRequestsObservationContext context}
 * as a {@link #CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE request attribute} and contribute
 * additional information to it.
 * The configured {@link HttpRequestsObservationConvention} will use this context to collect
 * {@link io.micrometer.common.KeyValue metadata} and attach it to the observation.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class HttpRequestsObservationWebFilter implements WebFilter {

	/**
	 * Name of the request attribute holding the {@link HttpRequestsObservationContext context} for the current observation.
	 */
	public static final String CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE = HttpRequestsObservationWebFilter.class.getName() + ".context";

	private static final HttpRequestsObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultHttpRequestsObservationConvention();

	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = Set.of("AbortedException",
			"ClientAbortException", "EOFException", "EofException");

	private final ObservationRegistry observationRegistry;

	private final HttpRequestsObservationConvention observationConvention;

	/**
	 * Create a {@code HttpRequestsObservationWebFilter} that records observations
	 * against the given {@link ObservationRegistry}. The default
	 * {@link DefaultHttpRequestsObservationConvention convention} will be used.
	 * @param observationRegistry the registry to use for recording observations
	 */
	public HttpRequestsObservationWebFilter(ObservationRegistry observationRegistry) {
		this(observationRegistry, new DefaultHttpRequestsObservationConvention());
	}

	/**
	 * Create a {@code HttpRequestsObservationWebFilter} that records observations
	 * against the given {@link ObservationRegistry} with a custom convention.
	 * @param observationRegistry the registry to use for recording observations
	 * @param observationConvention the convention to use for all recorded observations
	 */
	public HttpRequestsObservationWebFilter(ObservationRegistry observationRegistry, HttpRequestsObservationConvention observationConvention) {
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}

	/**
	 * Get the current {@link HttpRequestsObservationContext observation context} from the given request, if available.
	 * @param exchange the current exchange
	 * @return the current observation context
	 */
	public static Optional<HttpRequestsObservationContext> findObservationContext(ServerWebExchange exchange) {
		return Optional.ofNullable(exchange.getAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE));
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		HttpRequestsObservationContext observationContext = new HttpRequestsObservationContext(exchange);
		exchange.getAttributes().put(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);
		return chain.filter(exchange).transformDeferred(call -> filter(exchange, observationContext, call));
	}

	private Publisher<Void> filter(ServerWebExchange exchange, HttpRequestsObservationContext observationContext, Mono<Void> call) {
		Observation observation = HttpRequestsObservation.HTTP_REQUESTS.createNotStarted(this.observationConvention,
				DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);
		observation.start();
		return call.doOnEach(signal -> {
					Throwable throwable = signal.getThrowable();
					if (throwable != null) {
						if (DISCONNECTED_CLIENT_EXCEPTIONS.contains(throwable.getClass().getSimpleName())) {
							observationContext.setConnectionAborted(true);
						}
						observationContext.setError(throwable);
					}
					onTerminalSignal(observation, exchange);
				})
				.doOnCancel(() -> {
					observationContext.setConnectionAborted(true);
					observation.stop();
				});
	}

	private void onTerminalSignal(Observation observation, ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) {
			observation.stop();
		}
		else {
			response.beforeCommit(() -> {
				observation.stop();
				return Mono.empty();
			});
		}
	}

}
