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

package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.http.Cookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.DefaultHandshakeHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.support.ServerWebSocketSessionInitializer;
import org.springframework.web.socket.sockjs.AbstractSockJsService;
import org.springframework.web.socket.sockjs.AbstractSockJsSession;
import org.springframework.web.socket.sockjs.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.SockJsSessionFactory;
import org.springframework.web.socket.sockjs.TransportErrorException;
import org.springframework.web.socket.sockjs.TransportHandler;
import org.springframework.web.socket.sockjs.TransportType;
import org.springframework.web.socket.sockjs.support.Jackson2SockJsMessageCodec;


/**
 * A default implementation of {@link SockJsService} adding support for transport handling
 * and session management. See {@link AbstractSockJsService} base class for important
 * details on request mapping.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSockJsService extends AbstractSockJsService {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", DefaultSockJsService.class.getClassLoader());

	private static final boolean jacksonPresent = ClassUtils.isPresent(
			"org.codehaus.jackson.map.ObjectMapper", DefaultSockJsService.class.getClassLoader());


	private final Map<TransportType, TransportHandler> transportHandlers = new HashMap<TransportType, TransportHandler>();

	private SockJsMessageCodec messageCodec;

	private final Map<String, AbstractSockJsSession> sessions = new ConcurrentHashMap<String, AbstractSockJsSession>();

	private final ServerWebSocketSessionInitializer sessionInitializer = new ServerWebSocketSessionInitializer();

	private ScheduledFuture sessionCleanupTask;


	/**
	 * Create an instance with default {@link TransportHandler transport handler} types.
	 *
	 * @param taskScheduler a task scheduler for heart-beat messages and removing
	 *        timed-out sessions; the provided TaskScheduler should be declared as a
	 *        Spring bean to ensure it is initialized at start up and shut down when the
	 *        application stops.
	 */
	public DefaultSockJsService(TaskScheduler taskScheduler) {
		super(taskScheduler);
		addTransportHandlers(getDefaultTransportHandlers());
		initMessageCodec();
	}

	protected void initMessageCodec() {
		if (jackson2Present) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
		else if (jacksonPresent) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}

	/**
	 * Create an instance by overriding or replacing completely the default
	 * {@link TransportHandler transport handler} types.
	 *
	 * @param taskScheduler a task scheduler for heart-beat messages and removing
	 *        timed-out sessions; the provided TaskScheduler should be declared as a
	 *        Spring bean to ensure it is initialized at start up and shut down when the
	 *        application stops.
	 * @param transportHandlers the transport handlers to use (replaces the default ones);
	 *        can be {@code null}.
	 * @param transportHandlerOverrides zero or more overrides to the default transport
	 *        handler types.
	 */
	public DefaultSockJsService(TaskScheduler taskScheduler, Set<TransportHandler> transportHandlers,
			TransportHandler... transportHandlerOverrides) {

		super(taskScheduler);

		initMessageCodec();

		if (!CollectionUtils.isEmpty(transportHandlers)) {
			addTransportHandlers(transportHandlers);
		}
		if (!ObjectUtils.isEmpty(transportHandlerOverrides)) {
			addTransportHandlers(Arrays.asList(transportHandlerOverrides));
		}

		if (this.transportHandlers.isEmpty()) {
			logger.warn("No transport handlers");
		}
	}


	protected final Set<TransportHandler> getDefaultTransportHandlers() {
		Set<TransportHandler> result = new HashSet<TransportHandler>();
		result.add(new XhrPollingTransportHandler());
		result.add(new XhrTransportHandler());
		result.add(new JsonpPollingTransportHandler());
		result.add(new JsonpTransportHandler());
		result.add(new XhrStreamingTransportHandler());
		result.add(new EventSourceTransportHandler());
		result.add(new HtmlFileTransportHandler());
		try {
			result.add(new WebSocketTransportHandler(new DefaultHandshakeHandler()));
		}
		catch (Exception ex) {
			if (logger.isWarnEnabled()) {
				logger.warn("Failed to add default WebSocketTransportHandler: " + ex.getMessage());
			}
		}
		return result;
	}

	protected void addTransportHandlers(Collection<TransportHandler> handlers) {
		for (TransportHandler handler : handlers) {
			handler.setSockJsConfiguration(this);
			this.transportHandlers.put(handler.getTransportType(), handler);
		}
	}


	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		this.messageCodec = messageCodec;
	}

	public SockJsMessageCodec getMessageCodec() {
		return this.messageCodec;
	}

	@Override
	public SockJsMessageCodec getMessageCodecRequired() {
		Assert.state(this.messageCodec != null, "A SockJsMessageCodec is required but not available."
				+ " Either add Jackson 2 or Jackson 1.x to the classpath, or configure a SockJsMessageCode");
		return this.messageCodec;
	}

	public Map<TransportType, TransportHandler> getTransportHandlers() {
		return Collections.unmodifiableMap(this.transportHandlers);
	}

	@Override
	protected void handleRawWebSocketRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler webSocketHandler) throws IOException {

		if (isWebSocketEnabled()) {
			TransportHandler transportHandler = this.transportHandlers.get(TransportType.WEBSOCKET);
			if (transportHandler != null) {
				if (transportHandler instanceof HandshakeHandler) {
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
			String sessionId, TransportType transportType, WebSocketHandler webSocketHandler)
					throws IOException, TransportErrorException {

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
				addCorsHeaders(request, response, HttpMethod.OPTIONS, supportedMethod);
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

		AbstractSockJsSession session = getSockJsSession(sessionId, webSocketHandler,
				transportHandler, request, response);

		if (session != null) {
			if (transportType.sendsNoCacheInstruction()) {
				addNoCacheHeaders(response);
			}

			if (transportType.setsJsessionId() && isJsessionIdCookieRequired()) {
				Cookie cookie = request.getCookies().getCookie("JSESSIONID");
				String jsid = (cookie != null) ? cookie.getValue() : "dummy";
				// TODO: bypass use of Cookie object (causes Jetty to set Expires header)
				response.getHeaders().set("Set-Cookie", "JSESSIONID=" + jsid + ";path=/");
			}

			if (transportType.supportsCors()) {
				addCorsHeaders(request, response);
			}
		}

		transportHandler.handleRequest(request, response, webSocketHandler, session);
	}

	protected AbstractSockJsSession getSockJsSession(String sessionId, WebSocketHandler handler,
			TransportHandler transportHandler, ServerHttpRequest request, ServerHttpResponse response) {

		AbstractSockJsSession session = this.sessions.get(sessionId);
		if (session != null) {
			return session;
		}

		if (transportHandler instanceof SockJsSessionFactory) {
			SockJsSessionFactory sessionFactory = (SockJsSessionFactory) transportHandler;

			synchronized (this.sessions) {
				session = this.sessions.get(sessionId);
				if (session != null) {
					return session;
				}
				if (this.sessionCleanupTask == null) {
					scheduleSessionTask();
				}
				logger.debug("Creating new session with session id \"" + sessionId + "\"");
				session = sessionFactory.createSession(sessionId, handler);
				String protocol = null; // TODO: https://github.com/sockjs/sockjs-client/issues/130
				this.sessionInitializer.initialize(request, response, protocol, session);
				this.sessions.put(sessionId, session);
				return session;
			}
		}

		return null;
	}

	private void scheduleSessionTask() {
		this.sessionCleanupTask = getTaskScheduler().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				try {
					int count = sessions.size();
					if (logger.isTraceEnabled() && (count != 0)) {
						logger.trace("Checking " + count + " session(s) for timeouts [" + getName() + "]");
					}
					for (AbstractSockJsSession session : sessions.values()) {
						if (session.getTimeSinceLastActive() > getDisconnectDelay()) {
							if (logger.isTraceEnabled()) {
								logger.trace("Removing " + session + " for [" + getName() + "]");
							}
							session.close();
							sessions.remove(session.getId());
						}
					}
					if (logger.isTraceEnabled() && (count != 0)) {
						logger.trace(sessions.size() + " remaining session(s) [" + getName() + "]");
					}
				}
				catch (Throwable t) {
					logger.error("Failed to complete session timeout checks for [" + getName() + "]", t);
				}
			}
		}, getDisconnectDelay());
	}

}
