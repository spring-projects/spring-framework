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

package org.springframework.web.socket.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;

/**
 * Provides utility methods for parsing common WebSocket XML namespace elements.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class WebSocketNamespaceUtils {

	public static RuntimeBeanReference registerHandshakeHandler(Element element, ParserContext parserContext, Object source) {
		RuntimeBeanReference handlerRef;
		Element handlerElem = DomUtils.getChildElementByTagName(element, "handshake-handler");
		if (handlerElem != null) {
			handlerRef = new RuntimeBeanReference(handlerElem.getAttribute("ref"));
		}
		else {
			RootBeanDefinition defaultHandlerDef = new RootBeanDefinition(DefaultHandshakeHandler.class);
			defaultHandlerDef.setSource(source);
			defaultHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String handlerName = parserContext.getReaderContext().registerWithGeneratedName(defaultHandlerDef);
			handlerRef = new RuntimeBeanReference(handlerName);
		}
		return handlerRef;
	}

	public static RuntimeBeanReference registerSockJsService(Element element, String sockJsSchedulerName,
			ParserContext parserContext, Object source) {

		Element sockJsElement = DomUtils.getChildElementByTagName(element, "sockjs");

		if (sockJsElement != null) {
			RootBeanDefinition sockJsServiceDef = new RootBeanDefinition(DefaultSockJsService.class);
			sockJsServiceDef.setSource(source);

			Object scheduler;
			String customTaskSchedulerName = sockJsElement.getAttribute("scheduler");
			if (!customTaskSchedulerName.isEmpty()) {
				scheduler = new RuntimeBeanReference(customTaskSchedulerName);
			}
			else {
				scheduler = registerSockJsTaskScheduler(sockJsSchedulerName, parserContext, source);
			}
			sockJsServiceDef.getConstructorArgumentValues().addIndexedArgumentValue(0, scheduler);

			Element transportHandlersElement = DomUtils.getChildElementByTagName(sockJsElement, "transport-handlers");
			if (transportHandlersElement != null) {
				String registerDefaultsAttribute = transportHandlersElement.getAttribute("register-defaults");
				if (registerDefaultsAttribute.equals("false")) {
					sockJsServiceDef.setBeanClass(TransportHandlingSockJsService.class);
				}
				ManagedList<?> transportHandlersList = parseBeanSubElements(transportHandlersElement, parserContext);
				sockJsServiceDef.getConstructorArgumentValues().addIndexedArgumentValue(1, transportHandlersList);
			}

			String attrValue = sockJsElement.getAttribute("name");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("name", attrValue);
			}
			attrValue = sockJsElement.getAttribute("websocket-enabled");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("webSocketEnabled", Boolean.valueOf(attrValue));
			}
			attrValue = sockJsElement.getAttribute("session-cookie-needed");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("sessionCookieNeeded", Boolean.valueOf(attrValue));
			}
			attrValue = sockJsElement.getAttribute("stream-bytes-limit");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("streamBytesLimit", Integer.valueOf(attrValue));
			}
			attrValue = sockJsElement.getAttribute("disconnect-delay");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("disconnectDelay", Long.valueOf(attrValue));
			}
			attrValue = sockJsElement.getAttribute("message-cache-size");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("httpMessageCacheSize", Integer.valueOf(attrValue));
			}
			attrValue = sockJsElement.getAttribute("heartbeat-time");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("heartbeatTime", Long.valueOf(attrValue));
			}

			sockJsServiceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String sockJsServiceName = parserContext.getReaderContext().registerWithGeneratedName(sockJsServiceDef);
			return new RuntimeBeanReference(sockJsServiceName);
		}

		return null;
	}

	private static RuntimeBeanReference registerSockJsTaskScheduler(String schedulerName,
			ParserContext parserContext, Object source) {

		if (!parserContext.getRegistry().containsBeanDefinition(schedulerName)) {
			RootBeanDefinition taskSchedulerDef = new RootBeanDefinition(ThreadPoolTaskScheduler.class);
			taskSchedulerDef.setSource(source);
			taskSchedulerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			taskSchedulerDef.getPropertyValues().add("threadNamePrefix", schedulerName + "-");
			parserContext.getRegistry().registerBeanDefinition(schedulerName, taskSchedulerDef);
			parserContext.registerComponent(new BeanComponentDefinition(taskSchedulerDef, schedulerName));
		}

		return new RuntimeBeanReference(schedulerName);
	}

	public static ManagedList<? super Object> parseBeanSubElements(Element parentElement, ParserContext parserContext) {

		ManagedList<? super Object> beans = new ManagedList<Object>();
		if (parentElement != null) {
			beans.setSource(parserContext.extractSource(parentElement));
			for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, new String[] { "bean", "ref" })) {
				Object object = parserContext.getDelegate().parsePropertySubElement(beanElement, null);
				beans.add(object);
			}
		}

		return beans;
	}

}
