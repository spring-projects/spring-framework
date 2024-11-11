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

package org.springframework.web.server.adapter;

import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.observability.DefaultSignalListener;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import org.springframework.context.ApplicationContext;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.reactive.observation.ServerHttpObservationDocumentation;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.http.server.reactive.observation.ServerRequestObservationConvention;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;
import org.springframework.web.util.DisconnectedClientHelper;

/**
 * Default adapter of {@link WebHandler} to the {@link HttpHandler} contract.
 *
 * <p>By default creates and configures a {@link DefaultServerWebExchange} and
 * then invokes the target {@code WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

	/**
	 * Log category to use for network failure after a client has gone away.
	 * @see DisconnectedClientHelper
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.server.DisconnectedClient";

	private static final DisconnectedClientHelper disconnectedClientHelper =
			new DisconnectedClientHelper(DISCONNECTED_CLIENT_LOG_CATEGORY);

	private static final ServerRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION =
			new DefaultServerRequestObservationConvention();


	private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);


	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;

	private ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	private ServerRequestObservationConvention observationConvention = DEFAULT_OBSERVATION_CONVENTION;

	@Nullable
	private ApplicationContext applicationContext;

	/** Whether to log potentially sensitive info (form data at DEBUG, headers at TRACE). */
	private boolean enableLoggingRequestDetails = false;


	public HttpWebHandlerAdapter(WebHandler delegate) {
		super(delegate);
	}


	/**
	 * Configure a custom {@link WebSessionManager} to use for managing web
	 * sessions. The provided instance is set on each created
	 * {@link DefaultServerWebExchange}.
	 * <p>By default this is set to {@link DefaultWebSessionManager}.
	 * @param sessionManager the session manager to use
	 */
	public void setSessionManager(WebSessionManager sessionManager) {
		Assert.notNull(sessionManager, "WebSessionManager must not be null");
		this.sessionManager = sessionManager;
	}

	/**
	 * Return the configured {@link WebSessionManager}.
	 */
	public WebSessionManager getSessionManager() {
		return this.sessionManager;
	}

	/**
	 * Configure a custom {@link ServerCodecConfigurer}. The provided instance is set on
	 * each created {@link DefaultServerWebExchange}.
	 * <p>By default this is set to {@link ServerCodecConfigurer#create()}.
	 * @param codecConfigurer the codec configurer to use
	 */
	public void setCodecConfigurer(ServerCodecConfigurer codecConfigurer) {
		Assert.notNull(codecConfigurer, "ServerCodecConfigurer is required");
		this.codecConfigurer = codecConfigurer;

		this.enableLoggingRequestDetails = false;
		this.codecConfigurer.getReaders().stream()
				.filter(LoggingCodecSupport.class::isInstance)
				.forEach(reader -> {
					if (((LoggingCodecSupport) reader).isEnableLoggingRequestDetails()) {
						this.enableLoggingRequestDetails = true;
					}
				});
	}

	/**
	 * Return the configured {@link ServerCodecConfigurer}.
	 */
	@SuppressWarnings("NullAway")
	public ServerCodecConfigurer getCodecConfigurer() {
		if (this.codecConfigurer == null) {
			setCodecConfigurer(ServerCodecConfigurer.create());
		}
		return this.codecConfigurer;
	}

	/**
	 * Configure a custom {@link LocaleContextResolver}. The provided instance is set on
	 * each created {@link DefaultServerWebExchange}.
	 * <p>By default this is set to
	 * {@link org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver}.
	 * @param resolver the locale context resolver to use
	 */
	public void setLocaleContextResolver(LocaleContextResolver resolver) {
		Assert.notNull(resolver, "LocaleContextResolver is required");
		this.localeContextResolver = resolver;
	}

	/**
	 * Return the configured {@link LocaleContextResolver}.
	 */
	public LocaleContextResolver getLocaleContextResolver() {
		return this.localeContextResolver;
	}

	/**
	 * Enable processing of forwarded headers, either extracting and removing,
	 * or remove only.
	 * <p>By default this is not set.
	 * @param transformer the transformer to use
	 * @since 5.1
	 */
	public void setForwardedHeaderTransformer(@Nullable ForwardedHeaderTransformer transformer) {
		this.forwardedHeaderTransformer = transformer;
	}

	/**
	 * Return the configured {@link ForwardedHeaderTransformer}.
	 * @since 5.1
	 */
	@Nullable
	public ForwardedHeaderTransformer getForwardedHeaderTransformer() {
		return this.forwardedHeaderTransformer;
	}

	/**
	 * Configure an {@link ObservationRegistry} for recording server exchange observations.
	 * By default, a {@link ObservationRegistry#NOOP no-op} instance will be used.
	 * @param observationRegistry the observation registry to use
	 * @since 6.1
	 */
	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Return the configured {@link ObservationRegistry}.
	 * @since 6.1
	 */
	public ObservationRegistry getObservationRegistry() {
		return this.observationRegistry;
	}

	/**
	 * Configure a {@link ServerRequestObservationConvention} for server exchanges observations.
	 * By default, a {@link DefaultServerRequestObservationConvention} instance will be used.
	 * @param observationConvention the observation convention to use
	 * @since 6.1
	 */
	public void setObservationConvention(ServerRequestObservationConvention observationConvention) {
		this.observationConvention = observationConvention;
	}

	/**
	 * Return the Observation convention configured for server exchanges observations.
	 * @since 6.1
	 */
	public ServerRequestObservationConvention getObservationConvention() {
		return this.observationConvention;
	}

	/**
	 * Configure the {@code ApplicationContext} associated with the web application,
	 * if it was initialized with one via
	 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder#applicationContext(ApplicationContext)}.
	 * @param applicationContext the context
	 * @since 5.0.3
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Return the configured {@code ApplicationContext}, if any.
	 * @since 5.0.3
	 */
	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * This method must be invoked after all properties have been set to
	 * complete initialization.
	 */
	public void afterPropertiesSet() {
		if (logger.isDebugEnabled()) {
			String value = this.enableLoggingRequestDetails ?
					"shown which may lead to unsafe logging of potentially sensitive data" :
					"masked to prevent unsafe logging of potentially sensitive data";
			logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails +
					"': form data and headers will be " + value);
		}
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		if (this.forwardedHeaderTransformer != null) {
			try {
				request = this.forwardedHeaderTransformer.apply(request);
			}
			catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to apply forwarded headers to " + formatRequest(request), ex);
				}
				response.setStatusCode(HttpStatus.BAD_REQUEST);
				return response.setComplete();
			}
		}
		ServerWebExchange exchange = createExchange(request, response);

		LogFormatUtils.traceDebug(logger, traceOn ->
				exchange.getLogPrefix() + formatRequest(exchange.getRequest()) +
						(traceOn ? ", headers=" + formatHeaders(exchange.getRequest().getHeaders()) : ""));

		ServerRequestObservationContext observationContext = new ServerRequestObservationContext(
				exchange.getRequest(), exchange.getResponse(), exchange.getAttributes());
		exchange.getAttributes().put(
				ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);

		return getDelegate().handle(exchange)
				.doOnSuccess(aVoid -> logResponse(exchange))
				.onErrorResume(ex -> handleUnresolvedError(exchange, observationContext, ex))
				.tap(() -> new ObservationSignalListener(observationContext))
				.then(exchange.cleanupMultipart())
				.then(Mono.defer(response::setComplete));
	}

	protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultServerWebExchange(request, response, this.sessionManager,
				getCodecConfigurer(), getLocaleContextResolver(), this.applicationContext);
	}

	/**
	 * Format the request for logging purposes including HTTP method and URL.
	 * <p>By default this prints the HTTP method, the URL path, and the query.
	 * @param request the request to format
	 * @return the String to display, never empty or {@code null}
	 */
	protected String formatRequest(ServerHttpRequest request) {
		String rawQuery = request.getURI().getRawQuery();
		String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
		return "HTTP " + request.getMethod() + " \"" + request.getPath() + query + "\"";
	}

	private void logResponse(ServerWebExchange exchange) {
		LogFormatUtils.traceDebug(logger, traceOn -> {
			HttpStatusCode status = exchange.getResponse().getStatusCode();
			return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK") +
					(traceOn ? ", headers=" + formatHeaders(exchange.getResponse().getHeaders()) : "");
		});
	}

	private String formatHeaders(HttpHeaders responseHeaders) {
		return this.enableLoggingRequestDetails ?
				responseHeaders.toString() : responseHeaders.isEmpty() ? "{}" : "{masked}";
	}

	private Mono<Void> handleUnresolvedError(
			ServerWebExchange exchange, ServerRequestObservationContext observationContext, Throwable ex) {

		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		String logPrefix = exchange.getLogPrefix();

		// Sometimes a remote call error can look like a disconnected client.
		// Try to set the response first before the "isDisconnectedClient" check.

		if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
			logger.error(logPrefix + "500 Server Error for " + formatRequest(request), ex);
			return Mono.empty();
		}
		else if (disconnectedClientHelper.checkAndLogClientDisconnectedException(ex)) {
			observationContext.setConnectionAborted(true);
			return Mono.empty();
		}
		else {
			// After the response is committed, propagate errors to the server...
			logger.error(logPrefix + "Error [" + ex + "] for " + formatRequest(request) +
					", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
			return Mono.error(ex);
		}
	}


	private final class ObservationSignalListener extends DefaultSignalListener<Void> {

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
				Throwable throwable = (Throwable) this.observationContext.getAttributes()
						.get(ExceptionHandlingWebHandler.HANDLED_WEB_EXCEPTION);
				if (throwable != null) {
					this.observation.error(throwable);
				}
				doOnTerminate(this.observationContext);
			}
		}

		@Override
		public void doOnError(Throwable error) throws Throwable {
			if (this.observationRecorded.compareAndSet(false, true)) {
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
