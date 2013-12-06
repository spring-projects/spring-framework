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

import org.springframework.beans.MutablePropertyValues;
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
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.handler.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.handler.DefaultUserSessionRegistry;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.handler.UserDestinationMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;


/**
 * A {@link org.springframework.beans.factory.xml.BeanDefinitionParser}
 * that provides the configuration for the
 * {@code <websocket:message-broker/>} XML namespace element.
 * <p>
 * Registers a Spring MVC {@link org.springframework.web.servlet.handler.SimpleUrlHandlerMapping}
 * with order=1 to map HTTP WebSocket handshake requests from STOMP/WebSocket clients.
 * <p>
 * Registers the following {@link org.springframework.messaging.MessageChannel}s:
 * <ul>
 * 	<li>"clientInboundChannel" for receiving messages from clients (e.g. WebSocket clients)
 * 	<li>"clientOutboundChannel" for sending messages to clients (e.g. WebSocket clients)
 * 	<li>"brokerChannel" for sending messages from within the application to the message broker
 * </ul>
 * <p>
 * Registers one of the following based on the selected message broker options:
 * <ul>
 *     <li> a {@link SimpleBrokerMessageHandler} if the <simple-broker/> is used
 *     <li> a {@link StompBrokerRelayMessageHandler} if the <stomp-broker-relay/> is used
 * </ul>
 * <p>
 * Registers a {@link UserDestinationMessageHandler} for handling user destinations.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class MessageBrokerBeanDefinitionParser implements BeanDefinitionParser {

	protected static final String SOCKJS_SCHEDULER_BEAN_NAME = "messageBrokerSockJsScheduler";

	private static final int DEFAULT_MAPPING_ORDER = 1;

	private static final boolean jackson2Present= ClassUtils.isPresent(
			"com.fasterxml.jackson.databind.ObjectMapper", MessageBrokerBeanDefinitionParser.class.getClassLoader());


	@Override
	public BeanDefinition parse(Element element, ParserContext parserCxt) {

		Object source = parserCxt.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		parserCxt.pushContainingComponent(compDefinition);

		String orderAttribute = element.getAttribute("order");
		int order = orderAttribute.isEmpty() ? DEFAULT_MAPPING_ORDER : Integer.valueOf(orderAttribute);

		ManagedMap<String, Object> urlMap = new ManagedMap<String, Object>();
		urlMap.setSource(source);

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
		handlerMappingDef.getPropertyValues().add("order", order);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);

		String beanName = "clientInboundChannel";
		Element channelElem = DomUtils.getChildElementByTagName(element, "client-inbound-channel");
		RuntimeBeanReference clientInChannel = getMessageChannel(beanName, channelElem, parserCxt, source);

		beanName = "clientOutboundChannel";
		channelElem = DomUtils.getChildElementByTagName(element, "client-outbound-channel");
		RuntimeBeanReference clientOutChannel = getMessageChannel(beanName, channelElem, parserCxt, source);

		RootBeanDefinition beanDef = new RootBeanDefinition(DefaultUserSessionRegistry.class);
		beanName = registerBeanDef(beanDef, parserCxt, source);
		RuntimeBeanReference userSessionRegistry = new RuntimeBeanReference(beanName);

		RuntimeBeanReference subProtocolWsHandler = registerSubProtocolWebSocketHandler(
				clientInChannel, clientOutChannel, userSessionRegistry, parserCxt, source);

		for(Element stompEndpointElem : DomUtils.getChildElementsByTagName(element, "stomp-endpoint")) {

			RuntimeBeanReference httpRequestHandler = registerHttpRequestHandler(
					stompEndpointElem, subProtocolWsHandler, parserCxt, source);

			String pathAttribute = stompEndpointElem.getAttribute("path");
			Assert.state(StringUtils.hasText(pathAttribute), "Invalid <stomp-endpoint> (no path mapping)");

			List<String> paths = Arrays.asList(pathAttribute.split(","));
			for(String path : paths) {
				path = path.trim();
				Assert.state(StringUtils.hasText(path), "Invalid <stomp-endpoint> path attribute: " + pathAttribute);
				if (DomUtils.getChildElementByTagName(stompEndpointElem, "sockjs") != null) {
					path = path.endsWith("/") ? path + "**" : path + "/**";
				}
				urlMap.put(path, httpRequestHandler);
			}
		}

		registerBeanDef(handlerMappingDef, parserCxt, source);

		beanName = "brokerChannel";
		channelElem = DomUtils.getChildElementByTagName(element, "broker-channel");
		RuntimeBeanReference brokerChannel = getMessageChannel(beanName, channelElem, parserCxt, source);
		registerMessageBroker(element, clientInChannel, clientOutChannel, brokerChannel, parserCxt, source);

		RuntimeBeanReference messageConverter = registerBrokerMessageConverter(parserCxt, source);
		RuntimeBeanReference messagingTemplate = registerBrokerMessagingTemplate(element, brokerChannel,
				messageConverter, parserCxt, source);

		registerAnnotationMethodMessageHandler(element, clientInChannel, clientOutChannel,
				messageConverter, messagingTemplate, parserCxt, source);

		RuntimeBeanReference userDestinationResolver = registerUserDestinationResolver(element,
				userSessionRegistry, parserCxt, source);

		registerUserDestinationMessageHandler(clientInChannel, clientOutChannel, brokerChannel,
				userDestinationResolver, parserCxt, source);

		parserCxt.popAndRegisterContainingComponent();

		return null;
	}

	private RuntimeBeanReference getMessageChannel(String channelName, Element channelElement,
			ParserContext parserCxt, Object source) {

		RootBeanDefinition executorDef = null;

		if (channelElement != null) {
			Element executor = DomUtils.getChildElementByTagName(channelElement, "executor");
			if (executor != null) {
				executorDef = new RootBeanDefinition(ThreadPoolTaskExecutor.class);
				String attrValue = executor.getAttribute("core-pool-size");
				if (!StringUtils.isEmpty(attrValue)) {
					executorDef.getPropertyValues().add("corePoolSize", attrValue);
				}
				attrValue = executor.getAttribute("max-pool-size");
				if (!StringUtils.isEmpty(attrValue)) {
					executorDef.getPropertyValues().add("maxPoolSize", attrValue);
				}
				attrValue = executor.getAttribute("keep-alive-seconds");
				if (!StringUtils.isEmpty(attrValue)) {
					executorDef.getPropertyValues().add("keepAliveSeconds", attrValue);
				}
				attrValue = executor.getAttribute("queue-capacity");
				if (!StringUtils.isEmpty(attrValue)) {
					executorDef.getPropertyValues().add("queueCapacity", attrValue);
				}
			}
		}
		else if (!channelName.equals("brokerChannel")) {
			executorDef = new RootBeanDefinition(ThreadPoolTaskExecutor.class);
		}

		ConstructorArgumentValues values = new ConstructorArgumentValues();
		if (executorDef != null) {
			executorDef.getPropertyValues().add("threadNamePrefix", channelName + "-");
			String executorName = channelName + "Executor";
			registerBeanDefByName(executorName, executorDef, parserCxt, source);
			values.addIndexedArgumentValue(0, new RuntimeBeanReference(executorName));
		}

		RootBeanDefinition channelDef = new RootBeanDefinition(ExecutorSubscribableChannel.class, values, null);

		if (channelElement != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(channelElement, "interceptors");
			ManagedList<?> interceptorList = WebSocketNamespaceUtils.parseBeanSubElements(interceptorsElement, parserCxt);
			channelDef.getPropertyValues().add("interceptors", interceptorList);
		}

		registerBeanDefByName(channelName, channelDef, parserCxt, source);
		return new RuntimeBeanReference(channelName);
	}

	private RuntimeBeanReference registerSubProtocolWebSocketHandler(
			RuntimeBeanReference clientInChannel, RuntimeBeanReference clientOutChannel,
			RuntimeBeanReference userSessionRegistry, ParserContext parserCxt, Object source) {

		RootBeanDefinition stompHandlerDef = new RootBeanDefinition(StompSubProtocolHandler.class);
		stompHandlerDef.getPropertyValues().add("userSessionRegistry", userSessionRegistry);
		registerBeanDef(stompHandlerDef, parserCxt, source);

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, clientInChannel);
		cavs.addIndexedArgumentValue(1, clientOutChannel);

		RootBeanDefinition subProtocolWshDef = new RootBeanDefinition(SubProtocolWebSocketHandler.class, cavs, null);
		subProtocolWshDef.getPropertyValues().addPropertyValue("protocolHandlers", stompHandlerDef);
		String subProtocolWshName = registerBeanDef(subProtocolWshDef, parserCxt, source);
		return new RuntimeBeanReference(subProtocolWshName);
	}

	private RuntimeBeanReference registerHttpRequestHandler(Element stompEndpointElement,
			RuntimeBeanReference subProtocolWebSocketHandler, ParserContext parserCxt, Object source) {

		RootBeanDefinition httpRequestHandlerDef;

		RuntimeBeanReference handshakeHandler =
				WebSocketNamespaceUtils.registerHandshakeHandler(stompEndpointElement, parserCxt, source);

		RuntimeBeanReference sockJsService = WebSocketNamespaceUtils.registerSockJsService(
				stompEndpointElement, SOCKJS_SCHEDULER_BEAN_NAME, parserCxt, source);

		if (sockJsService != null) {
			ConstructorArgumentValues cavs = new ConstructorArgumentValues();
			cavs.addIndexedArgumentValue(0, sockJsService);
			cavs.addIndexedArgumentValue(1, subProtocolWebSocketHandler);
			httpRequestHandlerDef = new RootBeanDefinition(SockJsHttpRequestHandler.class, cavs, null);
		}
		else {
			ConstructorArgumentValues cavs = new ConstructorArgumentValues();
			cavs.addIndexedArgumentValue(0, subProtocolWebSocketHandler);
			if(handshakeHandler != null) {
				cavs.addIndexedArgumentValue(1, handshakeHandler);
			}
			httpRequestHandlerDef = new RootBeanDefinition(WebSocketHttpRequestHandler.class, cavs, null);
		}

		String httpRequestHandlerBeanName = registerBeanDef(httpRequestHandlerDef, parserCxt, source);
		return new RuntimeBeanReference(httpRequestHandlerBeanName);
	}

	private void registerMessageBroker(Element messageBrokerElement, RuntimeBeanReference clientInChannelDef,
			RuntimeBeanReference clientOutChannelDef, RuntimeBeanReference brokerChannelDef,
			ParserContext parserCxt, Object source) {

		Element simpleBrokerElem = DomUtils.getChildElementByTagName(messageBrokerElement, "simple-broker");
		Element brokerRelayElem = DomUtils.getChildElementByTagName(messageBrokerElement, "stomp-broker-relay");

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, clientInChannelDef);
		cavs.addIndexedArgumentValue(1, clientOutChannelDef);
		cavs.addIndexedArgumentValue(2, brokerChannelDef);

		if (simpleBrokerElem != null) {

			String prefix = simpleBrokerElem.getAttribute("prefix");
			cavs.addIndexedArgumentValue(3, Arrays.asList(prefix.split(",")));
			RootBeanDefinition brokerDef = new RootBeanDefinition(SimpleBrokerMessageHandler.class, cavs, null);
			registerBeanDef(brokerDef, parserCxt, source);
		}
		else if (brokerRelayElem != null) {

			String prefix = brokerRelayElem.getAttribute("prefix");
			cavs.addIndexedArgumentValue(3, Arrays.asList(prefix.split(",")));

			MutablePropertyValues mpvs = new MutablePropertyValues();
			String relayHost = brokerRelayElem.getAttribute("relay-host");
			if(!relayHost.isEmpty()) {
				mpvs.add("relayHost",relayHost);
			}
			String relayPort = brokerRelayElem.getAttribute("relay-port");
			if(!relayPort.isEmpty()) {
				mpvs.add("relayPort", Integer.valueOf(relayPort));
			}
			String attrValue = brokerRelayElem.getAttribute("login");
			if(!attrValue.isEmpty()) {
				mpvs.add("systemLogin",attrValue);
			}
			attrValue = brokerRelayElem.getAttribute("passcode");
			if(!attrValue.isEmpty()) {
				mpvs.add("systemPasscode", attrValue);
			}
			attrValue = brokerRelayElem.getAttribute("heartbeat-send-interval");
			if(!attrValue.isEmpty()) {
				mpvs.add("systemHeartbeatSendInterval", Long.parseLong(attrValue));
			}
			attrValue = brokerRelayElem.getAttribute("heartbeat-receive-interval");
			if(!attrValue.isEmpty()) {
				mpvs.add("systemHeartbeatReceiveInterval", Long.parseLong(attrValue));
			}
			attrValue = brokerRelayElem.getAttribute("virtual-host");
			if(!attrValue.isEmpty()) {
				mpvs.add("virtualHost", attrValue);
			}

			Class<?> handlerType = StompBrokerRelayMessageHandler.class;
			RootBeanDefinition messageBrokerDef = new RootBeanDefinition(handlerType, cavs, mpvs);
			registerBeanDef(messageBrokerDef, parserCxt, source);
		}

	}

	private RuntimeBeanReference registerBrokerMessageConverter(ParserContext parserCxt, Object source) {

		RootBeanDefinition contentTypeResolverDef = new RootBeanDefinition(DefaultContentTypeResolver.class);

		ManagedList<RootBeanDefinition> convertersDef = new ManagedList<RootBeanDefinition>();
		if (jackson2Present) {
			convertersDef.add(new RootBeanDefinition(MappingJackson2MessageConverter.class));
			contentTypeResolverDef.getPropertyValues().add("defaultMimeType", MimeTypeUtils.APPLICATION_JSON);
		}
		convertersDef.add(new RootBeanDefinition(StringMessageConverter.class));
		convertersDef.add(new RootBeanDefinition(ByteArrayMessageConverter.class));

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, convertersDef);
		cavs.addIndexedArgumentValue(1, contentTypeResolverDef);

		RootBeanDefinition brokerMessage = new RootBeanDefinition(CompositeMessageConverter.class, cavs, null);
		return new RuntimeBeanReference(registerBeanDef(brokerMessage,parserCxt, source));
	}

	private RuntimeBeanReference registerBrokerMessagingTemplate(
			Element element, RuntimeBeanReference brokerChannelDef, RuntimeBeanReference messageConverterRef,
			ParserContext parserCxt, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, brokerChannelDef);
		RootBeanDefinition messagingTemplateDef = new RootBeanDefinition(SimpMessagingTemplate.class,cavs, null);

		String userDestinationPrefixAttribute = element.getAttribute("user-destination-prefix");
		if(!userDestinationPrefixAttribute.isEmpty()) {
			messagingTemplateDef.getPropertyValues().add("userDestinationPrefix", userDestinationPrefixAttribute);
		}
		messagingTemplateDef.getPropertyValues().add("messageConverter", messageConverterRef);

		return new RuntimeBeanReference(registerBeanDef(messagingTemplateDef,parserCxt, source));
	}

	private void registerAnnotationMethodMessageHandler(Element messageBrokerElement,
			RuntimeBeanReference clientInChannelDef, RuntimeBeanReference clientOutChannelDef,
			RuntimeBeanReference brokerMessageConverterRef, RuntimeBeanReference brokerMessagingTemplateRef,
			ParserContext parserCxt, Object source) {

		String applicationDestinationPrefix = messageBrokerElement.getAttribute("application-destination-prefix");

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, clientInChannelDef);
		cavs.addIndexedArgumentValue(1, clientOutChannelDef);
		cavs.addIndexedArgumentValue(2, brokerMessagingTemplateRef);

		MutablePropertyValues mpvs = new MutablePropertyValues();
		mpvs.add("destinationPrefixes",Arrays.asList(applicationDestinationPrefix.split(",")));
		mpvs.add("messageConverter", brokerMessageConverterRef);

		RootBeanDefinition annotationMethodMessageHandlerDef =
				new RootBeanDefinition(SimpAnnotationMethodMessageHandler.class, cavs, mpvs);

		registerBeanDef(annotationMethodMessageHandlerDef, parserCxt, source);
	}

	private RuntimeBeanReference registerUserDestinationResolver(Element messageBrokerElement,
			RuntimeBeanReference userSessionRegistry, ParserContext parserCxt, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, userSessionRegistry);
		RootBeanDefinition userDestinationResolverDef =
				new RootBeanDefinition(DefaultUserDestinationResolver.class, cavs, null);
		String prefix = messageBrokerElement.getAttribute("user-destination-prefix");
		if (!prefix.isEmpty()) {
			userDestinationResolverDef.getPropertyValues().add("userDestinationPrefix", prefix);
		}
		String userDestinationResolverName = registerBeanDef(userDestinationResolverDef, parserCxt, source);
		return new RuntimeBeanReference(userDestinationResolverName);
	}

	private RuntimeBeanReference registerUserDestinationMessageHandler(RuntimeBeanReference clientInChannelDef,
			RuntimeBeanReference clientOutChannelDef, RuntimeBeanReference brokerChannelDef,
			RuntimeBeanReference userDestinationResolverRef, ParserContext parserCxt, Object source) {

		ConstructorArgumentValues cavs = new ConstructorArgumentValues();
		cavs.addIndexedArgumentValue(0, clientInChannelDef);
		cavs.addIndexedArgumentValue(1, clientOutChannelDef);
		cavs.addIndexedArgumentValue(2, brokerChannelDef);
		cavs.addIndexedArgumentValue(3, userDestinationResolverRef);

		RootBeanDefinition userDestinationMessageHandlerDef =
				new RootBeanDefinition(UserDestinationMessageHandler.class, cavs, null);

		String userDestinationMessageHandleName = registerBeanDef(userDestinationMessageHandlerDef, parserCxt, source);
		return new RuntimeBeanReference(userDestinationMessageHandleName);
	}


	private static String registerBeanDef(RootBeanDefinition beanDef, ParserContext parserCxt, Object source) {
		String beanName = parserCxt.getReaderContext().generateBeanName(beanDef);
		registerBeanDefByName(beanName, beanDef, parserCxt, source);
		return beanName;
	}

	private static void registerBeanDefByName(String beanName, RootBeanDefinition beanDef,
			ParserContext parserCxt, Object source) {

		beanDef.setSource(source);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		parserCxt.getRegistry().registerBeanDefinition(beanName, beanDef);
		parserCxt.registerComponent(new BeanComponentDefinition(beanDef, beanName));
	}

}
