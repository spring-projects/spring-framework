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

package org.springframework.web.socket.config;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;

/**
 * A {@link BeanDefinitionParser} that provides the configuration for the
 * {@code <websocket:handlers/>} namespace element. It registers a Spring MVC
 * {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping}
 * to map HTTP WebSocket handshake requests to
 * {@link org.springframework.web.socket.WebSocketHandler}s.
 *
 * @author Brian Clozel
 * @since 4.0
 */
class HandlersBeanDefinitionParser implements BeanDefinitionParser {

	private static final String SOCK_JS_SCHEDULER_NAME = "SockJsScheduler";

	private static final int DEFAULT_MAPPING_ORDER = 1;


	@Override
	public BeanDefinition parse(Element element, ParserContext parserCxt) {

		Object source = parserCxt.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserCxt.pushContainingComponent(compDefinition);

		String orderAttribute = element.getAttribute("order");
		int order = orderAttribute.isEmpty() ? DEFAULT_MAPPING_ORDER : Integer.valueOf(orderAttribute);

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("order", order);
		String handlerMappingName = parserCxt.getReaderContext().registerWithGeneratedName(handlerMappingDef);

		RuntimeBeanReference handshakeHandler = WebSocketNamespaceUtils.registerHandshakeHandler(element, parserCxt, source);
		Element interceptorsElement = DomUtils.getChildElementByTagName(element, "handshake-interceptors");
		ManagedList<?> interceptors = WebSocketNamespaceUtils.parseBeanSubElements(interceptorsElement, parserCxt);
		RuntimeBeanReference sockJsServiceRef =
				WebSocketNamespaceUtils.registerSockJsService(element, SOCK_JS_SCHEDULER_NAME, parserCxt, source);

		HandlerMappingStrategy strategy = createHandlerMappingStrategy(sockJsServiceRef, handshakeHandler, interceptors);

		List<Element> mappingElements = DomUtils.getChildElementsByTagName(element, "mapping");
		ManagedMap<String, Object> urlMap = new ManagedMap<String, Object>();
		urlMap.setSource(source);

		for(Element mappingElement : mappingElements) {
			urlMap.putAll(strategy.createMappings(mappingElement, parserCxt));
		}
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);

		parserCxt.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingName));
		parserCxt.popAndRegisterContainingComponent();
		return null;
	}


	private interface HandlerMappingStrategy {

		public ManagedMap<String, Object> createMappings(Element mappingElement, ParserContext parserContext);
	}

	private HandlerMappingStrategy createHandlerMappingStrategy(
			RuntimeBeanReference sockJsServiceRef, RuntimeBeanReference handshakeHandlerRef,
			ManagedList<? extends Object> interceptorsList) {

		if(sockJsServiceRef != null) {
			SockJSHandlerMappingStrategy strategy = new SockJSHandlerMappingStrategy();
			strategy.setSockJsServiceRef(sockJsServiceRef);
			return strategy;
		}
		else {
			WebSocketHandlerMappingStrategy strategy = new WebSocketHandlerMappingStrategy();
			strategy.setHandshakeHandlerReference(handshakeHandlerRef);
			strategy.setInterceptorsList(interceptorsList);
			return strategy;
		}
	}

	private class WebSocketHandlerMappingStrategy implements HandlerMappingStrategy {

		private RuntimeBeanReference handshakeHandlerReference;

		private ManagedList<?> interceptorsList;

		public void setHandshakeHandlerReference(RuntimeBeanReference handshakeHandlerReference) {
			this.handshakeHandlerReference = handshakeHandlerReference;
		}

		public void setInterceptorsList(ManagedList<?> interceptorsList) { this.interceptorsList = interceptorsList; }

		@Override
		public ManagedMap<String, Object> createMappings(Element mappingElement, ParserContext parserContext) {
			ManagedMap<String, Object> urlMap = new ManagedMap<String, Object>();
			Object source = parserContext.extractSource(mappingElement);

			List<String> mappings = Arrays.asList(mappingElement.getAttribute("path").split(","));
			RuntimeBeanReference webSocketHandlerReference = new RuntimeBeanReference(mappingElement.getAttribute("handler"));

			ConstructorArgumentValues cavs = new ConstructorArgumentValues();
			cavs.addIndexedArgumentValue(0, webSocketHandlerReference);
			if(this.handshakeHandlerReference != null) {
				cavs.addIndexedArgumentValue(1, this.handshakeHandlerReference);
			}
			RootBeanDefinition requestHandlerDef = new RootBeanDefinition(WebSocketHttpRequestHandler.class, cavs, null);
			requestHandlerDef.setSource(source);
			requestHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			requestHandlerDef.getPropertyValues().add("handshakeInterceptors", this.interceptorsList);
			String requestHandlerName = parserContext.getReaderContext().registerWithGeneratedName(requestHandlerDef);
			RuntimeBeanReference requestHandlerRef = new RuntimeBeanReference(requestHandlerName);

			for(String mapping : mappings) {
				urlMap.put(mapping, requestHandlerRef);
			}

			return urlMap;
		}
	}

	private class SockJSHandlerMappingStrategy implements HandlerMappingStrategy {

		private RuntimeBeanReference sockJsServiceRef;

		public void setSockJsServiceRef(RuntimeBeanReference sockJsServiceRef) {
			this.sockJsServiceRef = sockJsServiceRef;
		}

		@Override
		public ManagedMap<String, Object> createMappings(Element mappingElement, ParserContext parserContext) {

			ManagedMap<String, Object> urlMap = new ManagedMap<String, Object>();
			Object source = parserContext.extractSource(mappingElement);

			List<String> mappings = Arrays.asList(mappingElement.getAttribute("path").split(","));
			RuntimeBeanReference webSocketHandlerReference = new RuntimeBeanReference(mappingElement.getAttribute("handler"));

			ConstructorArgumentValues cavs = new ConstructorArgumentValues();
			cavs.addIndexedArgumentValue(0, this.sockJsServiceRef, "SockJsService");
			cavs.addIndexedArgumentValue(1, webSocketHandlerReference, "WebSocketHandler");

			RootBeanDefinition requestHandlerDef = new RootBeanDefinition(SockJsHttpRequestHandler.class, cavs, null);
			requestHandlerDef.setSource(source);
			requestHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String requestHandlerName = parserContext.getReaderContext().registerWithGeneratedName(requestHandlerDef);
			RuntimeBeanReference requestHandlerRef = new RuntimeBeanReference(requestHandlerName);

			for(String path : mappings) {
				String pathPattern = path.endsWith("/") ? path + "**" : path + "/**";
				urlMap.put(pathPattern, requestHandlerRef);
			}

			return urlMap;
		}
	}

}
