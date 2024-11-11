/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.filter.reactive;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerHttpObservationDocumentation;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * {@link org.springframework.web.server.WebFilter} that creates {@link Observation observations}
 * for HTTP exchanges. This collects information about the execution time and
 * information gathered from the {@link ServerRequestObservationContext}.
 * <p>Web Frameworks can fetch the current {@link ServerRequestObservationContext context}
 * as a {@link #CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE request attribute} and contribute
 * additional information to it.
 * The configured {@link ServerRequestObservationConvention} will use this context to collect
 * {@link io.micrometer.common.KeyValue metadata} and attach it to the observation.
 *
 * @author Brian Clozel
 * @since 6.0
 * @deprecated since 6.1 in favor of {@link WebHttpHandlerBuilder}.
 */
@Deprecated(since = "6.1", forRemoval = true)
public class ServerHttpObservationFilter implements WebFilter {

	/**
	 * Name of the request attribute holding the {@link ServerRequestObservationContext context} for the current observation.
	 */
	public static final String CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE = ServerHttpObservationFilter.class.getName() + ".context";

	private static final ServerRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultServerRequestObservationConvention();

	private final ObservationRegistry observationRegistry;

	private final ServerRequestObservationConvention observationConvention;

	/**
	 * Create an {@code HttpRequestsObservationWebFilter} that records observations
	 * against the given {@link ObservationRegistry}. The default
	 * {@link DefaultServerRequestObservationConvention convention} will be used.
	 * @param observationRegistry the registry to use for recording observations
	 */
	public ServerHttpObservationFilter(ObservationRegistry observationRegistry) {
		this(observationRegistry, DEFAULT_OBSERVATION_CONVENTION);
	}

	/**
	 * Create an {@code HttpRequestsObservationWebFilter} that records observations
	 * against the given {@link ObservationRegistry} with a custom convention.
	 * @param observationRegistry the registry to use for recording observations
	 * @param observationConvention the convention to use for all recorded observations
	 */
	public ServerHttpObservationFilter(ObservationRegistry observationRegistry, ServerRequestObservationConvention observationConvention) {
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}

	/**
	 * Get the current {@link ServerRequestObservationContext observation context} from the given request, if available.
	 * @param exchange the current exchange
	 * @return the current observation context
	 */
	public static Optional<ServerRequestObservationContext> findObservationContext(ServerWebExchange exchange) {
		return Optional.ofNullable(exchange.getAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE));
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerRequestObservationContext observationContext = new ServerRequestObservationContext(exchange.getRequest(),
				exchange.getResponse(), exchange.getAttributes());
		exchange.getAttributes().put(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);
		return chain.filter(exchange).tap(() -> new ObservationSignalListener(observationContext));
	}

	private final class ObservationSignalListener extends DefaultSignalListener<Void> {

		private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = Set.of("AbortedException",
				"ClientAbortException", "EOFException", "EofException");

		private final ServerRequestObservationContext observationContext;

		private final Observation observation;

		private final AtomicBoolean observationRecorded = new AtomicBoolean();

		ObservationSignalListener(ServerRequestObservationContext observationContext) {
			this.observationContext = observationContext;
			this.observation = ServerHttpObservationDocumentation.HTTP_REACTIVE_SERVER_REQUESTS.observation(observationConvention,
					DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, observationRegistry);
		}


		@Override
		public Context addToContext(Context originalContext) {
			return originalContext.put(ObservationThreadLocalAccessor.KEY, this.observation);
		}

		@Override
		public void doFirst() throws Throwable {
			this.observation.start();
		}

		@Override
		public void doOnCancel() throws Throwable {
			if (this.observationRecorded.compareAndSet(false, true)) {
				this.observationContext.setConnectionAborted(true);
				this.observation.stop();
			}
		}

		@Override
		public void doOnComplete() throws Throwable {
			if (this.observationRecorded.compareAndSet(false, true)) {
				doOnTerminate(this.observationContext);
			}
		}

		@Override
		public void doOnError(Throwable error) throws Throwable {
			if (this.observationRecorded.compareAndSet(false, true)) {
				if (DISCONNECTED_CLIENT_EXCEPTIONS.contains(error.getClass().getSimpleName())) {
					this.observationContext.setConnectionAborted(true);
				}
				this.observationContext.setError(error);
				doOnTerminate(this.observationContext);
			}
		}

		private void doOnTerminate(ServerRequestObservationContext context) {
			ServerHttpResponse response = context.getResponse();
			if (response != null) {
				if (response.isCommitted()) {
					this.observation.stop();
				}
				else {
					response.beforeCommit(() -> {
						this.observation.stop();
						return Mono.empty();
					});
				}
			}
		}
	}

}
