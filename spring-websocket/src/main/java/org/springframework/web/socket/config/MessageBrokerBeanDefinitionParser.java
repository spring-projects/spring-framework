/*
 * Copyright 2002-2025 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperFactoryBean;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.GsonMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.JsonbMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpSessionScope;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketAnnotationMethodMessageHandler;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;

/**
 * A {@link org.springframework.beans.factory.xml.BeanDefinitionParser} that provides
 * the configuration for the {@code <websocket:message-broker/>} XML namespace element.
 *
 * <p>Registers a Spring MVC {@link org.springframework.web.servlet.HandlerMapping}
 * with order 1 to map HTTP WebSocket handshake requests from STOMP/WebSocket clients.
 *
 * <p>Registers the following {@link org.springframework.messaging.MessageChannel MessageChannels}:
 * <ul>
 * <li>"clientInboundChannel" for receiving messages from clients (for example, WebSocket clients)
 * <li>"clientOutboundChannel" for sending messages to clients (for example, WebSocket clients)
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
 * @author Sebastien Deleuze
 * @since 4.0
 */
class MessageBrokerBeanDefinitionParser implements BeanDefinitionParser {

	public static final String WEB_SOCKET_HANDLER_BEAN_NAME = "subProtocolWebSocketHandler";

	public static final String SCHEDULER_BEAN_NAME = "messageBrokerScheduler";

	public static final String SOCKJS_SCHEDULER_BEAN_NAME = "messageBrokerSockJsScheduler";

	public static final String MESSAGING_TEMPLATE_BEAN_NAME = "brokerMessagingTemplate";

	public static final String MESSAGE_CONVERTER_BEAN_NAME = "brokerMessageConverter";

	private static final int DEFAULT_MAPPING_ORDER = 1;

	private static final boolean jacksonPresent;

	private static final boolean jackson2Present;

	private static final boolean gsonPresent;

	private static final boolean jsonbPresent;

	private static final boolean javaxValidationPresent;

	static {
		ClassLoader classLoader = MessageBrokerBeanDefinitionParser.class.getClassLoader();
		jacksonPresent = ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", classLoader);
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		gsonPresent = ClassUtils.isPresent("com.google.gson.Gson", classLoader);
		jsonbPresent = ClassUtils.isPresent("jakarta.json.bind.Jsonb", classLoader);
		javaxValidationPresent = ClassUtils.isPresent("jakarta.validation.Validator", classLoader);
	}


