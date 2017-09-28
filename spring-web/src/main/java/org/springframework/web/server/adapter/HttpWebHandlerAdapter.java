/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.server.adapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
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
	 * <p>Servlet containers do not expose a notification when a client disconnects,
	 * e.g. <a href="https://java.net/jira/browse/SERVLET_SPEC-44">SERVLET_SPEC-44</a>.
	 * <p>To avoid filling logs with unnecessary stack traces, we make an
	 * effort to identify such network failures on a per-server basis, and then
	 * log under a separate log category a simple one-line message at DEBUG level
	 * or a full stack trace only at TRACE level.
	 */
	private static final String DISCONNECTED_CLIENT_LOG_CATEGORY =
			"org.springframework.web.server.DisconnectedClient";

	/**
	 * Tomcat: ClientAbortException or EOFException
	 * Jetty: EofException
	 * WildFly, GlassFish: java.io.IOException "Broken pipe" (already covered)
	 * <p>TODO:
	 * This definition is currently duplicated between HttpWebHandlerAdapter
	 * and AbstractSockJsSession. It is a candidate for a common utility class.
	 * @see #indicatesDisconnectedClient(Throwable)
	 */
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS =
			new HashSet<>(Arrays.asList("ClientAbortException", "EOFException", "EofException"));


	private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);

	private static final Log disconnectedClientLogger = LogFactory.getLog(DISCONNECTED_CLIENT_LOG_CATEGORY);


	private WebSessionManager sessionManager = new DefaultWebSessionManager();

	@Nullable
	private ServerCodecConfigurer codecConfigurer;

	@Nullable
	private LocaleContextResolver localeContextResolver;


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
		Assert.notNull(codecConfigurer, "ServerCodecConfigurer must not be null");
		this.codecConfigurer = codecConfigurer;
	}

	/**
	 * Configure a custom {@link LocaleContextResolver}. The provided instance is set on
	 * each created {@link DefaultServerWebExchange}.
	 * <p>By default this is set to
	 * {@link org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver}.
	 * @param localeContextResolver the locale context resolver to use
	 */
	public void setLocaleContextResolver(LocaleContextResolver localeContextResolver) {
		this.localeContextResolver = localeContextResolver;
	}

	/**
	 * Return the configured {@link ServerCodecConfigurer}.
	 */
	public ServerCodecConfigurer getCodecConfigurer() {
		return (this.codecConfigurer != null ? this.codecConfigurer : ServerCodecConfigurer.create());
	}

	/**
	 * Return the configured {@link LocaleContextResolver}.
	 */
	public LocaleContextResolver getLocaleContextResolver() {
		return (this.localeContextResolver != null ?
				this.localeContextResolver : new AcceptHeaderLocaleContextResolver());
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		ServerWebExchange exchange = createExchange(request, response);
		return getDelegate().handle(exchange)
				.onErrorResume(ex -> {
					response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
					logHandleFailure(ex);
					return Mono.empty();
				})
				.then(Mono.defer(response::setComplete));
	}

	protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultServerWebExchange(request, response, this.sessionManager,
				getCodecConfigurer(), getLocaleContextResolver());
	}

	private void logHandleFailure(Throwable ex) {
		if (indicatesDisconnectedClient(ex)) {
			if (disconnectedClientLogger.isTraceEnabled()) {
				disconnectedClientLogger.trace("Looks like the client has gone away", ex);
			}
			else if (disconnectedClientLogger.isDebugEnabled()) {
				disconnectedClientLogger.debug("Looks like the client has gone away: " + ex +
						" (For a full stack trace, set the log category '" + DISCONNECTED_CLIENT_LOG_CATEGORY +
						"' to TRACE level.)");
			}
		}
		else {
			logger.error("Failed to handle request", ex);
		}
	}

	private boolean indicatesDisconnectedClient(Throwable ex)  {
		String message = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
		message = (message != null ? message.toLowerCase() : "");
		String className = ex.getClass().getSimpleName();
		return (message.contains("broken pipe") || DISCONNECTED_CLIENT_EXCEPTIONS.contains(className));
	}

}
