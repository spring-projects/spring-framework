/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.context.Lifecycle;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HandshakeInterceptorChain;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;
import org.springframework.web.socket.sockjs.support.AbstractSockJsService;

/**
 * A basic implementation of {@link org.springframework.web.socket.sockjs.SockJsService}
 * with support for SPI-based transport handling and session management.
 *
 * <p>Based on the {@link TransportHandler} SPI. {@code TransportHandlers} may
 * additionally implement the {@link SockJsSessionFactory} and {@link HandshakeHandler} interfaces.
 *
 * <p>See the {@link AbstractSockJsService} base class for important details on request mapping.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @since 4.0
 */
public class TransportHandlingSockJsService extends AbstractSockJsService implements SockJsServiceConfig, Lifecycle {

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", TransportHandlingSockJsService.class.getClassLoader());


	private final Map<TransportType, TransportHandler> handlers = new EnumMap<>(TransportType.class);

	@Nullable
	private SockJsMessageCodec messageCodec;

	private final List<HandshakeInterceptor> interceptors = new ArrayList<>();

	private final Map<String, SockJsSession> sessions = new ConcurrentHashMap<>();

	@Nullable
	private ScheduledFuture<?> sessionCleanupTask;

	private volatile boolean running;


	/**
	 * Create a TransportHandlingSockJsService with given {@link TransportHandler handler} types.
	 * @param scheduler a task scheduler for heart-beat messages and removing timed-out sessions;
	 * the provided TaskScheduler should be declared as a Spring bean to ensure it gets
	 * initialized at start-up and shuts down when the application stops
	 * @param handlers one or more {@link TransportHandler} implementations to use
	 */
	public TransportHandlingSockJsService(TaskScheduler scheduler, TransportHandler... handlers) {
		this(scheduler, Arrays.asList(handlers));
	}

	/**
	 * Create a TransportHandlingSockJsService with given {@link TransportHandler handler} types.
	 * @param scheduler a task scheduler for heart-beat messages and removing timed-out sessions;
	 * the provided TaskScheduler should be declared as a Spring bean to ensure it gets
	 * initialized at start-up and shuts down when the application stops
	 * @param handlers one or more {@link TransportHandler} implementations to use
	 */
	public TransportHandlingSockJsService(TaskScheduler scheduler, Collection<TransportHandler> handlers) {
		super(scheduler);

		if (CollectionUtils.isEmpty(handlers)) {
			logger.warn("No transport handlers specified for TransportHandlingSockJsService");
		}
		else {
			for (TransportHandler handler : handlers) {
				handler.initialize(this);
				this.handlers.put(handler.getTransportType(), handler);
			}
		}

		if (jackson2Present) {
			this.messageCodec = new Jackson2SockJsMessageCodec();
		}
	}


	/**
	 * Return the registered handlers per transport type.
	 */
	public Map<TransportType, TransportHandler> getTransportHandlers() {
		return Collections.unmodifiableMap(this.handlers);
	}

	/**
	 * The codec to use for encoding and decoding SockJS messages.
	 */
	public void setMessageCodec(SockJsMessageCodec messageCodec) {
		this.messageCodec = messageCodec;
	}

	@Override
	public SockJsMessageCodec getMessageCodec() {
		Assert.state(this.messageCodec != null, "A SockJsMessageCodec is required but not available: " +
				"Add Jackson to the classpath, or configure a custom SockJsMessageCodec.");
		return this.messageCodec;
	}

	/**
	 * Configure one or more WebSocket handshake request interceptors.
	 */
	public void setHandshakeInterceptors(@Nullable List<HandshakeInterceptor> interceptors) {
		this.interceptors.clear();
		if (interceptors != null) {
			this.interceptors.addAll(interceptors);
		}
	}

	/**
	 * Return the configured WebSocket handshake request interceptors.
	 */
	public List<HandshakeInterceptor> getHandshakeInterceptors() {
		return this.interceptors;
	}