	@Override
	public @Nullable BeanDefinition parse(Element element, ParserContext context) {
		Object source = context.extractSource(element);
		CompositeComponentDefinition compDefinition = new CompositeComponentDefinition(element.getTagName(), source);
		context.pushContainingComponent(compDefinition);

		Element channelElem = DomUtils.getChildElementByTagName(element, "client-inbound-channel");
		RuntimeBeanReference inChannel = getMessageChannel("clientInboundChannel", channelElem, context, source);

		channelElem = DomUtils.getChildElementByTagName(element, "client-outbound-channel");
		RuntimeBeanReference outChannel = getMessageChannel("clientOutboundChannel", channelElem, context, source);

		channelElem = DomUtils.getChildElementByTagName(element, "broker-channel");
		RuntimeBeanReference brokerChannel = getMessageChannel("brokerChannel", channelElem, context, source);

		RuntimeBeanReference userRegistry = registerUserRegistry(element, context, source);
		Object userDestHandler = registerUserDestHandler(element, userRegistry, inChannel, brokerChannel, context, source);

		RuntimeBeanReference converter = registerMessageConverter(element, context, source);
		RuntimeBeanReference template = registerMessagingTemplate(element, brokerChannel, converter, context, source);
		registerAnnotationMethodMessageHandler(element, inChannel, outChannel,converter, template, context, source);

		RootBeanDefinition broker = registerMessageBroker(element, inChannel, outChannel, brokerChannel,
				userDestHandler, template, userRegistry, context, source);

		// WebSocket and sub-protocol handling

		ManagedMap<String, Object> urlMap = registerHandlerMapping(element, context, source);
		RuntimeBeanReference stompHandler = registerStompHandler(element, inChannel, outChannel, context, source);
		for (Element endpointElem : DomUtils.getChildElementsByTagName(element, "stomp-endpoint")) {
			RuntimeBeanReference requestHandler = registerRequestHandler(endpointElem, stompHandler, context, source);
			String pathAttribute = endpointElem.getAttribute("path");
			Assert.hasText(pathAttribute, "Invalid <stomp-endpoint> (no path mapping)");
			for (String path : StringUtils.tokenizeToStringArray(pathAttribute, ",")) {
				path = path.trim();
				Assert.hasText(path, () -> "Invalid <stomp-endpoint> path attribute: " + pathAttribute);
				if (DomUtils.getChildElementByTagName(endpointElem, "sockjs") != null) {
					path = (path.endsWith("/") ? path + "**" : path + "/**");
				}
				urlMap.put(path, requestHandler);
			}
		}

		Map<String, Object> scopeMap = Collections.singletonMap("websocket", new SimpSessionScope());
		RootBeanDefinition scopeConfigurer = new RootBeanDefinition(CustomScopeConfigurer.class);
		scopeConfigurer.getPropertyValues().add("scopes", scopeMap);
		registerBeanDefByName("webSocketScopeConfigurer", scopeConfigurer, context, source);

		registerWebSocketMessageBrokerStats(broker, inChannel, outChannel, context, source);

		context.popAndRegisterContainingComponent();
		return null;
	}

	private RuntimeBeanReference registerUserRegistry(Element element, ParserContext context, @Nullable Object source) {
		Element relayElement = DomUtils.getChildElementByTagName(element, "stomp-broker-relay");
		boolean multiServer = (relayElement != null && relayElement.hasAttribute("user-registry-broadcast"));

		if (multiServer) {
			RootBeanDefinition localRegistryBeanDef = new RootBeanDefinition(DefaultSimpUserRegistry.class);
			RootBeanDefinition beanDef = new RootBeanDefinition(MultiServerUserRegistry.class);
			beanDef.getConstructorArgumentValues().addIndexedArgumentValue(0, localRegistryBeanDef);
			String beanName = registerBeanDef(beanDef, context, source);
			return new RuntimeBeanReference(beanName);
		}
		else {
			RootBeanDefinition beanDef = new RootBeanDefinition(DefaultSimpUserRegistry.class);
			String beanName = registerBeanDef(beanDef, context, source);
			return new RuntimeBeanReference(beanName);
		}
	}

	private ManagedMap<String, Object> registerHandlerMapping(
			Element element, ParserContext context, @Nullable Object source) {

		RootBeanDefinition handlerMappingDef = new RootBeanDefinition(WebSocketHandlerMapping.class);

		String orderAttribute = element.getAttribute("order");
		int order = orderAttribute.isEmpty() ? DEFAULT_MAPPING_ORDER : Integer.parseInt(orderAttribute);
		handlerMappingDef.getPropertyValues().add("order", order);

		String pathHelper = element.getAttribute("path-helper");
		if (StringUtils.hasText(pathHelper)) {
			handlerMappingDef.getPropertyValues().add("urlPathHelper", new RuntimeBeanReference(pathHelper));
		}

		ManagedMap<String, Object> urlMap = new ManagedMap<>();
		urlMap.setSource(source);
		handlerMappingDef.getPropertyValues().add("urlMap", urlMap);

		registerBeanDef(handlerMappingDef, context, source);
		return urlMap;
	}

