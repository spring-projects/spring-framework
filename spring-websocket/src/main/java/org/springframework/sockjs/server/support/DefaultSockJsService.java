/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.sockjs.server.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.Cookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.TransportType;
import org.springframework.sockjs.server.AbstractSockJsService;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportHandlerRegistrar;
import org.springframework.sockjs.server.TransportHandlerRegistry;
import org.springframework.util.Assert;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSockJsService extends AbstractSockJsService implements TransportHandlerRegistry, InitializingBean {

	private static final AtomicLong webSocketSessionIdSuffix = new AtomicLong();


	private final SockJsHandler sockJsHandler;

	private TaskScheduler sessionTimeoutScheduler;

	private final Map<String, SockJsSessionSupport> sessions = new ConcurrentHashMap<String, SockJsSessionSupport>();

	private final Map<TransportType, TransportHandler> transportHandlers = new HashMap<TransportType, TransportHandler>();


	/**
	 * Class constructor...
	 *
	 */
	public DefaultSockJsService(String prefix, SockJsHandler sockJsHandler) {
		super(prefix);
		Assert.notNull(sockJsHandler, "sockJsHandler is required");
		this.sockJsHandler = sockJsHandler;
		this.sessionTimeoutScheduler = createScheduler("SockJs-sessionTimeout-");
		new DefaultTransportHandlerRegistrar().registerTransportHandlers(this);
	}

	/**
	 * A scheduler instance to use for scheduling periodic expires session cleanup.
	 * <p>
	 * By default a {@link ThreadPoolTaskScheduler} with default settings is used.
	 */
	public TaskScheduler getSessionTimeoutScheduler() {
		return this.sessionTimeoutScheduler;
	}

	public void setSessionTimeoutScheduler(TaskScheduler sessionTimeoutScheduler) {
		Assert.notNull(sessionTimeoutScheduler, "sessionTimeoutScheduler is required");
		this.sessionTimeoutScheduler = sessionTimeoutScheduler;
	}

	@Override
	public void registerHandler(TransportHandler transportHandler) {
		Assert.notNull(transportHandler, "transportHandler is required");
		this.transportHandlers.put(transportHandler.getTransportType(), transportHandler);
	}

	public void setTransportHandlerRegistrar(TransportHandlerRegistrar registrar) {
		Assert.notNull(registrar, "registrar is required");
		this.transportHandlers.clear();
		registrar.registerTransportHandlers(this);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		this.sessionTimeoutScheduler.scheduleAtFixedRate(new Runnable() {
			public void run() {
				try {
					int count = sessions.size();
					if (logger.isTraceEnabled() && (count != 0)) {
						logger.trace("Checking " + count + " session(s) for timeouts [" + getPrefix() + "]");
					}
					for (SockJsSessionSupport session : sessions.values()) {
						if (session.getTimeSinceLastActive() > getDisconnectDelay()) {
							if (logger.isTraceEnabled()) {
								logger.trace("Removing " + session + " for [" + getPrefix() + "]");
							}
							session.close();
							sessions.remove(session.getId());
						}
					}
					if (logger.isTraceEnabled() && (count != 0)) {
						logger.trace(sessions.size() + " remaining session(s) [" + getPrefix() + "]");
					}
				}
				catch (Throwable t) {
					logger.error("Failed to complete session timeout checks for [" + getPrefix() + "]", t);
				}
			}
		}, getDisconnectDelay());
	}

	@Override
	protected void handleRequestInternal(ServerHttpRequest request, ServerHttpResponse response,
			String sessionId, TransportType transportType) throws Exception {

		TransportHandler transportHandler = this.transportHandlers.get(transportType);

		if (transportHandler == null) {
			logger.debug("Transport handler not found");
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		HttpMethod supportedMethod = transportType.getHttpMethod();
		if (!supportedMethod.equals(request.getMethod())) {
			if (HttpMethod.OPTIONS.equals(request.getMethod()) && transportType.isCorsSupported()) {
				response.setStatusCode(HttpStatus.NO_CONTENT);
				addCorsHeaders(request, response, supportedMethod, HttpMethod.OPTIONS);
				addCacheHeaders(response);
				response.getBody(); // ensure headers are flushed (TODO!)
			}
			else {
				List<HttpMethod> supportedMethods = Arrays.asList(supportedMethod);
				if (transportType.isCorsSupported()) {
					supportedMethods.add(HttpMethod.OPTIONS);
				}
				sendMethodNotAllowed(response, supportedMethods);
			}
			return;
		}

		SockJsSessionSupport session = getSockJsSession(sessionId, transportHandler);
		if (session == null) {
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		addNoCacheHeaders(response);

		if (isJsessionIdCookieNeeded()) {
			Cookie cookie = request.getCookies().getCookie("JSESSIONID");
			String jsid = (cookie != null) ? cookie.getValue() : "dummy";
			// TODO: Jetty sets Expires header, so bypass Cookie object for now
			response.getHeaders().set("Set-Cookie", "JSESSIONID=" + jsid + ";path=/");	// TODO
		}

		if (transportType.isCorsSupported()) {
			addCorsHeaders(request, response);
		}

		transportHandler.handleRequest(request, response, session);

		response.close(); // ensure headers are flushed (TODO !!)
	}

	public SockJsSessionSupport getSockJsSession(String sessionId, TransportHandler transportHandler) {

		TransportType transportType = transportHandler.getTransportType();

		// Always create new session for WebSocket requests
		sessionId = TransportType.WEBSOCKET.equals(transportType) ?
				sessionId + "#" + webSocketSessionIdSuffix.getAndIncrement() : sessionId;

		SockJsSessionSupport session = this.sessions.get(sessionId);
		if (session != null) {
			return session;
		}

		if (TransportType.XHR_SEND.equals(transportType) || TransportType.JSONP_SEND.equals(transportType)) {
			logger.debug(transportType + " did not find session");
			return null;
		}

		synchronized (this.sessions) {
			session = this.sessions.get(sessionId);
			if (session != null) {
				return session;
			}

			logger.debug("Creating new session with session id \"" + sessionId + "\"");
			session = transportHandler.createSession(sessionId, this.sockJsHandler, this);
			this.sessions.put(sessionId, session);

			return session;
		}
	}

}
