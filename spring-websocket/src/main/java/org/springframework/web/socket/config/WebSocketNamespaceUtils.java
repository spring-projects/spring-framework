/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.config;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.sockjs.transport.TransportHandlingSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;

/**
 * Provides utility methods for parsing common WebSocket XML namespace elements.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class WebSocketNamespaceUtils {

	public static RuntimeBeanReference registerHandshakeHandler(Element element, ParserContext context, Object source) {
		RuntimeBeanReference handlerRef;
		Element handlerElem = DomUtils.getChildElementByTagName(element, "handshake-handler");
		if (handlerElem != null) {
			handlerRef = new RuntimeBeanReference(handlerElem.getAttribute("ref"));
		}
		else {
			RootBeanDefinition defaultHandlerDef = new RootBeanDefinition(DefaultHandshakeHandler.class);
			defaultHandlerDef.setSource(source);
			defaultHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String handlerName = context.getReaderContext().registerWithGeneratedName(defaultHandlerDef);
			handlerRef = new RuntimeBeanReference(handlerName);
		}
		return handlerRef;
	}

	public static RuntimeBeanReference registerSockJsService(Element element, String schedulerName,
			ParserContext context, Object source) {

		Element sockJsElement = DomUtils.getChildElementByTagName(element, "sockjs");

		if (sockJsElement != null) {
			Element handshakeHandler = DomUtils.getChildElementByTagName(element, "handshake-handler");

			RootBeanDefinition sockJsServiceDef = new RootBeanDefinition(DefaultSockJsService.class);
			sockJsServiceDef.setSource(source);

			Object scheduler;
			String customTaskSchedulerName = sockJsElement.getAttribute("scheduler");
			if (!customTaskSchedulerName.isEmpty()) {
				scheduler = new RuntimeBeanReference(customTaskSchedulerName);
			}
			else {
				scheduler = registerScheduler(schedulerName, context, source);
			}
			sockJsServiceDef.getConstructorArgumentValues().addIndexedArgumentValue(0, scheduler);

			Element transportHandlersElement = DomUtils.getChildElementByTagName(sockJsElement, "transport-handlers");
			if (transportHandlersElement != null) {
				String registerDefaults = transportHandlersElement.getAttribute("register-defaults");
				if (registerDefaults.equals("false")) {
					sockJsServiceDef.setBeanClass(TransportHandlingSockJsService.class);
				}
				ManagedList<?> transportHandlers = parseBeanSubElements(transportHandlersElement, context);
				sockJsServiceDef.getConstructorArgumentValues().addIndexedArgumentValue(1, transportHandlers);
			}
			else if (handshakeHandler != null) {
				RuntimeBeanReference handshakeHandlerRef = new RuntimeBeanReference(handshakeHandler.getAttribute("ref"));
				RootBeanDefinition transportHandler = new RootBeanDefinition(WebSocketTransportHandler.class);
				transportHandler.setSource(source);
				transportHandler.getConstructorArgumentValues().addIndexedArgumentValue(0, handshakeHandlerRef);
				sockJsServiceDef.getConstructorArgumentValues().addIndexedArgumentValue(1, transportHandler);
			}

			Element interceptorsElement = DomUtils.getChildElementByTagName(element, "handshake-interceptors");
			ManagedList<? super Object> interceptors = WebSocketNamespaceUtils.parseBeanSubElements(interceptorsElement, context);
			String allowedOriginsAttribute = element.getAttribute("allowed-origins");
			List<String> allowedOrigins = Arrays.asList(StringUtils.tokenizeToStringArray(allowedOriginsAttribute, ","));
			sockJsServiceDef.getPropertyValues().add("allowedOrigins", allowedOrigins);
			RootBeanDefinition originHandshakeInterceptor = new RootBeanDefinition(OriginHandshakeInterceptor.class);
			originHandshakeInterceptor.getPropertyValues().add("allowedOrigins", allowedOrigins);
			interceptors.add(originHandshakeInterceptor);
			sockJsServiceDef.getPropertyValues().add("handshakeInterceptors", interceptors);

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
			attrValue = sockJsElement.getAttribute("client-library-url");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("sockJsClientLibraryUrl", attrValue);
			}
			attrValue = sockJsElement.getAttribute("message-codec");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("messageCodec", new RuntimeBeanReference(attrValue));
			}
			attrValue = sockJsElement.getAttribute("suppress-cors");
			if (!attrValue.isEmpty()) {
				sockJsServiceDef.getPropertyValues().add("suppressCors", Boolean.valueOf(attrValue));
			}
			sockJsServiceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String sockJsServiceName = context.getReaderContext().registerWithGeneratedName(sockJsServiceDef);
			return new RuntimeBeanReference(sockJsServiceName);
		}
		return null;
	}

	public static RuntimeBeanReference registerScheduler(String schedulerName, ParserContext context, Object source) {
		if (!context.getRegistry().containsBeanDefinition(schedulerName)) {
			RootBeanDefinition taskSchedulerDef = new RootBeanDefinition(ThreadPoolTaskScheduler.class);
			taskSchedulerDef.setSource(source);
			taskSchedulerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			taskSchedulerDef.getPropertyValues().add("poolSize", Runtime.getRuntime().availableProcessors());
			taskSchedulerDef.getPropertyValues().add("threadNamePrefix", schedulerName + "-");
			taskSchedulerDef.getPropertyValues().add("removeOnCancelPolicy", true);
			context.getRegistry().registerBeanDefinition(schedulerName, taskSchedulerDef);
			context.registerComponent(new BeanComponentDefinition(taskSchedulerDef, schedulerName));
		}
		return new RuntimeBeanReference(schedulerName);
	}

	public static ManagedList<? super Object> parseBeanSubElements(Element parentElement, ParserContext context) {
		ManagedList<? super Object> beans = new ManagedList<Object>();
		if (parentElement != null) {
			beans.setSource(context.extractSource(parentElement));
			for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean", "ref")) {
				beans.add(context.getDelegate().parsePropertySubElement(beanElement, null));
			}
		}
		return beans;
	}

}
