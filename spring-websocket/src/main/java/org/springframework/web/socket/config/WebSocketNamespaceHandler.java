/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.beans.factory.xml.NamespaceHandler} for Spring WebSocket
 * configuration namespace.
 *
 * @author Brian Clozel
 * @since 4.0
 */
public class WebSocketNamespaceHandler extends NamespaceHandlerSupport {

	private static final boolean SPRING_MESSAGING_PRESENT = ClassUtils.isPresent(
			"org.springframework.messaging.Message", WebSocketNamespaceHandler.class.getClassLoader());


	@Override
	public void init() {
		registerBeanDefinitionParser("handlers", new HandlersBeanDefinitionParser());
		if (SPRING_MESSAGING_PRESENT) {
			registerBeanDefinitionParser("message-broker", new MessageBrokerBeanDefinitionParser());
		}
	}

}