	private RuntimeBeanReference getMessageChannel(
			String name, @Nullable Element element, ParserContext context, @Nullable Object source) {

		RootBeanDefinition executor;
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

		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		if (executor != null) {
			executor.getPropertyValues().add("threadNamePrefix", name + "-");
			String executorName = name + "Executor";
			registerBeanDefByName(executorName, executor, context, source);
			cargs.addIndexedArgumentValue(0, new RuntimeBeanReference(executorName));
		}

		RootBeanDefinition channelDef = new RootBeanDefinition(ExecutorSubscribableChannel.class, cargs, null);
		ManagedList<Object> interceptors = new ManagedList<>();
		if (element != null) {
			Element interceptorsElement = DomUtils.getChildElementByTagName(element, "interceptors");
			interceptors.addAll(WebSocketNamespaceUtils.parseBeanSubElements(interceptorsElement, context));
		}
		interceptors.add(new ImmutableMessageChannelInterceptor());
		channelDef.getPropertyValues().add("interceptors", interceptors);

		registerBeanDefByName(name, channelDef, context, source);
		return new RuntimeBeanReference(name);
	}

	private @Nullable RootBeanDefinition getDefaultExecutorBeanDefinition(String channelName) {
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

	private RuntimeBeanReference registerStompHandler(Element element, RuntimeBeanReference inChannel,
			RuntimeBeanReference outChannel, ParserContext context, @Nullable Object source) {

		RootBeanDefinition stompHandlerDef = new RootBeanDefinition(StompSubProtocolHandler.class);
		registerBeanDef(stompHandlerDef, context, source);

		Element errorHandlerElem = DomUtils.getChildElementByTagName(element, "stomp-error-handler");
		if (errorHandlerElem != null) {
			RuntimeBeanReference errorHandlerRef = new RuntimeBeanReference(errorHandlerElem.getAttribute("ref"));
			stompHandlerDef.getPropertyValues().add("errorHandler", errorHandlerRef);
		}

		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		cargs.addIndexedArgumentValue(0, inChannel);
		cargs.addIndexedArgumentValue(1, outChannel);

		RootBeanDefinition handlerDef = new RootBeanDefinition(SubProtocolWebSocketHandler.class, cargs, null);
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
			if (transportElem.hasAttribute("time-to-first-message")) {
				handlerDef.getPropertyValues().add("timeToFirstMessage", transportElem.getAttribute("time-to-first-message"));
			}
			Element factoriesElement = DomUtils.getChildElementByTagName(transportElem, "decorator-factories");
			if (factoriesElement != null) {
				ManagedList<Object> factories = extractBeanSubElements(factoriesElement, context);
				RootBeanDefinition factoryBean = new RootBeanDefinition(DecoratingFactoryBean.class);
				factoryBean.getConstructorArgumentValues().addIndexedArgumentValue(0, result);
				factoryBean.getConstructorArgumentValues().addIndexedArgumentValue(1, factories);
				result = new RuntimeBeanReference(registerBeanDef(factoryBean, context, source));
			}
		}
		return result;
	}

