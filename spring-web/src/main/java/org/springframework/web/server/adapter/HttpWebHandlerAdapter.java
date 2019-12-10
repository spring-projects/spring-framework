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

package org.springframework.web.server.adapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Default adapter of {@link WebHandler} to the {@link HttpHandler} contract.
 *
 * <p>By default creates and configures a {@link DefaultServerWebExchange} and
 * then invokes the target {@code WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

	/**
	 * Dedicated log category for disconnected client exceptions.
	 * <p>Servlet containers dn't expose a a client disconnected callback, see
	 * <a href="https://github.com/eclipse-ee4j/servlet-api/issues/44">eclipse-ee4j/servlet-api#44</a>.
	 * <p>To avoid filling logs with unnecessary stack traces, we make an
	 * effort to identify such network failures on a per-server basis, and then
	 * log under a separate log category a simple one-line message at DEBUG level
	 * or a full stack trace only at TRACE level.
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.server.DisconnectedClient";

	 // Similar declaration exists in AbstractSockJsSession..
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS = new HashSet<>(
			Arrays.asList("AbortedException", "ClientAbortException", "EOFException", "EofException"));


	private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);

	private static final Log lostClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);


	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	private ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

	private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

	@Nullable
	private ForwardedHeaderTransformer forwardedHeaderTransformer;

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
	public ServerCodecConfigurer getCodecConfigurer() {
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
	public void setForwardedHeaderTransformer(ForwardedHeaderTransformer transformer) {
		Assert.notNull(transformer, "ForwardedHeaderTransformer is required");
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
			request = this.forwardedHeaderTransformer.apply(request);
		}
		ServerWebExchange exchange = createExchange(request, response);

		LogFormatUtils.traceDebug(logger, traceOn ->
				exchange.getLogPrefix() + formatRequest(exchange.getRequest()) +
						(traceOn ? ", headers=" + formatHeaders(exchange.getRequest().getHeaders()) : ""));

		return getDelegate().handle(exchange)
				.doOnSuccess(aVoid -> logResponse(exchange))
				.onErrorResume(ex -> handleUnresolvedError(exchange, ex))
				.then(Mono.defer(response::setComplete));
	}

	protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultServerWebExchange(request, response, this.sessionManager,
				getCodecConfigurer(), getLocaleContextResolver(), this.applicationContext);
	}

	private String formatRequest(ServerHttpRequest request) {
		String rawQuery = request.getURI().getRawQuery();
		String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
		return "HTTP " + request.getMethod() + " \"" + request.getPath() + query + "\"";
	}

	private void logResponse(ServerWebExchange exchange) {
		LogFormatUtils.traceDebug(logger, traceOn -> {
			HttpStatus status = exchange.getResponse().getStatusCode();
			return exchange.getLogPrefix() + "Completed " + (status != null ? status : "200 OK") +
					(traceOn ? ", headers=" + formatHeaders(exchange.getResponse().getHeaders()) : "");
		});
	}

	private String formatHeaders(HttpHeaders responseHeaders) {
		return this.enableLoggingRequestDetails ?
				responseHeaders.toString() : responseHeaders.isEmpty() ? "{}" : "{masked}";
	}

	private Mono<Void> handleUnresolvedError(ServerWebExchange exchange, Throwable ex) {
		ServerHttpRequest request = exchange.getRequest();
		ServerHttpResponse response = exchange.getResponse();
		String logPrefix = exchange.getLogPrefix();

		// Sometimes a remote call error can look like a disconnected client.
		// Try to set the response first before the "isDisconnectedClient" check.

		if (response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR)) {
			logger.error(logPrefix + "500 Server Error for " + formatRequest(request), ex);
			return Mono.empty();
		}
		else if (isDisconnectedClientError(ex)) {
			if (lostClientLogger.isTraceEnabled()) {
				lostClientLogger.trace(logPrefix + "Client went away", ex);
			}
			else if (lostClientLogger.isDebugEnabled()) {
				lostClientLogger.debug(logPrefix + "Client went away: " + ex +
						" (stacktrace at TRACE level for '" + DISCONNECTED_CLIENT_LOG_CATEGORY + "')");
			}
			return Mono.empty();
		}
		else {
			// After the response is committed, propagate errors to the server...
			logger.error(logPrefix + "Error [" + ex + "] for " + formatRequest(request) +
					", but ServerHttpResponse already committed (" + response.getStatusCode() + ")");
			return Mono.error(ex);
		}
	}

	private boolean isDisconnectedClientError(Throwable ex) {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		if (message != null) {
			String text = message.toLowerCase();
			if (text.contains("broken pipe") || text.contains("connection reset by peer")) {
				return true;
			}
		}
		return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName());
	}

}
