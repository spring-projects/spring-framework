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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.Cookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionFactory;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.AbstractSockJsService;
import org.springframework.sockjs.server.ConfigurableTransportHandler;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportType;
import org.springframework.sockjs.server.transport.EventSourceTransportHandler;
import org.springframework.sockjs.server.transport.HtmlFileTransportHandler;
import org.springframework.sockjs.server.transport.JsonpPollingTransportHandler;
import org.springframework.sockjs.server.transport.JsonpTransportHandler;
import org.springframework.sockjs.server.transport.WebSocketSockJsHandlerAdapter;
import org.springframework.sockjs.server.transport.WebSocketTransportHandler;
import org.springframework.sockjs.server.transport.XhrPollingTransportHandler;
import org.springframework.sockjs.server.transport.XhrStreamingTransportHandler;
import org.springframework.sockjs.server.transport.XhrTransportHandler;
import org.springframework.util.Assert;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.server.DefaultHandshakeHandler;
import org.springframework.websocket.server.HandshakeHandler;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSockJsService extends AbstractSockJsService implements InitializingBean {

	private final Map<TransportType, TransportHandler> transportHandlers = new HashMap<TransportType, TransportHandler>();

	private final Map<TransportType, TransportHandler> transportHandlerOverrides = new HashMap<TransportType, TransportHandler>();

	private TaskSchedulerHolder sessionTimeoutSchedulerHolder;

	private final Map<String, SockJsSessionSupport> sessions = new ConcurrentHashMap<String, SockJsSessionSupport>();

	private final Map<SockJsHandler, WebSocketHandler> sockJsHandlers = new HashMap<SockJsHandler, WebSocketHandler>();


	public DefaultSockJsService(String prefix) {
		super(prefix);
		this.sessionTimeoutSchedulerHolder = new TaskSchedulerHolder("SockJs-sessionTimeout-");
	}

	public DefaultSockJsService(String prefix, TaskScheduler heartbeatScheduler, TaskScheduler sessionTimeoutScheduler) {
		super(prefix, heartbeatScheduler);
		Assert.notNull(sessionTimeoutScheduler, "sessionTimeoutScheduler is required");
		this.sessionTimeoutSchedulerHolder = new TaskSchedulerHolder(sessionTimeoutScheduler);
	}

	public void setTransportHandlers(TransportHandler... handlers) {
		this.transportHandlers.clear();
		for (TransportHandler handler : handlers) {
			this.transportHandlers.put(handler.getTransportType(), handler);
		}
	}

	public void setTransportHandlerOverrides(TransportHandler... handlers) {
		this.transportHandlerOverrides.clear();
		for (TransportHandler handler : handlers) {
			this.transportHandlerOverrides.put(handler.getTransportType(), handler);
		}
	}

	public void registerSockJsHandlers(Collection<SockJsHandler> sockJsHandlers) {
		for (SockJsHandler sockJsHandler : sockJsHandlers) {
			if (!this.sockJsHandlers.containsKey(sockJsHandler)) {
				this.sockJsHandlers.put(sockJsHandler, adaptSockJsHandler(sockJsHandler));
			}
		}
		configureTransportHandlers();
	}

	/**
	 * Adapt the {@link SockJsHandler} to the {@link WebSocketHandler} contract for
	 * <em>raw WebSocket</em> communication on SockJS path "/websocket".
	 */
	protected WebSocketSockJsHandlerAdapter adaptSockJsHandler(SockJsHandler sockJsHandler) {
		return new WebSocketSockJsHandlerAdapter(this, sockJsHandler);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		super.afterPropertiesSet();

		if (this.transportHandlers.isEmpty()) {
			if (isWebSocketEnabled() && (this.transportHandlerOverrides.get(TransportType.WEBSOCKET) == null)) {
				this.transportHandlers.put(TransportType.WEBSOCKET,
						new WebSocketTransportHandler(new DefaultHandshakeHandler()));
			}
			this.transportHandlers.put(TransportType.XHR, new XhrPollingTransportHandler());
			this.transportHandlers.put(TransportType.XHR_SEND, new XhrTransportHandler());
			this.transportHandlers.put(TransportType.JSONP, new JsonpPollingTransportHandler());
			this.transportHandlers.put(TransportType.JSONP_SEND, new JsonpTransportHandler());
			this.transportHandlers.put(TransportType.XHR_STREAMING, new XhrStreamingTransportHandler());
			this.transportHandlers.put(TransportType.EVENT_SOURCE, new EventSourceTransportHandler());
			this.transportHandlers.put(TransportType.HTML_FILE, new HtmlFileTransportHandler());
		}

		if (!this.transportHandlerOverrides.isEmpty()) {
			for (TransportHandler transportHandler : this.transportHandlerOverrides.values()) {
				this.transportHandlers.put(transportHandler.getTransportType(), transportHandler);
			}
		}

		configureTransportHandlers();

		this.sessionTimeoutSchedulerHolder.initialize();

		this.sessionTimeoutSchedulerHolder.getScheduler().scheduleAtFixedRate(new Runnable() {
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
	public void destroy() throws Exception {
		super.destroy();
		this.sessionTimeoutSchedulerHolder.destroy();
	}

	private void configureTransportHandlers() {
		for (TransportHandler h : this.transportHandlers.values()) {
			if (h instanceof ConfigurableTransportHandler) {
				((ConfigurableTransportHandler) h).setSockJsConfiguration(this);
				if (!this.sockJsHandlers.isEmpty()) {
					((ConfigurableTransportHandler) h).registerSockJsHandlers(this.sockJsHandlers.keySet());
					if (h instanceof HandshakeHandler) {
						((HandshakeHandler) h).registerWebSocketHandlers(this.sockJsHandlers.values());
					}
				}
			}
		}
	}

	@Override
	protected void handleRawWebSocketRequest(ServerHttpRequest request, ServerHttpResponse response,
			SockJsHandler sockJsHandler) throws Exception {

		if (isWebSocketEnabled()) {
			TransportHandler transportHandler = this.transportHandlers.get(TransportType.WEBSOCKET);
			if (transportHandler != null) {
				if (transportHandler instanceof HandshakeHandler) {
					WebSocketHandler webSocketHandler = this.sockJsHandlers.get(sockJsHandler);
					if (webSocketHandler == null) {
						webSocketHandler = adaptSockJsHandler(sockJsHandler);
					}
					((HandshakeHandler) transportHandler).doHandshake(request, response, webSocketHandler);
					return;
				}
			}
			logger.warn("No handler for raw WebSocket messages");
		}
		response.setStatusCode(HttpStatus.NOT_FOUND);
	}

	@Override
	protected void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			String sessionId, TransportType transportType, SockJsHandler sockJsHandler) throws Exception {

		TransportHandler transportHandler = this.transportHandlers.get(transportType);

		if (transportHandler == null) {
			logger.debug("Transport handler not found");
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		HttpMethod supportedMethod = transportType.getHttpMethod();
		if (!supportedMethod.equals(request.getMethod())) {
			if (HttpMethod.OPTIONS.equals(request.getMethod()) && transportType.supportsCors()) {
				response.setStatusCode(HttpStatus.NO_CONTENT);
				addCorsHeaders(request, response, supportedMethod, HttpMethod.OPTIONS);
				addCacheHeaders(response);
			}
			else {
				List<HttpMethod> supportedMethods = Arrays.asList(supportedMethod);
				if (transportType.supportsCors()) {
					supportedMethods.add(HttpMethod.OPTIONS);
				}
				sendMethodNotAllowed(response, supportedMethods);
			}
			return;
		}

		SockJsSessionSupport session = getSockJsSession(sessionId, sockJsHandler, transportHandler);

		if (session != null) {
			if (transportType.setsNoCacheHeader()) {
				addNoCacheHeaders(response);
			}

			if (transportType.setsJsessionIdCookie() && isJsessionIdCookieRequired()) {
				Cookie cookie = request.getCookies().getCookie("JSESSIONID");
				String jsid = (cookie != null) ? cookie.getValue() : "dummy";
				// TODO: bypass use of Cookie object (causes Jetty to set Expires header)
				response.getHeaders().set("Set-Cookie", "JSESSIONID=" + jsid + ";path=/");
			}

			if (transportType.supportsCors()) {
				addCorsHeaders(request, response);
			}
		}

		transportHandler.handleRequest(request, response, sockJsHandler, session);
	}

	public SockJsSessionSupport getSockJsSession(String sessionId, SockJsHandler sockJsHandler,
			TransportHandler transportHandler) {

		SockJsSessionSupport session = this.sessions.get(sessionId);
		if (session != null) {
			return session;
		}

		if (transportHandler instanceof SockJsSessionFactory) {
			SockJsSessionFactory<?> sessionFactory = (SockJsSessionFactory<?>) transportHandler;

			synchronized (this.sessions) {
				session = this.sessions.get(sessionId);
				if (session != null) {
					return session;
				}
				logger.debug("Creating new session with session id \"" + sessionId + "\"");
				session = (SockJsSessionSupport) sessionFactory.createSession(sessionId, sockJsHandler);
				this.sessions.put(sessionId, session);
				return session;
			}
		}

		return null;
	}

}