	@Override
	public void start() {
		if (!isRunning()) {
			this.running = true;
			for (TransportHandler handler : this.handlers.values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).start();
				}
			}
		}
	}

	@Override
	public void stop() {
		if (isRunning()) {
			this.running = false;
			for (TransportHandler handler : this.handlers.values()) {
				if (handler instanceof Lifecycle) {
					((Lifecycle) handler).stop();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	@Override
	protected void handleRawWebSocketRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler) throws IOException {

		TransportHandler transportHandler = this.handlers.get(TransportType.WEBSOCKET);
		if (!(transportHandler instanceof HandshakeHandler)) {
			logger.error("No handler configured for raw WebSocket messages");
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, handler);
		HandshakeFailureException failure = null;

		try {
			Map<String, Object> attributes = new HashMap<>();
			if (!chain.applyBeforeHandshake(request, response, attributes)) {
				return;
			}
			((HandshakeHandler) transportHandler).doHandshake(request, response, handler, attributes);
			chain.applyAfterHandshake(request, response, null);
		}
		catch (HandshakeFailureException ex) {
			failure = ex;
		}
		catch (Exception ex) {
			failure = new HandshakeFailureException("Uncaught failure for request " + request.getURI(), ex);
		}
		finally {
			if (failure != null) {
				chain.applyAfterHandshake(request, response, failure);
				throw failure;
			}
		}
	}

	@Override
	protected void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler handler, String sessionId, String transport) throws SockJsException {

		TransportType transportType = TransportType.fromValue(transport);
		if (transportType == null) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue("Unknown transport type for " + request.getURI(), -1, true));
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		TransportHandler transportHandler = this.handlers.get(transportType);
		if (transportHandler == null) {
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue("No TransportHandler for " + request.getURI(), -1, true));
			}
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		SockJsException failure = null;
		HandshakeInterceptorChain chain = new HandshakeInterceptorChain(this.interceptors, handler);

		try {
			HttpMethod supportedMethod = transportType.getHttpMethod();
			if (supportedMethod != request.getMethod()) {
				if (request.getMethod() == HttpMethod.OPTIONS && transportType.supportsCors()) {
					if (checkOrigin(request, response, HttpMethod.OPTIONS, supportedMethod)) {
						response.setStatusCode(HttpStatus.NO_CONTENT);
						addCacheHeaders(response);
					}
				}
				else if (transportType.supportsCors()) {
					sendMethodNotAllowed(response, supportedMethod, HttpMethod.OPTIONS);
				}
				else {
					sendMethodNotAllowed(response, supportedMethod);
				}
				return;
			}

			SockJsSession session = this.sessions.get(sessionId);
			boolean isNewSession = false;
			if (session == null) {
				if (transportHandler instanceof SockJsSessionFactory) {
					Map<String, Object> attributes = new HashMap<>();
					if (!chain.applyBeforeHandshake(request, response, attributes)) {
						return;
					}
					SockJsSessionFactory sessionFactory = (SockJsSessionFactory) transportHandler;
					session = createSockJsSession(sessionId, sessionFactory, handler, attributes);
					isNewSession = true;
				}
				else {
					response.setStatusCode(HttpStatus.NOT_FOUND);
					if (logger.isDebugEnabled()) {
						logger.debug("Session not found, sessionId=" + sessionId +
								". The session may have been closed " +
								"(e.g. missed heart-beat) while a message was coming in.");
					}
					return;
				}
			}
			else {
				Principal principal = session.getPrincipal();
				if (principal != null && !principal.equals(request.getPrincipal())) {
					logger.debug("The user for the session does not match the user for the request.");
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				if (!transportHandler.checkSessionType(session)) {
					logger.debug("Session type does not match the transport type for the request.");
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
			}

			if (transportType.sendsNoCacheInstruction()) {
				addNoCacheHeaders(response);
			}
			if (transportType.supportsCors() && !checkOrigin(request, response)) {
				return;
			}

			transportHandler.handleRequest(request, response, handler, session);

			if (isNewSession && (response instanceof ServletServerHttpResponse)) {
				int status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
				if (HttpStatus.valueOf(status).is4xxClientError()) {
					this.sessions.remove(sessionId);
				}
			}

			chain.applyAfterHandshake(request, response, null);
		}
		catch (SockJsException ex) {
			failure = ex;
		}
		catch (Exception ex) {
			failure = new SockJsException("Uncaught failure for request " + request.getURI(), sessionId, ex);
		}
		finally {
			if (failure != null) {
				chain.applyAfterHandshake(request, response, failure);
				throw failure;
			}
		}
	}

	@Override
	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		if (!super.validateRequest(serverId, sessionId, transport)) {
			return false;
		}

		if (!getAllowedOrigins().isEmpty() && !getAllowedOrigins().contains("*") ||
				!getAllowedOriginPatterns().isEmpty()) {
			TransportType transportType = TransportType.fromValue(transport);
			if (transportType == null || !transportType.supportsOrigin()) {
				if (logger.isWarnEnabled()) {
					logger.warn("Origin check enabled but transport '" + transport + "' does not support it.");
				}
				return false;
			}
		}

		return true;
	}

	private SockJsSession createSockJsSession(String sessionId, SockJsSessionFactory sessionFactory,
			WebSocketHandler handler, Map<String, Object> attributes) {

		SockJsSession session = this.sessions.get(sessionId);
		if (session != null) {
			return session;
		}
		if (this.sessionCleanupTask == null) {
			scheduleSessionTask();
		}
		session = sessionFactory.createSession(sessionId, handler, attributes);
		this.sessions.put(sessionId, session);
		return session;
	}

	private void scheduleSessionTask() {
		synchronized (this.sessions) {
			if (this.sessionCleanupTask != null) {
				return;
			}
			this.sessionCleanupTask = getTaskScheduler().scheduleAtFixedRate(() -> {
				List<String> removedIds = new ArrayList<>();
				for (SockJsSession session : this.sessions.values()) {
					try {
						if (session.getTimeSinceLastActive() > getDisconnectDelay()) {
							this.sessions.remove(session.getId());
							removedIds.add(session.getId());
							session.close();
						}
					}
					catch (Throwable ex) {
						// Could be part of normal workflow (e.g. browser tab closed)
						logger.debug("Failed to close " + session, ex);
					}
				}
				if (logger.isDebugEnabled() && !removedIds.isEmpty()) {
					logger.debug("Closed " + removedIds.size() + " sessions: " + removedIds);
				}
			}, getDisconnectDelay());
		}
	}

}
