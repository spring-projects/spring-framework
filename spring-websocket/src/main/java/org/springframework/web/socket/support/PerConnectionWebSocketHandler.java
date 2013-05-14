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

package org.springframework.web.socket.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

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
public class PerConnectionWebSocketHandler implements WebSocketHandler, BeanFactoryAware {

	private static final Log logger = LogFactory.getLog(PerConnectionWebSocketHandler.class);

	private final BeanCreatingHandlerProvider<WebSocketHandler> provider;

	private final Map<WebSocketSession, WebSocketHandler> handlers =
			new ConcurrentHashMap<WebSocketSession, WebSocketHandler>();


	public PerConnectionWebSocketHandler(Class<? extends WebSocketHandler> handlerType) {
		this.provider = new BeanCreatingHandlerProvider<WebSocketHandler>(handlerType);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.provider.setBeanFactory(beanFactory);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		WebSocketHandler handler = this.provider.getHandler();
		this.handlers.put(session, handler);
		handler.afterConnectionEstablished(session);
	}

	@Override
	public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
		getHandler(session).handleMessage(session, message);
	}

	private WebSocketHandler getHandler(WebSocketSession session) {
		WebSocketHandler handler = this.handlers.get(session);
		Assert.isTrue(handler != null, "WebSocketHandler not found for " + session);
		return handler;
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		getHandler(session).handleTransportError(session, exception);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		try {
			getHandler(session).afterConnectionClosed(session, closeStatus);
		}
		finally {
			destroy(session);
		}
	}

	private void destroy(WebSocketSession session) {
		WebSocketHandler handler = this.handlers.remove(session);
		try {
			if (handler != null) {
				this.provider.destroy(handler);
			}
		}
		catch (Throwable t) {
			logger.warn("Error while destroying handler", t);
		}
	}

	@Override
	public String toString() {
		return "PerConnectionWebSocketHandlerProxy [handlerType=" + this.provider.getHandlerType() + "]";
	}

}
