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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpSessionScope;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.DefaultUserSessionRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;

/**
 * A {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that provides
 * the configuration for the {@code <websocket:message-broker/>} XML namespace element.
 *
 * <p>Registers a Spring MVC {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping}
 * with order 1 to map HTTP WebSocket handshake requests from STOMP/WebSocket clients.
 *
 * <p>Registers the following {@link org.springframework.messaging.MessageChannel}s:
 * <ul>
 * <li>"clientInboundChannel" for receiving messages from clients (e.g. WebSocket clients)
 * <li>"clientOutboundChannel" for sending messages to clients (e.g. WebSocket clients)
 * <li>"brokerChannel" for sending messages from within the application to the message broker
 * </ul>
 *
 * <p>Registers one of the following based on the selected message broker options:
 * <ul>
 * <li>a {@link SimpleBrokerMessageHandler} if the <simple-broker/> is used
 * <li>a {@link StompBrokerRelayMessageHandler} if the <stomp-broker-relay/> is used
 * </ul>
 *
 * <p>Registers a {@link UserDestinationMessageHandler} for handling user destinations.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class MessageBrokerBeanDefinitionParser implements BeanDefinitionParser {

	public static final String WEB_SOCKET_HANDLER_BEAN_NAME = "subProtocolWebSocketHandler";

	public static final String SOCKJS_SCHEDULER_BEAN_NAME = "messageBrokerSockJsScheduler";

	private static final int DEFAULT_MAPPING_ORDER = 1;

	private static final boolean jackson2Present = ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", MessageBrokerBeanDefinitionParser.class.getClassLoader());


	@Override
	public BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		context.pushContainingComponent(compDefinition);

		String orderAttribute = element.getAttribute("order");
		int order = orderAttribute.isEmpty() ? DEFAULT_MAPPING_ORDER : Integer.valueOf(orderAttribute);
		ManagedMap<String, Object> urlMap = new ManagedMap<String, Object>();
		urlMap.setSource(source);
		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.getPropertyValues().add("order", order);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
		registerBeanDef(handlerMappingDef, context, source);

		Element channelElem = DomUtils.getChildElementByTagName(element, "client-inbound-channel");
		RuntimeBeanReference inChannel = getMessageChannel("clientInboundChannel", channelElem, context, source);

		channelElem = DomUtils.getChildElementByTagName(element, "client-outbound-channel");
		RuntimeBeanReference outChannel = getMessageChannel("clientOutboundChannel", channelElem, context, source);

		RootBeanDefinition registryBeanDef = new RootBeanDefinition(DefaultUserSessionRegistry.class);
		String registryBeanName = registerBeanDef(registryBeanDef, context, source);
		RuntimeBeanReference sessionRegistry = new RuntimeBeanReference(registryBeanName);

		RuntimeBeanReference subProtoHandler = registerSubProtoHandler(element, inChannel, outChannel,
				sessionRegistry, context, source);

		for (Element endpointElem : DomUtils.getChildElementsByTagName(element, "stomp-endpoint")) {
			RuntimeBeanReference requestHandler = registerRequestHandler(endpointElem, subProtoHandler, context, source);
			String pathAttribute = endpointElem.getAttribute("path");
			Assert.state(StringUtils.hasText(pathAttribute), "Invalid <stomp-endpoint> (no path mapping)");
			List<String> paths = Arrays.asList(StringUtils.tokenizeToStringArray(pathAttribute, ","));
			for (String path : paths) {
				path = path.trim();
				Assert.state(StringUtils.hasText(path), "Invalid <stomp-endpoint> path attribute: " + pathAttribute);
				if (DomUtils.getChildElementByTagName(endpointElem, "sockjs") != null) {
					path = path.endsWith("/") ? path + "**" : path + "/**";
				}
				urlMap.put(path, requestHandler);
			}
		}

		channelElem = DomUtils.getChildElementByTagName(element, "broker-channel");
		RuntimeBeanReference brokerChannel = getMessageChannel("brokerChannel", channelElem, context, source);
		RootBeanDefinition broker = registerMessageBroker(element, inChannel, outChannel, brokerChannel, context, source);

		RuntimeBeanReference converter = registerMessageConverter(element, context, source);
		RuntimeBeanReference template = registerMessagingTemplate(element, brokerChannel, converter, context, source);
		registerAnnotationMethodMessageHandler(element, inChannel, outChannel,converter, template, context, source);

		RuntimeBeanReference resolver = registerUserDestinationResolver(element, sessionRegistry, context, source);
		registerUserDestinationMessageHandler(inChannel, brokerChannel, resolver, context, source);

		Map<String, Object> scopeMap = Collections.<String, Object>singletonMap("websocket", new SimpSessionScope());
		RootBeanDefinition scopeConfigurer = new RootBeanDefinition(CustomScopeConfigurer.class);
		scopeConfigurer.getPropertyValues().add("scopes", scopeMap);
		registerBeanDefByName("webSocketScopeConfigurer", scopeConfigurer, context, source);

		registerWebSocketMessageBrokerStats(broker, inChannel, outChannel, context, source);

		context.popAndRegisterContainingComponent();
		return null;
	}

	private RuntimeBeanReference getMessageChannel(String name, Element element, ParserContext context, Object source) {
		RootBeanDefinition executor = null;
		if (element == null) {
			executor = getDefaultExecutorBeanDefinition(name);
		}
		else {
			Element executorElem = DomUtils.getChildElementByTagName(element, "executor");
			if (executorElem == null) {
				executor = getDefaultExecutorBeanDefinition(name);
			}
			else {
				executor = new RootBeanDefinition(ThreadPoolTaskExecutor.class);
				if (executorElem.hasAttribute("core-pool-size")) {
					executor.getPropertyValues().add("corePoolSize", executorElem.getAttribute("core-pool-size"));
				}
				if (executorElem.hasAttribute("max-pool-size")) {
					executor.getPropertyValues().add("maxPoolSize", executorElem.getAttribute("max-pool-size"));
				}
				if (executorElem.hasAttribute("keep-alive-seconds")) {
					executor.getPropertyValues().add("keepAliveSeconds", executorElem.getAttribute("keep-alive-seconds"));
				}
				if (executorElem.hasAttribute("queue-capacity")) {
					executor.getPropertyValues().add("queueCapacity", executorElem.getAttribute("queue-capacity"));
				}
			}
		}
		ConstructorArgumentValues argValues = new ConstructorArgumentValues();
		if (executor != null) {
			executor.getPropertyValues().add("threadNamePrefix", name + "-");
			String executorName = name + "Executor";
			registerBeanDefByName(executorName, executor, context, source);
			argValues.addIndexedArgumentValue(0, new RuntimeBeanReference(executorName));
		}
		RootBeanDefinition channelDef = new RootBeanDefinition(ExecutorSubscribableChannel.class, argValues, null);
		ManagedList<? super Object> interceptors = new ManagedList<Object>();
		if (element != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(element, "interceptors");
			interceptors.addAll(WebSocketNamespaceUtils.parseBeanSubElements(interceptorsElement, context));
		}
		interceptors.add(new ImmutableMessageChannelInterceptor());
		channelDef.getPropertyValues().add("interceptors", interceptors);

		registerBeanDefByName(name, channelDef, context, source);
		return new RuntimeBeanReference(name);
	}

	private RootBeanDefinition getDefaultExecutorBeanDefinition(String channelName) {
		if (channelName.equals("brokerChannel")) {
			return null;
		}
		RootBeanDefinition executorDef = new RootBeanDefinition(ThreadPoolTaskExecutor.class);
		executorDef.getPropertyValues().add("corePoolSize", Runtime.getRuntime().availableProcessors() * 2);
		executorDef.getPropertyValues().add("maxPoolSize", Integer.MAX_VALUE);
		executorDef.getPropertyValues().add("queueCapacity", Integer.MAX_VALUE);
		executorDef.getPropertyValues().add("allowCoreThreadTimeOut", true);
		return executorDef;
	}

	private RuntimeBeanReference registerSubProtoHandler(Element element, RuntimeBeanReference inChannel,
			RuntimeBeanReference outChannel, RuntimeBeanReference registry, ParserContext context, Object source) {

		RootBeanDefinition stompHandlerDef = new RootBeanDefinition(StompSubProtocolHandler.class);
		stompHandlerDef.getPropertyValues().add("userSessionRegistry", registry);
		registerBeanDef(stompHandlerDef, context, source);

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, inChannel);
		cavs.addIndexedArgumentValue(1, outChannel);

		RootBeanDefinition handlerDef = new RootBeanDefinition(SubProtocolWebSocketHandler.class, cavs, null);
		handlerDef.getPropertyValues().addPropertyValue("protocolHandlers", stompHandlerDef);
		registerBeanDefByName(WEB_SOCKET_HANDLER_BEAN_NAME, handlerDef, context, source);
		RuntimeBeanReference result = new RuntimeBeanReference(WEB_SOCKET_HANDLER_BEAN_NAME);

		Element transportElem = DomUtils.getChildElementByTagName(element, "transport");
		if (transportElem != null) {
			if (transportElem.hasAttribute("message-size")) {
				stompHandlerDef.getPropertyValues().add("messageSizeLimit", transportElem.getAttribute("message-size"));
			}
			if (transportElem.hasAttribute("send-timeout")) {
				handlerDef.getPropertyValues().add("sendTimeLimit", transportElem.getAttribute("send-timeout"));
			}
			if (transportElem.hasAttribute("send-buffer-size")) {
				handlerDef.getPropertyValues().add("sendBufferSizeLimit", transportElem.getAttribute("send-buffer-size"));
			}
			Element factoriesElement = DomUtils.getChildElementByTagName(transportElem, "decorator-factories");
			if (factoriesElement != null) {
				ManagedList<Object> factories = extractBeanSubElements(factoriesElement, context);
				RootBeanDefinition factoryBean = new RootBeanDefinition(DecoratingFactoryBean.class);
				factoryBean.getConstructorArgumentValues().addIndexedArgumentValue(0, handlerDef);
				factoryBean.getConstructorArgumentValues().addIndexedArgumentValue(1, factories);
				result = new RuntimeBeanReference(registerBeanDef(factoryBean, context, source));
			}
		}
		return result;
	}

	private RuntimeBeanReference registerRequestHandler(Element element, RuntimeBeanReference subProtoHandler,
			ParserContext context, Object source) {

		RootBeanDefinition beanDef;

		RuntimeBeanReference sockJsService = WebSocketNamespaceUtils.registerSockJsService(
				element, SOCKJS_SCHEDULER_BEAN_NAME, context, source);

		if (sockJsService != null) {
			ConstructorArgumentValues cavs = new ConstructorArgumentValues();
			cavs.addIndexedArgumentValue(0, sockJsService);
			cavs.addIndexedArgumentValue(1, subProtoHandler);
			beanDef = new RootBeanDefinition(SockJsHttpRequestHandler.class, cavs, null);
		}
		else {
			RuntimeBeanReference handshakeHandler = WebSocketNamespaceUtils.registerHandshakeHandler(element, context, source);
			Element interceptorsElement = DomUtils.getChildElementByTagName(element, "handshake-interceptors");
			ManagedList<? super Object> interceptors = WebSocketNamespaceUtils.parseBeanSubElements(interceptorsElement, context);
			String allowedOriginsAttribute = element.getAttribute("allowed-origins");
			List<String> allowedOrigins = Arrays.asList(StringUtils.tokenizeToStringArray(allowedOriginsAttribute, ","));
			interceptors.add(new OriginHandshakeInterceptor(allowedOrigins));
			ConstructorArgumentValues cavs = new ConstructorArgumentValues();
			cavs.addIndexedArgumentValue(0, subProtoHandler);
			if (handshakeHandler != null) {
				cavs.addIndexedArgumentValue(1, handshakeHandler);
			}
			beanDef = new RootBeanDefinition(WebSocketHttpRequestHandler.class, cavs, null);
			beanDef.getPropertyValues().add("handshakeInterceptors", interceptors);
		}
		return new RuntimeBeanReference(registerBeanDef(beanDef, context, source));
	}

	private RootBeanDefinition registerMessageBroker(Element messageBrokerElement, RuntimeBeanReference inChannel,
			RuntimeBeanReference outChannel, RuntimeBeanReference brokerChannel, ParserContext context, Object source) {

		Element simpleBrokerElem = DomUtils.getChildElementByTagName(messageBrokerElement, "simple-broker");
		Element brokerRelayElem = DomUtils.getChildElementByTagName(messageBrokerElement, "stomp-broker-relay");

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, inChannel);
		cavs.addIndexedArgumentValue(1, outChannel);
		cavs.addIndexedArgumentValue(2, brokerChannel);

		RootBeanDefinition brokerDef;
		if (simpleBrokerElem != null) {
			String prefix = simpleBrokerElem.getAttribute("prefix");
			cavs.addIndexedArgumentValue(3, Arrays.asList(StringUtils.tokenizeToStringArray(prefix, ",")));
			brokerDef = new RootBeanDefinition(SimpleBrokerMessageHandler.class, cavs, null);
			if (messageBrokerElement.hasAttribute("path-matcher")) {
				String pathMatcherRef = messageBrokerElement.getAttribute("path-matcher");
				brokerDef.getPropertyValues().add("pathMatcher", new RuntimeBeanReference(pathMatcherRef));
			}
		}
		else if (brokerRelayElem != null) {
			String prefix = brokerRelayElem.getAttribute("prefix");
			cavs.addIndexedArgumentValue(3, Arrays.asList(StringUtils.tokenizeToStringArray(prefix, ",")));

			MutablePropertyValues values = new MutablePropertyValues();
			if (brokerRelayElem.hasAttribute("relay-host")) {
				values.add("relayHost", brokerRelayElem.getAttribute("relay-host"));
			}
			if (brokerRelayElem.hasAttribute("relay-port")) {
				values.add("relayPort", brokerRelayElem.getAttribute("relay-port"));
			}
			if (brokerRelayElem.hasAttribute("client-login")) {
				values.add("clientLogin", brokerRelayElem.getAttribute("client-login"));
			}
			if (brokerRelayElem.hasAttribute("client-passcode")) {
				values.add("clientPasscode", brokerRelayElem.getAttribute("client-passcode"));
			}
			if (brokerRelayElem.hasAttribute("system-login")) {
				values.add("systemLogin", brokerRelayElem.getAttribute("system-login"));
			}
			if (brokerRelayElem.hasAttribute("system-passcode")) {
				values.add("systemPasscode", brokerRelayElem.getAttribute("system-passcode"));
			}
			if (brokerRelayElem.hasAttribute("heartbeat-send-interval")) {
				values.add("systemHeartbeatSendInterval", brokerRelayElem.getAttribute("heartbeat-send-interval"));
			}
			if (brokerRelayElem.hasAttribute("heartbeat-receive-interval")) {
				values.add("systemHeartbeatReceiveInterval", brokerRelayElem.getAttribute("heartbeat-receive-interval"));
			}
			if (brokerRelayElem.hasAttribute("virtual-host")) {
				values.add("virtualHost", brokerRelayElem.getAttribute("virtual-host"));
			}
			Class<?> handlerType = StompBrokerRelayMessageHandler.class;
			brokerDef = new RootBeanDefinition(handlerType, cavs, values);
		}
		else {
			// Should not happen
			throw new IllegalStateException("Neither <simple-broker> nor <stomp-broker-relay> elements found.");
		}
		registerBeanDef(brokerDef, context, source);
		return brokerDef;
	}

	private RuntimeBeanReference registerMessageConverter(Element element, ParserContext context, Object source) {
		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		ManagedList<? super Object> converters = new ManagedList<Object>();
		if (convertersElement != null) {
			converters.setSource(source);
			for (Element beanElement : DomUtils.getChildElementsByTagName(convertersElement, "bean", "ref")) {
				Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
				converters.add(object);
			}
		}
		if (convertersElement == null || Boolean.valueOf(convertersElement.getAttribute("register-defaults"))) {
			converters.setSource(source);
			converters.add(new RootBeanDefinition(StringMessageConverter.class));
			converters.add(new RootBeanDefinition(ByteArrayMessageConverter.class));
			if (jackson2Present) {
				RootBeanDefinition jacksonConverterDef = new RootBeanDefinition(MappingJackson2MessageConverter.class);
				RootBeanDefinition resolverDef = new RootBeanDefinition(DefaultContentTypeResolver.class);
				resolverDef.getPropertyValues().add("defaultMimeType", MimeTypeUtils.APPLICATION_JSON);
				jacksonConverterDef.getPropertyValues().add("contentTypeResolver", resolverDef);
				// Use Jackson factory in order to have JSR-310 and Joda-Time modules registered automatically
				GenericBeanDefinition jacksonFactoryDef = new GenericBeanDefinition();
				jacksonFactoryDef.setBeanClass(Jackson2ObjectMapperFactoryBean.class);
				jacksonFactoryDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				jacksonFactoryDef.setSource(source);
				jacksonConverterDef.getPropertyValues().add("objectMapper", jacksonFactoryDef);
				converters.add(jacksonConverterDef);
			}
		}
		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, converters);
		RootBeanDefinition messageConverterDef = new RootBeanDefinition(CompositeMessageConverter.class, cavs, null);
		return new RuntimeBeanReference(registerBeanDef(messageConverterDef, context, source));
	}

	private RuntimeBeanReference registerMessagingTemplate(Element element, RuntimeBeanReference brokerChannel,
			RuntimeBeanReference messageConverter, ParserContext context, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, brokerChannel);
		RootBeanDefinition beanDef = new RootBeanDefinition(SimpMessagingTemplate.class,cavs, null);
		if (element.hasAttribute("user-destination-prefix")) {
			beanDef.getPropertyValues().add("userDestinationPrefix", element.getAttribute("user-destination-prefix"));
		}
		beanDef.getPropertyValues().add("messageConverter", messageConverter);
		return new RuntimeBeanReference(registerBeanDef(beanDef,context, source));
	}

	private void registerAnnotationMethodMessageHandler(Element messageBrokerElement,
			RuntimeBeanReference inChannel, RuntimeBeanReference outChannel,
			RuntimeBeanReference converter, RuntimeBeanReference messagingTemplate,
			ParserContext context, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, inChannel);
		cavs.addIndexedArgumentValue(1, outChannel);
		cavs.addIndexedArgumentValue(2, messagingTemplate);

		MutablePropertyValues values = new MutablePropertyValues();
		String prefixAttribute = messageBrokerElement.getAttribute("application-destination-prefix");
		values.add("destinationPrefixes", Arrays.asList(StringUtils.tokenizeToStringArray(prefixAttribute, ",")));
		values.add("messageConverter", converter);

		RootBeanDefinition beanDef = new RootBeanDefinition(SimpAnnotationMethodMessageHandler.class, cavs, values);
		if (messageBrokerElement.hasAttribute("path-matcher")) {
			String pathMatcherRef = messageBrokerElement.getAttribute("path-matcher");
			beanDef.getPropertyValues().add("pathMatcher", new RuntimeBeanReference(pathMatcherRef));
		}

		Element resolversElement = DomUtils.getChildElementByTagName(messageBrokerElement, "argument-resolvers");
		if (resolversElement != null) {
			values.add("customArgumentResolvers", extractBeanSubElements(resolversElement, context));
		}

		Element handlersElement = DomUtils.getChildElementByTagName(messageBrokerElement, "return-value-handlers");
		if (handlersElement != null) {
			values.add("customReturnValueHandlers", extractBeanSubElements(handlersElement, context));
		}

		registerBeanDef(beanDef, context, source);
	}

	private ManagedList<Object> extractBeanSubElements(Element parentElement, ParserContext parserContext) {
		ManagedList<Object> list = new ManagedList<Object>();
		list.setSource(parserContext.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean", "ref")) {
			Object object = parserContext.getDelegate().parsePropertySubElement(beanElement, null);
			list.add(object);
		}
		return list;
	}

	private RuntimeBeanReference registerUserDestinationResolver(Element brokerElem,
			RuntimeBeanReference userSessionRegistry, ParserContext context, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, userSessionRegistry);
		RootBeanDefinition beanDef = new RootBeanDefinition(DefaultUserDestinationResolver.class, cavs, null);
		if (brokerElem.hasAttribute("user-destination-prefix")) {
			beanDef.getPropertyValues().add("userDestinationPrefix", brokerElem.getAttribute("user-destination-prefix"));
		}
		return new RuntimeBeanReference(registerBeanDef(beanDef, context, source));
	}

	private RuntimeBeanReference registerUserDestinationMessageHandler(RuntimeBeanReference inChannel,
			RuntimeBeanReference brokerChannel, RuntimeBeanReference userDestinationResolver,
			ParserContext context, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, inChannel);
		cavs.addIndexedArgumentValue(1, brokerChannel);
		cavs.addIndexedArgumentValue(2, userDestinationResolver);
		RootBeanDefinition beanDef = new RootBeanDefinition(UserDestinationMessageHandler.class, cavs, null);
		return new RuntimeBeanReference(registerBeanDef(beanDef, context, source));
	}

	private void registerWebSocketMessageBrokerStats(RootBeanDefinition broker, RuntimeBeanReference inChannel,
			RuntimeBeanReference outChannel, ParserContext context, Object source) {

		RootBeanDefinition beanDef = new RootBeanDefinition(WebSocketMessageBrokerStats.class);

		RuntimeBeanReference webSocketHandler = new RuntimeBeanReference(WEB_SOCKET_HANDLER_BEAN_NAME);
		beanDef.getPropertyValues().add("subProtocolWebSocketHandler", webSocketHandler);

		if (StompBrokerRelayMessageHandler.class.equals(broker.getBeanClass())) {
			beanDef.getPropertyValues().add("stompBrokerRelay", broker);
		}
		String name = inChannel.getBeanName() + "Executor";
		if (context.getRegistry().containsBeanDefinition(name)) {
			beanDef.getPropertyValues().add("inboundChannelExecutor", context.getRegistry().getBeanDefinition(name));
		}
		name = outChannel.getBeanName() + "Executor";
		if (context.getRegistry().containsBeanDefinition(name)) {
			beanDef.getPropertyValues().add("outboundChannelExecutor", context.getRegistry().getBeanDefinition(name));
		}
		name = SOCKJS_SCHEDULER_BEAN_NAME;
		if (context.getRegistry().containsBeanDefinition(name)) {
			beanDef.getPropertyValues().add("sockJsTaskScheduler", context.getRegistry().getBeanDefinition(name));
		}
		registerBeanDefByName("webSocketMessageBrokerStats", beanDef, context, source);
	}

	private static String registerBeanDef(RootBeanDefinition beanDef, ParserContext context, Object source) {
		String name = context.getReaderContext().generateBeanName(beanDef);
		registerBeanDefByName(name, beanDef, context, source);
		return name;
	}

	private static void registerBeanDefByName(String name, RootBeanDefinition beanDef, ParserContext context, Object source) {
		beanDef.setSource(source);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		context.getRegistry().registerBeanDefinition(name, beanDef);
		context.registerComponent(new BeanComponentDefinition(beanDef, name));
	}


	private static class DecoratingFactoryBean implements FactoryBean<WebSocketHandler> {

		private final WebSocketHandler handler;

		private final List<WebSocketHandlerDecoratorFactory> factories;


		private DecoratingFactoryBean(WebSocketHandler handler, List<WebSocketHandlerDecoratorFactory> factories) {
			this.handler = handler;
			this.factories = factories;
		}

		@Override
		public WebSocketHandler getObject() throws Exception {
			WebSocketHandler result = this.handler;
			for (WebSocketHandlerDecoratorFactory factory : this.factories) {
				result = factory.decorate(result);
			}
			return result;
		}

		@Override
		public Class<?> getObjectType() {
			return WebSocketHandler.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

}
