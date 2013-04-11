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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.http.Cookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.SockJsSessionSupport;
import org.springframework.sockjs.server.AbstractSockJsService;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportHandlerRegistrar;
import org.springframework.sockjs.server.TransportHandlerRegistry;
import org.springframework.sockjs.server.TransportType;
import org.springframework.util.Assert;
import org.springframework.websocket.server.HandshakeHandler;


/**
 * TODO
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultSockJsService extends AbstractSockJsService
		implements TransportHandlerRegistry, BeanFactoryAware, InitializingBean {

	private final Class<? extends SockJsHandler> sockJsHandlerClass;

	private final SockJsHandler sockJsHandler;

	private TaskScheduler sessionTimeoutScheduler;

	private final Map<String, SockJsSessionSupport> sessions = new ConcurrentHashMap<String, SockJsSessionSupport>();

	private final Map<TransportType, TransportHandler> transportHandlers = new HashMap<TransportType, TransportHandler>();

	private AutowireCapableBeanFactory beanFactory;


	public DefaultSockJsService(String prefix, Class<? extends SockJsHandler> sockJsHandlerClass) {
		this(prefix, sockJsHandlerClass, null);
	}

	public DefaultSockJsService(String prefix, SockJsHandler sockJsHandler) {
		this(prefix, null, sockJsHandler);
	}

	private DefaultSockJsService(String prefix, Class<? extends SockJsHandler> handlerClass, SockJsHandler handler) {
		super(prefix);
		Assert.isTrue(((handlerClass != null) || (handler != null)), "A sockJsHandler class or instance is required");
		this.sockJsHandlerClass = handlerClass;
		this.sockJsHandler = handler;
		this.sessionTimeoutScheduler = createScheduler("SockJs-sessionTimeout-");
		new DefaultTransportHandlerRegistrar().registerTransportHandlers(this, this);
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
		registrar.registerTransportHandlers(this, this);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	@Override
	public SockJsHandler getSockJsHandler() {
		return (this.sockJsHandlerClass != null) ?
				this.beanFactory.createBean(this.sockJsHandlerClass) : this.sockJsHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (this.sockJsHandler != null) {
			Assert.notNull(this.beanFactory,
					"An AutowirecapableBeanFactory is required to initialize SockJS handler instances per request.");
		}

		if (this.transportHandlers.get(TransportType.WEBSOCKET) == null) {
			logger.warn("No WebSocket transport handler was registered");
		}

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
	protected void handleRawWebSocket(ServerHttpRequest request, ServerHttpResponse response) throws Exception {
		TransportHandler transportHandler = this.transportHandlers.get(TransportType.WEBSOCKET);
		if ((transportHandler != null) && transportHandler instanceof HandshakeHandler) {
			HandshakeHandler handshakeHandler = (HandshakeHandler) transportHandler;
			handshakeHandler.doHandshake(request, response);
		}
		else {
			logger.debug("No handler found for raw WebSocket messages");
			response.setStatusCode(HttpStatus.NOT_FOUND);
		}
	}

	@Override
	protected void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
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
		if ((session == null) && !transportHandler.handleNoSession(request, response)) {
			return;
		}

		addNoCacheHeaders(response);

		if (isJsessionIdCookieNeeded()) {
			Cookie cookie = request.getCookies().getCookie("JSESSIONID");
			String jsid = (cookie != null) ? cookie.getValue() : "dummy";
			// TODO: bypass use of Cookie object (causes Jetty to set Expires header)
			response.getHeaders().set("Set-Cookie", "JSESSIONID=" + jsid + ";path=/");	// TODO
		}

		if (transportType.isCorsSupported()) {
			addCorsHeaders(request, response);
		}

		transportHandler.handleRequest(request, response, session);
	}

	public SockJsSessionSupport getSockJsSession(String sessionId, TransportHandler transportHandler) {

		SockJsSessionSupport session = this.sessions.get(sessionId);
		if (session != null) {
			return session;
		}

		if (!transportHandler.canCreateSession()) {
			return null;
		}

		synchronized (this.sessions) {
			session = this.sessions.get(sessionId);
			if (session != null) {
				return session;
			}

			logger.debug("Creating new session with session id \"" + sessionId + "\"");
			session = transportHandler.createSession(sessionId);
			this.sessions.put(sessionId, session);

			return session;
		}
	}

}
