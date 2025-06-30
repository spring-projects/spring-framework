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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * A registry for STOMP over WebSocket endpoints that maps the endpoints with a
 * {@link org.springframework.web.servlet.HandlerMapping} for use in Spring MVC.
 *
 * @author Rossen Stoyanchev
 * @author Artem Bilan
 * @since 4.0
 */
public class WebMvcStompEndpointRegistry implements StompEndpointRegistry {

	private final WebSocketHandler webSocketHandler;

	private final TaskScheduler sockJsScheduler;

	private int order = 1;

	private @Nullable UrlPathHelper urlPathHelper;

	private final SubProtocolWebSocketHandler subProtocolWebSocketHandler;

	private final StompSubProtocolHandler stompHandler;

	private final List<WebMvcStompWebSocketEndpointRegistration> registrations = new ArrayList<>();


	public WebMvcStompEndpointRegistry(WebSocketHandler webSocketHandler,
			WebSocketTransportRegistration transportRegistration, TaskScheduler defaultSockJsTaskScheduler) {

		Assert.notNull(webSocketHandler, "WebSocketHandler is required ");
		Assert.notNull(transportRegistration, "WebSocketTransportRegistration is required");

		this.webSocketHandler = webSocketHandler;
		this.subProtocolWebSocketHandler = unwrapSubProtocolWebSocketHandler(webSocketHandler);

		if (transportRegistration.getSendTimeLimit() != null) {
			this.subProtocolWebSocketHandler.setSendTimeLimit(transportRegistration.getSendTimeLimit());
		}
		if (transportRegistration.getSendBufferSizeLimit() != null) {
			this.subProtocolWebSocketHandler.setSendBufferSizeLimit(transportRegistration.getSendBufferSizeLimit());
		}
		if (transportRegistration.getTimeToFirstMessage() != null) {
			this.subProtocolWebSocketHandler.setTimeToFirstMessage(transportRegistration.getTimeToFirstMessage());
		}

		this.stompHandler = new StompSubProtocolHandler();
		if (transportRegistration.getMessageSizeLimit() != null) {
			this.stompHandler.setMessageSizeLimit(transportRegistration.getMessageSizeLimit());
		}

		this.sockJsScheduler = defaultSockJsTaskScheduler;
	}

	private static SubProtocolWebSocketHandler unwrapSubProtocolWebSocketHandler(WebSocketHandler handler) {
		WebSocketHandler actual = WebSocketHandlerDecorator.unwrap(handler);
		if (!(actual instanceof SubProtocolWebSocketHandler subProtocolWebSocketHandler)) {
			throw new IllegalArgumentException("No SubProtocolWebSocketHandler in " + handler);
		}
		return subProtocolWebSocketHandler;
	}


	@Override
	public StompWebSocketEndpointRegistration addEndpoint(String... paths) {
		this.subProtocolWebSocketHandler.addProtocolHandler(this.stompHandler);
		WebMvcStompWebSocketEndpointRegistration registration =
				new WebMvcStompWebSocketEndpointRegistration(paths, this.webSocketHandler, this.sockJsScheduler);
		this.registrations.add(registration);
		return registration;
	}

	/**
	 * Set the order for the resulting
	 * {@link org.springframework.web.servlet.HandlerMapping}
	 * relative to other handler mappings configured in Spring MVC.
	 * <p>The default value is 1.
	 */
	@Override
	public void setOrder(int order) {
		this.order = order;
	}

	protected int getOrder() {
		return this.order;
	}

	/**
	 * Set the UrlPathHelper to configure on the {@code HandlerMapping}
	 * used to map handshake requests.
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	@Override
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	@Deprecated(since = "7.0", forRemoval = true)
	protected @Nullable UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}

	@Override
	public WebMvcStompEndpointRegistry setErrorHandler(StompSubProtocolErrorHandler errorHandler) {
		this.stompHandler.setErrorHandler(errorHandler);
		return this;
	}

	@Override
	public WebMvcStompEndpointRegistry setPreserveReceiveOrder(boolean preserveReceiveOrder) {
		this.stompHandler.setPreserveReceiveOrder(preserveReceiveOrder);
		return this;
	}

	protected boolean isPreserveReceiveOrder() {
		return this.stompHandler.isPreserveReceiveOrder();
	}

	protected void setApplicationContext(ApplicationContext applicationContext) {
		this.stompHandler.setApplicationEventPublisher(applicationContext);
	}

	/**
	 * Return a handler mapping with the mapped ViewControllers.
	 */
	@SuppressWarnings("removal")
	public AbstractHandlerMapping getHandlerMapping() {
		Map<String, Object> urlMap = new LinkedHashMap<>();
		for (WebMvcStompWebSocketEndpointRegistration registration : this.registrations) {
			MultiValueMap<HttpRequestHandler, String> mappings = registration.getMappings();
			mappings.forEach((httpHandler, patterns) -> {
				for (String pattern : patterns) {
					urlMap.put(pattern, httpHandler);
				}
			});
		}
		WebSocketHandlerMapping hm = new WebSocketHandlerMapping();
		hm.setUrlMap(urlMap);
		hm.setOrder(this.order);
		if (this.urlPathHelper != null) {
			hm.setUrlPathHelper(this.urlPathHelper);
		}
		return hm;
	}

}
