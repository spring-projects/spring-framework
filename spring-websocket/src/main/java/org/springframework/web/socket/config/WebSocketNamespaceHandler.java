/*
 * Copyright 2002-2022 the original author or authors.
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

	private static final boolean isSpringMessagingPresent = ClassUtils.isPresent(
			"org.springframework.messaging.Message", WebSocketNamespaceHandler.class.getClassLoader());


	@Override
	public void init() {
		registerBeanDefinitionParser("handlers", new HandlersBeanDefinitionParser());
		if (isSpringMessagingPresent) {
			registerBeanDefinitionParser("message-broker", new MessageBrokerBeanDefinitionParser());
		}
	}

}
