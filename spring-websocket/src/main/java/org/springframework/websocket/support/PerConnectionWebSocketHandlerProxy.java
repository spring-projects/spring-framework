/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.websocket.support;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;
import org.springframework.websocket.CloseStatus;
import org.springframework.websocket.WebSocketHandler;
import org.springframework.websocket.WebSocketMessage;
import org.springframework.websocket.WebSocketSession;

/**
 * A {@link WebSocketHandler} that initializes and destroys a {@link WebSocketHandler}
 * instance for each WebSocket connection and delegates all other methods to it.
 *
 * <p>
 * Essentially create an instance of this class once, providing the type of
 * {@link WebSocketHandler} class to create for each connection, and then pass it to any
 * API method that expects a {@link WebSocketHandler}.
 *
 * <p>
 * If initializing the target {@link WebSocketHandler} type requires a Spring BeanFctory,
 * then the {@link #setBeanFactory(BeanFactory)} property accordingly. Simply declaring
 * this class as a Spring bean will do that. Otherwise, {@link WebSocketHandler} instances
 * of the target type will be created using the default constructor.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PerConnectionWebSocketHandlerProxy implements WebSocketHandler<WebSocketMessage<?>>, BeanFactoryAware {

	private Log logger = LogFactory.getLog(PerConnectionWebSocketHandlerProxy.class);

	private final BeanCreatingHandlerProvider<WebSocketHandler<?>> provider;

	private Map<WebSocketSession, WebSocketHandler<?>> handlers =
			new ConcurrentHashMap<WebSocketSession, WebSocketHandler<?>>();

	private final Class<?> supportedMessageType;


	public PerConnectionWebSocketHandlerProxy(Class<? extends WebSocketHandler<?>> handlerType) {
		this.provider = new BeanCreatingHandlerProvider<WebSocketHandler<?>>(handlerType);
		this.supportedMessageType = GenericTypeResolver.resolveTypeArgument(handlerType, WebSocketHandler.class);
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.provider.setBeanFactory(beanFactory);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		WebSocketHandler<?> handler = this.provider.getHandler();
		this.handlers.put(session, handler);
		handler.afterConnectionEstablished(session);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
		if (!this.supportedMessageType.isAssignableFrom(message.getClass())) {
			try {
				session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Message type not supported"));
			}
			catch (IOException e) {
				destroy(session);
			}
		}
		else {
			((WebSocketHandler) getHandler(session)).handleMessage(session, message);
		}
	}

	private WebSocketHandler<?> getHandler(WebSocketSession session) {
		WebSocketHandler<?> handler = this.handlers.get(session);
		Assert.isTrue(handler != null, "WebSocketHandler not found for " + session);
		return handler;
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		getHandler(session).handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
		try {
			getHandler(session).afterConnectionClosed(session, closeStatus);
		}
		finally {
			destroy(session);
		}
	}

	private void destroy(WebSocketSession session) {
		WebSocketHandler<?> handler = this.handlers.remove(session);
		try {
			if (handler != null) {
				this.provider.destroy(handler);
			}
		}
		catch (Throwable t) {
			logger.warn("Error while destroying handler", t);
		}
	}

}
