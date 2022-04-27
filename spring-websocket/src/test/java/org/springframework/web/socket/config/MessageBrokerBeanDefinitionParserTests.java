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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.handler.invocation.HandlerMethodReturnValueHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.broker.DefaultSubscriptionRegistry;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.user.MultiServerUserRegistry;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.messaging.simp.user.UserRegistryMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ImmutableMessageChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.LoggingWebSocketHandlerDecorator;
import org.springframework.web.socket.handler.TestWebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.OriginHandshakeInterceptor;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.sockjs.transport.TransportType;
import org.springframework.web.socket.sockjs.transport.handler.DefaultSockJsService;
import org.springframework.web.socket.sockjs.transport.handler.WebSocketTransportHandler;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

/**
 * Test fixture for {@link MessageBrokerBeanDefinitionParser}.
 * Also see test configuration files websocket-config-broker-*.xml.
 *
 * @author Brian Clozel
 * @author Artem Bilan
 * @author Rossen Stoyanchev
 */
public class MessageBrokerBeanDefinitionParserTests {

	private final GenericWebApplicationContext appContext = new GenericWebApplicationContext();


	@Test
	public void simpleBroker() throws Exception {
		loadBeanDefinitions("websocket-config-broker-simple.xml");

		HandlerMapping hm = this.appContext.getBean(HandlerMapping.class);
		assertThat(hm).isInstanceOf(SimpleUrlHandlerMapping.class);
		SimpleUrlHandlerMapping suhm = (SimpleUrlHandlerMapping) hm;
		assertThat(suhm.getUrlMap()).hasSize(4);

		HttpRequestHandler httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/foo");
		assertThat(httpRequestHandler).isNotNull();
		assertThat(httpRequestHandler).isInstanceOf(WebSocketHttpRequestHandler.class);

		WebSocketHttpRequestHandler wsHttpRequestHandler = (WebSocketHttpRequestHandler) httpRequestHandler;
		HandshakeHandler handshakeHandler = wsHttpRequestHandler.getHandshakeHandler();
		assertThat(handshakeHandler).isNotNull();
		assertThat(handshakeHandler instanceof TestHandshakeHandler).isTrue();
		List<HandshakeInterceptor> interceptors = wsHttpRequestHandler.getHandshakeInterceptors();
		assertThat(interceptors).extracting("class").containsExactly(FooTestInterceptor.class,
				BarTestInterceptor.class, OriginHandshakeInterceptor.class);

		WebSocketSession session = new TestWebSocketSession("id");
		wsHttpRequestHandler.getWebSocketHandler().afterConnectionEstablished(session);
		assertThat(session.getAttributes().get("decorated")).asInstanceOf(BOOLEAN).isTrue();

		WebSocketHandler wsHandler = wsHttpRequestHandler.getWebSocketHandler();
		assertThat(wsHandler).isInstanceOf(ExceptionWebSocketHandlerDecorator.class);
		wsHandler = ((ExceptionWebSocketHandlerDecorator) wsHandler).getDelegate();
		assertThat(wsHandler).isInstanceOf(LoggingWebSocketHandlerDecorator.class);
		wsHandler = ((LoggingWebSocketHandlerDecorator) wsHandler).getDelegate();
		assertThat(wsHandler).isInstanceOf(TestWebSocketHandlerDecorator.class);
		wsHandler = ((TestWebSocketHandlerDecorator) wsHandler).getDelegate();
		assertThat(wsHandler).isInstanceOf(SubProtocolWebSocketHandler.class);
		assertThat(this.appContext.getBean(MessageBrokerBeanDefinitionParser.WEB_SOCKET_HANDLER_BEAN_NAME)).isSameAs(wsHandler);

		SubProtocolWebSocketHandler subProtocolWsHandler = (SubProtocolWebSocketHandler) wsHandler;
		assertThat(subProtocolWsHandler.getSubProtocols()).isEqualTo(Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp"));
		assertThat(subProtocolWsHandler.getSendTimeLimit()).isEqualTo(25 * 1000);
		assertThat(subProtocolWsHandler.getSendBufferSizeLimit()).isEqualTo(1024 * 1024);
		assertThat(subProtocolWsHandler.getTimeToFirstMessage()).isEqualTo(30 * 1000);

		Map<String, SubProtocolHandler> handlerMap = subProtocolWsHandler.getProtocolHandlerMap();
		StompSubProtocolHandler stompHandler = (StompSubProtocolHandler) handlerMap.get("v12.stomp");
		assertThat(stompHandler).isNotNull();
		assertThat(stompHandler.getMessageSizeLimit()).isEqualTo(128 * 1024);
		assertThat(stompHandler.getErrorHandler()).isNotNull();
		assertThat(stompHandler.getErrorHandler().getClass()).isEqualTo(TestStompErrorHandler.class);

		assertThat(new DirectFieldAccessor(stompHandler).getPropertyValue("eventPublisher")).isNotNull();

		httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/test/**");
		assertThat(httpRequestHandler).isNotNull();
		assertThat(httpRequestHandler).isInstanceOf(SockJsHttpRequestHandler.class);

		SockJsHttpRequestHandler sockJsHttpRequestHandler = (SockJsHttpRequestHandler) httpRequestHandler;
		wsHandler = unwrapWebSocketHandler(sockJsHttpRequestHandler.getWebSocketHandler());
		assertThat(wsHandler).isNotNull();
		assertThat(wsHandler).isInstanceOf(SubProtocolWebSocketHandler.class);
		assertThat(sockJsHttpRequestHandler.getSockJsService()).isNotNull();
		assertThat(sockJsHttpRequestHandler.getSockJsService()).isInstanceOf(DefaultSockJsService.class);

		DefaultSockJsService defaultSockJsService = (DefaultSockJsService) sockJsHttpRequestHandler.getSockJsService();
		WebSocketTransportHandler wsTransportHandler = (WebSocketTransportHandler) defaultSockJsService
				.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertThat(wsTransportHandler.getHandshakeHandler()).isNotNull();
		assertThat(wsTransportHandler.getHandshakeHandler()).isInstanceOf(TestHandshakeHandler.class);
		assertThat(defaultSockJsService.shouldSuppressCors()).isFalse();

		ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) defaultSockJsService.getTaskScheduler();
		ScheduledThreadPoolExecutor executor = scheduler.getScheduledThreadPoolExecutor();
		assertThat(executor.getCorePoolSize()).isEqualTo(Runtime.getRuntime().availableProcessors());
		assertThat(executor.getRemoveOnCancelPolicy()).isTrue();

		interceptors = defaultSockJsService.getHandshakeInterceptors();
		assertThat(interceptors).extracting("class").containsExactly(FooTestInterceptor.class,
				BarTestInterceptor.class, OriginHandshakeInterceptor.class);
		assertThat(defaultSockJsService.getAllowedOrigins()).containsExactly("https://mydomain3.com", "https://mydomain4.com");
		assertThat(defaultSockJsService.getAllowedOriginPatterns()).containsExactly("https://*.mydomain.com");

		SimpUserRegistry userRegistry = this.appContext.getBean(SimpUserRegistry.class);
		assertThat(userRegistry).isNotNull();
		assertThat(userRegistry.getClass()).isEqualTo(DefaultSimpUserRegistry.class);

		UserDestinationResolver userDestResolver = this.appContext.getBean(UserDestinationResolver.class);
		assertThat(userDestResolver).isNotNull();
		assertThat(userDestResolver).isInstanceOf(DefaultUserDestinationResolver.class);
		DefaultUserDestinationResolver defaultUserDestResolver = (DefaultUserDestinationResolver) userDestResolver;
		assertThat(defaultUserDestResolver.getDestinationPrefix()).isEqualTo("/personal/");

		UserDestinationMessageHandler userDestHandler = this.appContext.getBean(UserDestinationMessageHandler.class);
		assertThat(userDestHandler).isNotNull();

		SimpleBrokerMessageHandler brokerMessageHandler = this.appContext.getBean(SimpleBrokerMessageHandler.class);
		assertThat(brokerMessageHandler).isNotNull();
		Collection<String> prefixes = brokerMessageHandler.getDestinationPrefixes();
		assertThat(new ArrayList<>(prefixes)).isEqualTo(Arrays.asList("/topic", "/queue"));
		DefaultSubscriptionRegistry registry = (DefaultSubscriptionRegistry) brokerMessageHandler.getSubscriptionRegistry();
		assertThat(registry.getSelectorHeaderName()).isEqualTo("my-selector");
		assertThat(brokerMessageHandler.getTaskScheduler()).isNotNull();
		assertThat(brokerMessageHandler.getHeartbeatValue()).isEqualTo(new long[] {15000, 15000});
		assertThat(brokerMessageHandler.isPreservePublishOrder()).isTrue();

		List<Class<? extends MessageHandler>> subscriberTypes = Arrays.asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, SimpleBrokerMessageHandler.class);
		testChannel("clientInboundChannel", subscriberTypes, 2);
		testExecutor("clientInboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Collections.singletonList(SubProtocolWebSocketHandler.class);
		testChannel("clientOutboundChannel", subscriberTypes, 2);
		testExecutor("clientOutboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.asList(SimpleBrokerMessageHandler.class, UserDestinationMessageHandler.class);
		testChannel("brokerChannel", subscriberTypes, 1);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				this.appContext.getBean("brokerChannelExecutor", ThreadPoolTaskExecutor.class));

		assertThat(this.appContext.getBean("webSocketScopeConfigurer", CustomScopeConfigurer.class)).isNotNull();

		DirectFieldAccessor accessor = new DirectFieldAccessor(registry);
		Object pathMatcher = accessor.getPropertyValue("pathMatcher");
		String pathSeparator = (String) new DirectFieldAccessor(pathMatcher).getPropertyValue("pathSeparator");
		assertThat(pathSeparator).isEqualTo(".");
	}

	@Test
	public void stompBrokerRelay() {
		loadBeanDefinitions("websocket-config-broker-relay.xml");

		HandlerMapping hm = this.appContext.getBean(HandlerMapping.class);
		assertThat(hm).isNotNull();
		assertThat(hm).isInstanceOf(SimpleUrlHandlerMapping.class);

		SimpleUrlHandlerMapping suhm = (SimpleUrlHandlerMapping) hm;
		assertThat(suhm.getUrlMap()).hasSize(1);
		assertThat(suhm.getOrder()).isEqualTo(2);

		HttpRequestHandler httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/foo/**");
		assertThat(httpRequestHandler).isNotNull();
		assertThat(httpRequestHandler).isInstanceOf(SockJsHttpRequestHandler.class);
		SockJsHttpRequestHandler sockJsHttpRequestHandler = (SockJsHttpRequestHandler) httpRequestHandler;
		WebSocketHandler wsHandler = unwrapWebSocketHandler(sockJsHttpRequestHandler.getWebSocketHandler());
		assertThat(wsHandler).isNotNull();
		assertThat(wsHandler).isInstanceOf(SubProtocolWebSocketHandler.class);
		assertThat(sockJsHttpRequestHandler.getSockJsService()).isNotNull();

		UserDestinationResolver userDestResolver = this.appContext.getBean(UserDestinationResolver.class);
		assertThat(userDestResolver).isNotNull();
		assertThat(userDestResolver).isInstanceOf(DefaultUserDestinationResolver.class);
		DefaultUserDestinationResolver defaultUserDestResolver = (DefaultUserDestinationResolver) userDestResolver;
		assertThat(defaultUserDestResolver.getDestinationPrefix()).isEqualTo("/user/");

		StompBrokerRelayMessageHandler messageBroker = this.appContext.getBean(StompBrokerRelayMessageHandler.class);
		assertThat(messageBroker).isNotNull();
		assertThat(messageBroker.getClientLogin()).isEqualTo("clientlogin");
		assertThat(messageBroker.getClientPasscode()).isEqualTo("clientpass");
		assertThat(messageBroker.getSystemLogin()).isEqualTo("syslogin");
		assertThat(messageBroker.getSystemPasscode()).isEqualTo("syspass");
		assertThat(messageBroker.getRelayHost()).isEqualTo("relayhost");
		assertThat(messageBroker.getRelayPort()).isEqualTo(1234);
		assertThat(messageBroker.getVirtualHost()).isEqualTo("spring.io");
		assertThat(messageBroker.getSystemHeartbeatReceiveInterval()).isEqualTo(5000);
		assertThat(messageBroker.getSystemHeartbeatSendInterval()).isEqualTo(5000);
		assertThat(messageBroker.getDestinationPrefixes()).containsExactlyInAnyOrder("/topic","/queue");
		assertThat(messageBroker.isPreservePublishOrder()).isTrue();

		List<Class<? extends MessageHandler>> subscriberTypes = Arrays.asList(SimpAnnotationMethodMessageHandler.class,
				UserDestinationMessageHandler.class, StompBrokerRelayMessageHandler.class);
		testChannel("clientInboundChannel", subscriberTypes, 2);
		testExecutor("clientInboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Collections.singletonList(SubProtocolWebSocketHandler.class);
		testChannel("clientOutboundChannel", subscriberTypes, 2);
		testExecutor("clientOutboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.asList(StompBrokerRelayMessageHandler.class, UserDestinationMessageHandler.class);
		testChannel("brokerChannel", subscriberTypes, 1);
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				this.appContext.getBean("brokerChannelExecutor", ThreadPoolTaskExecutor.class));

		String destination = "/topic/unresolved-user-destination";
		UserDestinationMessageHandler userDestHandler = this.appContext.getBean(UserDestinationMessageHandler.class);
		assertThat(userDestHandler.getBroadcastDestination()).isEqualTo(destination);
		assertThat(messageBroker.getSystemSubscriptions()).isNotNull();
		assertThat(messageBroker.getSystemSubscriptions().get(destination)).isSameAs(userDestHandler);

		destination = "/topic/simp-user-registry";
		UserRegistryMessageHandler userRegistryHandler = this.appContext.getBean(UserRegistryMessageHandler.class);
		assertThat(userRegistryHandler.getBroadcastDestination()).isEqualTo(destination);
		assertThat(messageBroker.getSystemSubscriptions()).isNotNull();
		assertThat(messageBroker.getSystemSubscriptions().get(destination)).isSameAs(userRegistryHandler);

		SimpUserRegistry userRegistry = this.appContext.getBean(SimpUserRegistry.class);
		assertThat(userRegistry.getClass()).isEqualTo(MultiServerUserRegistry.class);

		String name = "webSocketMessageBrokerStats";
		WebSocketMessageBrokerStats stats = this.appContext.getBean(name, WebSocketMessageBrokerStats.class);
		String actual = stats.toString();
		String expected = "WebSocketSession\\[0 current WS\\(0\\)-HttpStream\\(0\\)-HttpPoll\\(0\\), " +
				"0 total, 0 closed abnormally \\(0 connect failure, 0 send limit, 0 transport error\\)], " +
				"stompSubProtocol\\[processed CONNECT\\(0\\)-CONNECTED\\(0\\)-DISCONNECT\\(0\\)], " +
				"stompBrokerRelay\\[0 sessions, relayhost:1234 \\(not available\\), " +
				"processed CONNECT\\(0\\)-CONNECTED\\(0\\)-DISCONNECT\\(0\\)], " +
				"inboundChannel\\[pool size = \\d, active threads = \\d, queued tasks = \\d, " +
				"completed tasks = \\d], " +
				"outboundChannel\\[pool size = \\d, active threads = \\d, queued tasks = \\d, " +
				"completed tasks = \\d], " +
				"sockJsScheduler\\[pool size = \\d, active threads = \\d, queued tasks = \\d, " +
				"completed tasks = \\d]";

		assertThat(actual.matches(expected)).as("\nExpected: " + expected.replace("\\", "") + "\n  Actual: " + actual).isTrue();
	}

	@Test
	public void annotationMethodMessageHandler() {
		loadBeanDefinitions("websocket-config-broker-simple.xml");

		SimpAnnotationMethodMessageHandler annotationMethodMessageHandler =
				this.appContext.getBean(SimpAnnotationMethodMessageHandler.class);

		assertThat(annotationMethodMessageHandler).isNotNull();
		MessageConverter messageConverter = annotationMethodMessageHandler.getMessageConverter();
		assertThat(messageConverter).isNotNull();
		assertThat(messageConverter instanceof CompositeMessageConverter).isTrue();

		String name = MessageBrokerBeanDefinitionParser.MESSAGE_CONVERTER_BEAN_NAME;
		CompositeMessageConverter compositeMessageConverter = this.appContext.getBean(name, CompositeMessageConverter.class);
		assertThat(compositeMessageConverter).isNotNull();

		name = MessageBrokerBeanDefinitionParser.MESSAGING_TEMPLATE_BEAN_NAME;
		SimpMessagingTemplate simpMessagingTemplate = this.appContext.getBean(name, SimpMessagingTemplate.class);
		assertThat(simpMessagingTemplate).isNotNull();
		assertThat(simpMessagingTemplate.getUserDestinationPrefix()).isEqualTo("/personal/");

		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertThat(converters).hasSize(3);
		assertThat(converters.get(0)).isInstanceOf(StringMessageConverter.class);
		assertThat(converters.get(1)).isInstanceOf(ByteArrayMessageConverter.class);
		assertThat(converters.get(2)).isInstanceOf(MappingJackson2MessageConverter.class);

		ContentTypeResolver resolver = ((MappingJackson2MessageConverter) converters.get(2)).getContentTypeResolver();
		assertThat(((DefaultContentTypeResolver) resolver).getDefaultMimeType()).isEqualTo(MimeTypeUtils.APPLICATION_JSON);

		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(annotationMethodMessageHandler);
		Object pathMatcher = handlerAccessor.getPropertyValue("pathMatcher");
		String pathSeparator = (String) new DirectFieldAccessor(pathMatcher).getPropertyValue("pathSeparator");
		assertThat(pathSeparator).isEqualTo(".");
	}

	@Test
	public void customChannels() {
		loadBeanDefinitions("websocket-config-broker-customchannels.xml");

		SimpAnnotationMethodMessageHandler annotationMethodMessageHandler =
				this.appContext.getBean(SimpAnnotationMethodMessageHandler.class);

		Validator validator = annotationMethodMessageHandler.getValidator();
		assertThat(validator).isNotNull();
		assertThat(validator).isSameAs(this.appContext.getBean("myValidator"));
		assertThat(validator).isInstanceOf(TestValidator.class);

		List<Class<? extends MessageHandler>> subscriberTypes = Arrays.asList(SimpAnnotationMethodMessageHandler.class,
				UserDestinationMessageHandler.class, SimpleBrokerMessageHandler.class);

		testChannel("clientInboundChannel", subscriberTypes, 3);
		testExecutor("clientInboundChannel", 100, 200, 600);

		subscriberTypes = Collections.singletonList(SubProtocolWebSocketHandler.class);

		testChannel("clientOutboundChannel", subscriberTypes, 3);
		testExecutor("clientOutboundChannel", 101, 201, 601);

		subscriberTypes = Arrays.asList(SimpleBrokerMessageHandler.class, UserDestinationMessageHandler.class);

		testChannel("brokerChannel", subscriberTypes, 1);
		testExecutor("brokerChannel", 102, 202, 602);
	}

	@Test  // SPR-11623
	public void customChannelsWithDefaultExecutor() {
		loadBeanDefinitions("websocket-config-broker-customchannels-default-executor.xml");

		testExecutor("clientInboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);
		testExecutor("clientOutboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);
		assertThat(this.appContext.containsBean("brokerChannelExecutor")).isFalse();
	}

	@Test
	public void customArgumentAndReturnValueTypes() {
		loadBeanDefinitions("websocket-config-broker-custom-argument-and-return-value-types.xml");

		SimpAnnotationMethodMessageHandler handler = this.appContext.getBean(SimpAnnotationMethodMessageHandler.class);

		List<HandlerMethodArgumentResolver> customResolvers = handler.getCustomArgumentResolvers();
		assertThat(customResolvers.size()).isEqualTo(2);
		assertThat(handler.getArgumentResolvers().contains(customResolvers.get(0))).isTrue();
		assertThat(handler.getArgumentResolvers().contains(customResolvers.get(1))).isTrue();

		List<HandlerMethodReturnValueHandler> customHandlers = handler.getCustomReturnValueHandlers();
		assertThat(customHandlers.size()).isEqualTo(2);
		assertThat(handler.getReturnValueHandlers().contains(customHandlers.get(0))).isTrue();
		assertThat(handler.getReturnValueHandlers().contains(customHandlers.get(1))).isTrue();
	}

	@Test
	public void messageConverters() {
		loadBeanDefinitions("websocket-config-broker-converters.xml");

		CompositeMessageConverter compositeConverter = this.appContext.getBean(CompositeMessageConverter.class);
		assertThat(compositeConverter).isNotNull();

		assertThat(compositeConverter.getConverters().size()).isEqualTo(4);
		assertThat(compositeConverter.getConverters().iterator().next().getClass()).isEqualTo(StringMessageConverter.class);
	}

	@Test
	public void messageConvertersDefaultsOff() {
		loadBeanDefinitions("websocket-config-broker-converters-defaults-off.xml");

		CompositeMessageConverter compositeConverter = this.appContext.getBean(CompositeMessageConverter.class);
		assertThat(compositeConverter).isNotNull();

		assertThat(compositeConverter.getConverters().size()).isEqualTo(1);
		assertThat(compositeConverter.getConverters().iterator().next().getClass()).isEqualTo(StringMessageConverter.class);
	}


	private void testChannel(
			String channelName, List<Class<? extends  MessageHandler>> subscriberTypes, int interceptorCount) {

		AbstractSubscribableChannel channel = this.appContext.getBean(channelName, AbstractSubscribableChannel.class);
		for (Class<? extends  MessageHandler> subscriberType : subscriberTypes) {
			MessageHandler subscriber = this.appContext.getBean(subscriberType);
			assertThat(subscriber).as("No subscription for " + subscriberType).isNotNull();
			assertThat(channel.hasSubscription(subscriber)).isTrue();
		}
		List<ChannelInterceptor> interceptors = channel.getInterceptors();
		assertThat(interceptors.size()).isEqualTo(interceptorCount);
		assertThat(interceptors.get(interceptors.size() - 1).getClass()).isEqualTo(ImmutableMessageChannelInterceptor.class);
	}

	private void testExecutor(String channelName, int corePoolSize, int maxPoolSize, int keepAliveSeconds) {
		ThreadPoolTaskExecutor taskExecutor =
				this.appContext.getBean(channelName + "Executor", ThreadPoolTaskExecutor.class);
		assertThat(taskExecutor.getCorePoolSize()).isEqualTo(corePoolSize);
		assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(maxPoolSize);
		assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(keepAliveSeconds);
	}

	private void loadBeanDefinitions(String fileName) {
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.appContext);
		ClassPathResource resource = new ClassPathResource(fileName, MessageBrokerBeanDefinitionParserTests.class);
		reader.loadBeanDefinitions(resource);
		this.appContext.setServletContext(new MockServletContext());
		this.appContext.refresh();
	}

	private WebSocketHandler unwrapWebSocketHandler(WebSocketHandler handler) {
		return (handler instanceof WebSocketHandlerDecorator) ?
				((WebSocketHandlerDecorator) handler).getLastHandler() : handler;
	}

}


class CustomArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return false;
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
		return null;
	}
}


class CustomReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return false;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, Message<?> message) throws Exception {
	}
}


class TestWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

	@Override
	public WebSocketHandler decorate(WebSocketHandler handler) {
		return new TestWebSocketHandlerDecorator(handler);
	}
}


class TestWebSocketHandlerDecorator extends WebSocketHandlerDecorator {

	public TestWebSocketHandlerDecorator(WebSocketHandler delegate) {
		super(delegate);
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		session.getAttributes().put("decorated", true);
		super.afterConnectionEstablished(session);
	}
}


class TestStompErrorHandler extends StompSubProtocolErrorHandler {
}


class TestValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return false;
	}

	@Override
	public void validate(@Nullable Object target, Errors errors) {
	}
}
