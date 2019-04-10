/*
 * Copyright 2002-2018 the original author or authors.
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
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;

/**
 * Parses the configuration for the {@code <websocket:handlers/>} namespace element.
 * Registers a Spring MVC {@code SimpleUrlHandlerMapping} to map HTTP WebSocket
 * handshake (or SockJS) requests to
 * {@link org.springframework.web.socket.WebSocketHandler WebSocketHandlers}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class HandlersBeanDefinitionParser implements BeanDefinitionParser {

	private static final String SOCK_JS_SCHEDULER_NAME = "SockJsScheduler";

	private static final int DEFAULT_MAPPING_ORDER = 1;


	@Override
	@Nullable
	public BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		context.pushContainingComponent(compDefinition);

		String orderAttribute = element.getAttribute("order");
		int order = orderAttribute.isEmpty() ? DEFAULT_MAPPING_ORDER : Integer.valueOf(orderAttribute);

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(WebSocketHandlerMapping.class);
		handlerMappingDef.setSource(source);
		handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		handlerMappingDef.getPropertyValues().add("order", order);
		String handlerMappingName = context.getReaderContext().registerWithGeneratedName(handlerMappingDef);

		RuntimeBeanReference sockJsService = WebSocketNamespaceUtils.registerSockJsService(
				element, SOCK_JS_SCHEDULER_NAME, context, source);

		HandlerMappingStrategy strategy;
		if (sockJsService != null) {
			strategy = new SockJsHandlerMappingStrategy(sockJsService);
		}
		else {
			RuntimeBeanReference handler = WebSocketNamespaceUtils.registerHandshakeHandler(element, context, source);
			Element interceptElem = DomUtils.getChildElementByTagName(element, "handshake-interceptors");
			ManagedList<Object> interceptors = WebSocketNamespaceUtils.parseBeanSubElements(interceptElem, context);
			String allowedOrigins = element.getAttribute("allowed-origins");
			List<String> origins = Arrays.asList(StringUtils.tokenizeToStringArray(allowedOrigins, ","));
			interceptors.add(new OriginHandshakeInterceptor(origins));
			strategy = new WebSocketHandlerMappingStrategy(handler, interceptors);
		}

		ManagedMap<String, Object> urlMap = new ManagedMap<>();
		urlMap.setSource(source);
		for (Element mappingElement : DomUtils.getChildElementsByTagName(element, "mapping")) {
			strategy.addMapping(mappingElement, urlMap, context);
		}
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);

		context.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingName));
		context.popAndRegisterContainingComponent();
		return null;
	}


	private interface HandlerMappingStrategy {

		void addMapping(Element mappingElement, ManagedMap<String, Object> map, ParserContext context);
	}


	private static class WebSocketHandlerMappingStrategy implements HandlerMappingStrategy {

		private final RuntimeBeanReference handshakeHandlerReference;

		private final ManagedList<?> interceptorsList;

		public WebSocketHandlerMappingStrategy(RuntimeBeanReference handshakeHandler, ManagedList<?> interceptors) {
			this.handshakeHandlerReference = handshakeHandler;
			this.interceptorsList = interceptors;
		}

		@Override
		public void addMapping(Element element, ManagedMap<String, Object> urlMap, ParserContext context) {
			String pathAttribute = element.getAttribute("path");
			String[] mappings = StringUtils.tokenizeToStringArray(pathAttribute, ",");
			RuntimeBeanReference handlerReference = new RuntimeBeanReference(element.getAttribute("handler"));

			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, handlerReference);
			cargs.addIndexedArgumentValue(1, this.handshakeHandlerReference);
			RootBeanDefinition requestHandlerDef = new RootBeanDefinition(WebSocketHttpRequestHandler.class, cargs, null);
			requestHandlerDef.setSource(context.extractSource(element));
			requestHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			requestHandlerDef.getPropertyValues().add("handshakeInterceptors", this.interceptorsList);
			String requestHandlerName = context.getReaderContext().registerWithGeneratedName(requestHandlerDef);
			RuntimeBeanReference requestHandlerRef = new RuntimeBeanReference(requestHandlerName);

			for (String mapping : mappings) {
				urlMap.put(mapping, requestHandlerRef);
			}
		}
	}


	private static class SockJsHandlerMappingStrategy implements HandlerMappingStrategy {

		private final RuntimeBeanReference sockJsService;

		public SockJsHandlerMappingStrategy(RuntimeBeanReference sockJsService) {
			this.sockJsService = sockJsService;
		}

		@Override
		public void addMapping(Element element, ManagedMap<String, Object> urlMap, ParserContext context) {
			String pathAttribute = element.getAttribute("path");
			String[] mappings = StringUtils.tokenizeToStringArray(pathAttribute, ",");
			RuntimeBeanReference handlerReference = new RuntimeBeanReference(element.getAttribute("handler"));

			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, this.sockJsService, "SockJsService");
			cargs.addIndexedArgumentValue(1, handlerReference, "WebSocketHandler");

			RootBeanDefinition requestHandlerDef = new RootBeanDefinition(SockJsHttpRequestHandler.class, cargs, null);
			requestHandlerDef.setSource(context.extractSource(element));
			requestHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String requestHandlerName = context.getReaderContext().registerWithGeneratedName(requestHandlerDef);
			RuntimeBeanReference requestHandlerRef = new RuntimeBeanReference(requestHandlerName);

			for (String mapping : mappings) {
				String pathPattern = (mapping.endsWith("/") ? mapping + "**" : mapping + "/**");
				urlMap.put(pathPattern, requestHandlerRef);
			}
		}
	}

}
