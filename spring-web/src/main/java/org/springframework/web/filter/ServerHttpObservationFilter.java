/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.filter;

import java.io.IOException;
import java.util.Optional;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerHttpObservationDocumentation;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;


/**
 * {@link jakarta.servlet.Filter} that creates {@link Observation observations}
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
 */
public class ServerHttpObservationFilter extends OncePerRequestFilter {

	/**
	 * Name of the request attribute holding the {@link ServerRequestObservationContext} for the current observation.
	 */
	public static final String CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE =
			ServerHttpObservationFilter.class.getName() + ".context";

	private static final String CURRENT_OBSERVATION_ATTRIBUTE =
			ServerHttpObservationFilter.class.getName() + ".observation";

	private static final String HANDLED_EXCEPTION_ATTRIBUTE =
			ServerHttpObservationFilter.class.getName() + ".handledException";

	private static final ServerRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION =
			new DefaultServerRequestObservationConvention();


	private final ObservationRegistry observationRegistry;

	private final ServerRequestObservationConvention observationConvention;


	/**
	 * Create an {@code HttpRequestsObservationFilter} that records observations
	 * against the given {@link ObservationRegistry}. The default
	 * {@link DefaultServerRequestObservationConvention convention} will be used.
	 * @param observationRegistry the registry to use for recording observations
	 */
	public ServerHttpObservationFilter(ObservationRegistry observationRegistry) {
		this(observationRegistry, DEFAULT_OBSERVATION_CONVENTION);
	}

	/**
	 * Create an {@code HttpRequestsObservationFilter} that records observations
	 * against the given {@link ObservationRegistry} with a custom convention.
	 * @param observationRegistry the registry to use for recording observations
	 * @param observationConvention the convention to use for all recorded observations
	 */
	public ServerHttpObservationFilter(ObservationRegistry observationRegistry, ServerRequestObservationConvention observationConvention) {
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
	}


	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	@Override
	@SuppressWarnings("try")
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		Observation observation = createOrFetchObservation(request, response);
		try (Observation.Scope scope = observation.openScope()) {
			onScopeOpened(scope, request, response);
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			observation.error(unwrapServletException(ex));
			response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			throw ex;
		}
		finally {
			// If async is started during the first dispatch, register a listener for completion notification.
			if (request.isAsyncStarted() && request.getDispatcherType() == DispatcherType.REQUEST) {
				request.getAsyncContext().addListener(new ObservationAsyncListener(observation, request));
			}
			// scope is opened for ASYNC dispatches, but the observation will be closed
			// by the async listener.
			else if (!isAsyncDispatch(request)) {
				recordError(observation, request);
				observation.stop();
			}
		}
	}

	/**
	 * Notify this filter that a new {@link Observation.Scope} is opened for the
	 * observation that was just created.
	 * @param scope the newly opened observation scope
	 * @param request the HTTP client request
	 * @param response the filter's response
	 * @since 6.2
	 */
	protected void onScopeOpened(Observation.Scope scope, HttpServletRequest request, HttpServletResponse response) {
	}

	private Observation createOrFetchObservation(HttpServletRequest request, HttpServletResponse response) {
		Observation observation = (Observation) request.getAttribute(CURRENT_OBSERVATION_ATTRIBUTE);
		if (observation == null) {
			ServerRequestObservationContext context = new ServerRequestObservationContext(request, response);
			observation = ServerHttpObservationDocumentation.HTTP_SERVLET_SERVER_REQUESTS.observation(
					this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> context, this.observationRegistry)
					.start();
			request.setAttribute(CURRENT_OBSERVATION_ATTRIBUTE, observation);
			if (!observation.isNoop()) {
				request.setAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observation.getContext());
			}
		}
		return observation;
	}


	/**
	 * Get the current {@link ServerRequestObservationContext observation context} from the given request, if available.
	 * @param request the current request
	 * @return the current observation context
	 */
	public static Optional<ServerRequestObservationContext> findObservationContext(HttpServletRequest request) {
		return Optional.ofNullable(
				(ServerRequestObservationContext) request.getAttribute(CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE));
	}

	/**
	 * Record an exception that was handled by the web framework or the
	 * application on the observation for the current request. The exception
	 * is reported to the {@link Observation} by this filter when the exchange
	 * completes, so that all handlers, including tracing, are notified.
	 * <p>This has no effect if there is no observation for the request.
	 * @param request the current request
	 * @param error the handled exception
	 * @since 7.1
	 */
	public static void recordHandledException(HttpServletRequest request, Throwable error) {
		if (findObservationContext(request).isPresent()) {
			request.setAttribute(HANDLED_EXCEPTION_ATTRIBUTE, error);
		}
	}

	private static void recordError(Observation observation, ServletRequest request) {
		Throwable error = (Throwable) request.getAttribute(HANDLED_EXCEPTION_ATTRIBUTE);
		if (error == null) {
			error = fetchException(request);
		}
		if (error != null) {
			observation.error(error);
		}
	}

	static @Nullable Throwable fetchException(ServletRequest request) {
		return (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
	}

	static Throwable unwrapServletException(Throwable ex) {
		if (ex instanceof ServletException) {
			Throwable cause = ex.getCause();
			if (cause != null) {
				return cause;
			}
		}
		return ex;
	}


	private static class ObservationAsyncListener implements AsyncListener {

		private final Observation currentObservation;

		private final ServletRequest request;

		public ObservationAsyncListener(Observation currentObservation, ServletRequest request) {
			this.currentObservation = currentObservation;
			this.request = request;
		}

		@Override
		public void onStartAsync(AsyncEvent event) {
			event.getAsyncContext().addListener(this);
		}

		@Override
		public void onTimeout(AsyncEvent event) {
			// The container must perform an error dispatch after.
		}

		@Override
		public void onComplete(AsyncEvent event) {
			recordError(this.currentObservation, this.request);
			this.currentObservation.stop();
		}

		@Override
		public void onError(AsyncEvent event) {
			this.currentObservation.error(unwrapServletException(event.getThrowable()));
		}
	}

}