	private RuntimeBeanReference registerRequestHandler(
			Element element, RuntimeBeanReference subProtoHandler, ParserContext ctx, @Nullable Object source) {

		RootBeanDefinition beanDef;

		RuntimeBeanReference sockJsService = WebSocketNamespaceUtils.registerSockJsService(
				element, SCHEDULER_BEAN_NAME, ctx, source);

		if (sockJsService != null) {
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, sockJsService);
			cargs.addIndexedArgumentValue(1, subProtoHandler);
			beanDef = new RootBeanDefinition(SockJsHttpRequestHandler.class, cargs, null);

			// Register alias for backwards compatibility with 4.1
			ctx.getRegistry().registerAlias(SCHEDULER_BEAN_NAME, SOCKJS_SCHEDULER_BEAN_NAME);
		}
		else {
			RuntimeBeanReference handler = WebSocketNamespaceUtils.registerHandshakeHandler(element, ctx, source);
			Element interceptElem = DomUtils.getChildElementByTagName(element, "handshake-interceptors");
			ManagedList<Object> interceptors = WebSocketNamespaceUtils.parseBeanSubElements(interceptElem, ctx);
			String allowedOrigins = element.getAttribute("allowed-origins");
			List<String> origins = Arrays.asList(StringUtils.tokenizeToStringArray(allowedOrigins, ","));
			String allowedOriginPatterns = element.getAttribute("allowed-origin-patterns");
			List<String> originPatterns = Arrays.asList(StringUtils.tokenizeToStringArray(allowedOriginPatterns, ","));
			OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(origins);
			if (!ObjectUtils.isEmpty(originPatterns)) {
				interceptor.setAllowedOriginPatterns(originPatterns);
			}
			interceptors.add(interceptor);
			ConstructorArgumentValues cargs = new ConstructorArgumentValues();
			cargs.addIndexedArgumentValue(0, subProtoHandler);
			cargs.addIndexedArgumentValue(1, handler);
			beanDef = new RootBeanDefinition(WebSocketHttpRequestHandler.class, cargs, null);
			beanDef.getPropertyValues().add("handshakeInterceptors", interceptors);
		}
		return new RuntimeBeanReference(registerBeanDef(beanDef, ctx, source));
	}

	private RootBeanDefinition registerMessageBroker(Element brokerElement,
			RuntimeBeanReference inChannel, RuntimeBeanReference outChannel, RuntimeBeanReference brokerChannel,
			Object userDestHandler, RuntimeBeanReference brokerTemplate, RuntimeBeanReference userRegistry,
			ParserContext context, @Nullable Object source) {

		Element simpleBrokerElem = DomUtils.getChildElementByTagName(brokerElement, "simple-broker");
		Element brokerRelayElem = DomUtils.getChildElementByTagName(brokerElement, "stomp-broker-relay");

		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		cargs.addIndexedArgumentValue(0, inChannel);
		cargs.addIndexedArgumentValue(1, outChannel);
		cargs.addIndexedArgumentValue(2, brokerChannel);

		RootBeanDefinition brokerDef;
		if (simpleBrokerElem != null) {
			String prefix = simpleBrokerElem.getAttribute("prefix");
			cargs.addIndexedArgumentValue(3, Arrays.asList(StringUtils.tokenizeToStringArray(prefix, ",")));
			brokerDef = new RootBeanDefinition(SimpleBrokerMessageHandler.class, cargs, null);
			if (brokerElement.hasAttribute("path-matcher")) {
				String pathMatcherRef = brokerElement.getAttribute("path-matcher");
				brokerDef.getPropertyValues().add("pathMatcher", new RuntimeBeanReference(pathMatcherRef));
			}
			if (simpleBrokerElem.hasAttribute("scheduler")) {
				String scheduler = simpleBrokerElem.getAttribute("scheduler");
				brokerDef.getPropertyValues().add("taskScheduler", new RuntimeBeanReference(scheduler));
			}
			if (simpleBrokerElem.hasAttribute("heartbeat")) {
				String heartbeatValue = simpleBrokerElem.getAttribute("heartbeat");
				brokerDef.getPropertyValues().add("heartbeatValue", heartbeatValue);
			}
			if (simpleBrokerElem.hasAttribute("selector-header")) {
				String headerName = simpleBrokerElem.getAttribute("selector-header");
				brokerDef.getPropertyValues().add("selectorHeaderName", headerName);
			}
		}
		else if (brokerRelayElem != null) {
			String prefix = brokerRelayElem.getAttribute("prefix");
			cargs.addIndexedArgumentValue(3, Arrays.asList(StringUtils.tokenizeToStringArray(prefix, ",")));

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
			ManagedMap<String, Object> map = new ManagedMap<>();
			map.setSource(source);
			if (brokerRelayElem.hasAttribute("user-destination-broadcast")) {
				String destination = brokerRelayElem.getAttribute("user-destination-broadcast");
				map.put(destination, userDestHandler);
			}
			if (brokerRelayElem.hasAttribute("user-registry-broadcast")) {
				String destination = brokerRelayElem.getAttribute("user-registry-broadcast");
				map.put(destination, registerUserRegistryMessageHandler(userRegistry,
						brokerTemplate, destination, context, source));
			}
			if (!map.isEmpty()) {
				values.add("systemSubscriptions", map);
			}
			Class<?> handlerType = StompBrokerRelayMessageHandler.class;
			brokerDef = new RootBeanDefinition(handlerType, cargs, values);
		}
		else {
			// Should not happen
			throw new IllegalStateException("Neither <simple-broker> nor <stomp-broker-relay> elements found.");
		}

		if (brokerElement.hasAttribute("preserve-publish-order")) {
			String preservePublishOrder = brokerElement.getAttribute("preserve-publish-order");
			brokerDef.getPropertyValues().add("preservePublishOrder", preservePublishOrder);
		}

		registerBeanDef(brokerDef, context, source);
		return brokerDef;
	}

	private RuntimeBeanReference registerUserRegistryMessageHandler(
			RuntimeBeanReference userRegistry, RuntimeBeanReference brokerTemplate,
			String destination, ParserContext context, @Nullable Object source) {

		Object scheduler = WebSocketNamespaceUtils.registerScheduler(SCHEDULER_BEAN_NAME, context, source);

		RootBeanDefinition beanDef = new RootBeanDefinition(UserRegistryMessageHandler.class);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(0, userRegistry);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(1, brokerTemplate);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(2, destination);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(3, scheduler);

		String beanName = registerBeanDef(beanDef, context, source);
		return new RuntimeBeanReference(beanName);
	}

	@SuppressWarnings("removal")
	private RuntimeBeanReference registerMessageConverter(
			Element element, ParserContext context, @Nullable Object source) {

		Element convertersElement = DomUtils.getChildElementByTagName(element, "message-converters");
		ManagedList<Object> converters = new ManagedList<>();
		if (convertersElement != null) {
			converters.setSource(source);
			for (Element beanElement : DomUtils.getChildElementsByTagName(convertersElement, "bean", "ref")) {
				Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
				converters.add(object);
			}
		}
		if (convertersElement == null || Boolean.parseBoolean(convertersElement.getAttribute("register-defaults"))) {
			converters.setSource(source);
			converters.add(new RootBeanDefinition(StringMessageConverter.class));
			converters.add(new RootBeanDefinition(ByteArrayMessageConverter.class));
			if (jacksonPresent) {
				RootBeanDefinition jacksonConverterDef = new RootBeanDefinition(JacksonJsonMessageConverter.class);
				RootBeanDefinition resolverDef = new RootBeanDefinition(DefaultContentTypeResolver.class);
				resolverDef.getPropertyValues().add("defaultMimeType", MimeTypeUtils.APPLICATION_JSON);
				jacksonConverterDef.getPropertyValues().add("contentTypeResolver", resolverDef);
				converters.add(jacksonConverterDef);
			}
			else if (jackson2Present) {
				RootBeanDefinition jacksonConverterDef = new RootBeanDefinition(MappingJackson2MessageConverter.class);
				RootBeanDefinition resolverDef = new RootBeanDefinition(DefaultContentTypeResolver.class);
				resolverDef.getPropertyValues().add("defaultMimeType", MimeTypeUtils.APPLICATION_JSON);
				jacksonConverterDef.getPropertyValues().add("contentTypeResolver", resolverDef);
				// Use Jackson factory in order to have well known modules registered automatically
				GenericBeanDefinition jacksonFactoryDef = new GenericBeanDefinition();
				jacksonFactoryDef.setBeanClass(Jackson2ObjectMapperFactoryBean.class);
				jacksonFactoryDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
				jacksonFactoryDef.setSource(source);
				jacksonConverterDef.getPropertyValues().add("objectMapper", jacksonFactoryDef);
				converters.add(jacksonConverterDef);
			}
			else if (gsonPresent) {
				converters.add(new RootBeanDefinition(GsonMessageConverter.class));
			}
			else if (jsonbPresent) {
				converters.add(new RootBeanDefinition(JsonbMessageConverter.class));
			}
		}
		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		cargs.addIndexedArgumentValue(0, converters);
		RootBeanDefinition messageConverterDef = new RootBeanDefinition(CompositeMessageConverter.class, cargs, null);
		String name = MESSAGE_CONVERTER_BEAN_NAME;
		registerBeanDefByName(name, messageConverterDef, context, source);
		return new RuntimeBeanReference(name);
	}

	private RuntimeBeanReference registerMessagingTemplate(Element element, RuntimeBeanReference brokerChannel,
			RuntimeBeanReference messageConverter, ParserContext context, @Nullable Object source) {

		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		cargs.addIndexedArgumentValue(0, brokerChannel);
		RootBeanDefinition beanDef = new RootBeanDefinition(SimpMessagingTemplate.class, cargs, null);
		if (element.hasAttribute("user-destination-prefix")) {
			beanDef.getPropertyValues().add("userDestinationPrefix", element.getAttribute("user-destination-prefix"));
		}
		beanDef.getPropertyValues().add("messageConverter", messageConverter);
		String name = MESSAGING_TEMPLATE_BEAN_NAME;
		registerBeanDefByName(name, beanDef, context, source);
		return new RuntimeBeanReference(name);
	}

	private void registerAnnotationMethodMessageHandler(Element messageBrokerElement,
			RuntimeBeanReference inChannel, RuntimeBeanReference outChannel,
			RuntimeBeanReference converter, RuntimeBeanReference messagingTemplate,
			ParserContext context, @Nullable Object source) {

		ConstructorArgumentValues cargs = new ConstructorArgumentValues();
		cargs.addIndexedArgumentValue(0, inChannel);
		cargs.addIndexedArgumentValue(1, outChannel);
		cargs.addIndexedArgumentValue(2, messagingTemplate);

		MutablePropertyValues values = new MutablePropertyValues();
		String prefixAttribute = messageBrokerElement.getAttribute("application-destination-prefix");
		values.add("destinationPrefixes", Arrays.asList(StringUtils.tokenizeToStringArray(prefixAttribute, ",")));
		values.add("messageConverter", converter);

		RootBeanDefinition beanDef = new RootBeanDefinition(WebSocketAnnotationMethodMessageHandler.class, cargs, values);
		if (messageBrokerElement.hasAttribute("path-matcher")) {
			String pathMatcherRef = messageBrokerElement.getAttribute("path-matcher");
			beanDef.getPropertyValues().add("pathMatcher", new RuntimeBeanReference(pathMatcherRef));
		}

		RuntimeBeanReference validatorRef = getValidator(messageBrokerElement, source, context);
		if (validatorRef != null) {
			beanDef.getPropertyValues().add("validator", validatorRef);
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

	private @Nullable RuntimeBeanReference getValidator(
			Element messageBrokerElement, @Nullable Object source, ParserContext context) {

		if (messageBrokerElement.hasAttribute("validator")) {
			return new RuntimeBeanReference(messageBrokerElement.getAttribute("validator"));
		}
		else if (javaxValidationPresent) {
			RootBeanDefinition validatorDef = new RootBeanDefinition(
					"org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean");
			validatorDef.setSource(source);
			validatorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			String validatorName = context.getReaderContext().registerWithGeneratedName(validatorDef);
			context.registerComponent(new BeanComponentDefinition(validatorDef, validatorName));
			return new RuntimeBeanReference(validatorName);
		}
		else {
			return null;
		}
	}

	private ManagedList<Object> extractBeanSubElements(Element parentElement, ParserContext context) {
		ManagedList<Object> list = new ManagedList<>();
		list.setSource(context.extractSource(parentElement));
		for (Element beanElement : DomUtils.getChildElementsByTagName(parentElement, "bean", "ref")) {
			Object object = context.getDelegate().parsePropertySubElement(beanElement, null);
			list.add(object);
		}
		return list;
	}

	private RuntimeBeanReference registerUserDestResolver(Element brokerElem,
			RuntimeBeanReference userRegistry, ParserContext context, @Nullable Object source) {

		RootBeanDefinition beanDef = new RootBeanDefinition(DefaultUserDestinationResolver.class);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(0, userRegistry);
		if (brokerElem.hasAttribute("user-destination-prefix")) {
			beanDef.getPropertyValues().add("userDestinationPrefix", brokerElem.getAttribute("user-destination-prefix"));
		}
		return new RuntimeBeanReference(registerBeanDef(beanDef, context, source));
	}

	private RuntimeBeanReference registerUserDestHandler(Element brokerElem,
			RuntimeBeanReference userRegistry, RuntimeBeanReference inChannel,
			RuntimeBeanReference brokerChannel, ParserContext context, @Nullable Object source) {

		Object userDestResolver = registerUserDestResolver(brokerElem, userRegistry, context, source);

		RootBeanDefinition beanDef = new RootBeanDefinition(UserDestinationMessageHandler.class);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(0, inChannel);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(1, brokerChannel);
		beanDef.getConstructorArgumentValues().addIndexedArgumentValue(2, userDestResolver);

		Element relayElement = DomUtils.getChildElementByTagName(brokerElem, "stomp-broker-relay");
		if (relayElement != null && relayElement.hasAttribute("user-destination-broadcast")) {
			String destination = relayElement.getAttribute("user-destination-broadcast");
			beanDef.getPropertyValues().add("broadcastDestination", destination);
		}

		String beanName = registerBeanDef(beanDef, context, source);
		return new RuntimeBeanReference(beanName);
	}

	private void registerWebSocketMessageBrokerStats(RootBeanDefinition broker, RuntimeBeanReference inChannel,
			RuntimeBeanReference outChannel, ParserContext context, @Nullable Object source) {

		RootBeanDefinition beanDef = new RootBeanDefinition(WebSocketMessageBrokerStats.class);

		RuntimeBeanReference webSocketHandler = new RuntimeBeanReference(WEB_SOCKET_HANDLER_BEAN_NAME);
		beanDef.getPropertyValues().add("subProtocolWebSocketHandler", webSocketHandler);

		if (StompBrokerRelayMessageHandler.class == broker.getBeanClass()) {
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
		Object scheduler = WebSocketNamespaceUtils.registerScheduler(SCHEDULER_BEAN_NAME, context, source);
		beanDef.getPropertyValues().add("sockJsTaskScheduler", scheduler);

		registerBeanDefByName("webSocketMessageBrokerStats", beanDef, context, source);
	}

	private static String registerBeanDef(RootBeanDefinition beanDef, ParserContext context, @Nullable Object source) {
		String name = context.getReaderContext().generateBeanName(beanDef);
		registerBeanDefByName(name, beanDef, context, source);
		return name;
	}

	private static void registerBeanDefByName(
			String name, RootBeanDefinition beanDef, ParserContext context, @Nullable Object source) {

		beanDef.setSource(source);
		beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		context.getRegistry().registerBeanDefinition(name, beanDef);
		context.registerComponent(new BeanComponentDefinition(beanDef, name));
	}


	private static final class DecoratingFactoryBean implements FactoryBean<WebSocketHandler> {

		private final WebSocketHandler handler;

		private final List<WebSocketHandlerDecoratorFactory> factories;

		@SuppressWarnings("unused")
		public DecoratingFactoryBean(WebSocketHandler handler, List<WebSocketHandlerDecoratorFactory> factories) {
			this.handler = handler;
			this.factories = factories;
		}

		@Override
		public WebSocketHandler getObject() {
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
