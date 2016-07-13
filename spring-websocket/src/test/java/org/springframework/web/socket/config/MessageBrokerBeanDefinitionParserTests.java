/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
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
import org.springframework.mock.web.test.MockServletContext;
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Test fixture for MessageBrokerBeanDefinitionParser.
 * See test configuration files websocket-config-broker-*.xml.
 *
 * @author Brian Clozel
 * @author Artem Bilan
 * @author Rossen Stoyanchev
 */
public class MessageBrokerBeanDefinitionParserTests {

	private final GenericWebApplicationContext appContext = new GenericWebApplicationContext();


	@Test
	@SuppressWarnings("unchecked")
	public void simpleBroker() throws Exception {
		loadBeanDefinitions("websocket-config-broker-simple.xml");

		HandlerMapping hm = this.appContext.getBean(HandlerMapping.class);
		assertThat(hm, Matchers.instanceOf(SimpleUrlHandlerMapping.class));
		SimpleUrlHandlerMapping suhm = (SimpleUrlHandlerMapping) hm;
		assertThat(suhm.getUrlMap().keySet(), Matchers.hasSize(4));
		assertThat(suhm.getUrlMap().values(), Matchers.hasSize(4));

		HttpRequestHandler httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/foo");
		assertNotNull(httpRequestHandler);
		assertThat(httpRequestHandler, Matchers.instanceOf(WebSocketHttpRequestHandler.class));

		WebSocketHttpRequestHandler wsHttpRequestHandler = (WebSocketHttpRequestHandler) httpRequestHandler;
		HandshakeHandler handshakeHandler = wsHttpRequestHandler.getHandshakeHandler();
		assertNotNull(handshakeHandler);
		assertTrue(handshakeHandler instanceof TestHandshakeHandler);
		List<HandshakeInterceptor> interceptors = wsHttpRequestHandler.getHandshakeInterceptors();
		assertThat(interceptors, contains(instanceOf(FooTestInterceptor.class),
				instanceOf(BarTestInterceptor.class), instanceOf(OriginHandshakeInterceptor.class)));

		WebSocketSession session = new TestWebSocketSession("id");
		wsHttpRequestHandler.getWebSocketHandler().afterConnectionEstablished(session);
		assertEquals(true, session.getAttributes().get("decorated"));

		WebSocketHandler wsHandler = wsHttpRequestHandler.getWebSocketHandler();
		assertThat(wsHandler, Matchers.instanceOf(ExceptionWebSocketHandlerDecorator.class));
		wsHandler = ((ExceptionWebSocketHandlerDecorator) wsHandler).getDelegate();
		assertThat(wsHandler, Matchers.instanceOf(LoggingWebSocketHandlerDecorator.class));
		wsHandler = ((LoggingWebSocketHandlerDecorator) wsHandler).getDelegate();
		assertThat(wsHandler, Matchers.instanceOf(TestWebSocketHandlerDecorator.class));
		wsHandler = ((TestWebSocketHandlerDecorator) wsHandler).getDelegate();
		assertThat(wsHandler, Matchers.instanceOf(SubProtocolWebSocketHandler.class));
		assertSame(wsHandler, this.appContext.getBean(MessageBrokerBeanDefinitionParser.WEB_SOCKET_HANDLER_BEAN_NAME));

		SubProtocolWebSocketHandler subProtocolWsHandler = (SubProtocolWebSocketHandler) wsHandler;
		assertEquals(Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp"), subProtocolWsHandler.getSubProtocols());
		assertEquals(25 * 1000, subProtocolWsHandler.getSendTimeLimit());
		assertEquals(1024 * 1024, subProtocolWsHandler.getSendBufferSizeLimit());

		Map<String, SubProtocolHandler> handlerMap = subProtocolWsHandler.getProtocolHandlerMap();
		StompSubProtocolHandler stompHandler = (StompSubProtocolHandler) handlerMap.get("v12.stomp");
		assertNotNull(stompHandler);
		assertEquals(128 * 1024, stompHandler.getMessageSizeLimit());
		assertNotNull(stompHandler.getErrorHandler());
		assertEquals(TestStompErrorHandler.class, stompHandler.getErrorHandler().getClass());

		assertNotNull(new DirectFieldAccessor(stompHandler).getPropertyValue("eventPublisher"));

		httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/test/**");
		assertNotNull(httpRequestHandler);
		assertThat(httpRequestHandler, Matchers.instanceOf(SockJsHttpRequestHandler.class));

		SockJsHttpRequestHandler sockJsHttpRequestHandler = (SockJsHttpRequestHandler) httpRequestHandler;
		wsHandler = unwrapWebSocketHandler(sockJsHttpRequestHandler.getWebSocketHandler());
		assertNotNull(wsHandler);
		assertThat(wsHandler, Matchers.instanceOf(SubProtocolWebSocketHandler.class));
		assertNotNull(sockJsHttpRequestHandler.getSockJsService());
		assertThat(sockJsHttpRequestHandler.getSockJsService(), Matchers.instanceOf(DefaultSockJsService.class));

		DefaultSockJsService defaultSockJsService = (DefaultSockJsService) sockJsHttpRequestHandler.getSockJsService();
		WebSocketTransportHandler wsTransportHandler = (WebSocketTransportHandler) defaultSockJsService
				.getTransportHandlers().get(TransportType.WEBSOCKET);
		assertNotNull(wsTransportHandler.getHandshakeHandler());
		assertThat(wsTransportHandler.getHandshakeHandler(), Matchers.instanceOf(TestHandshakeHandler.class));
		assertFalse(defaultSockJsService.shouldSuppressCors());

		ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) defaultSockJsService.getTaskScheduler();
		assertEquals(Runtime.getRuntime().availableProcessors(), scheduler.getScheduledThreadPoolExecutor().getCorePoolSize());
		assertTrue(scheduler.getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy());

		interceptors = defaultSockJsService.getHandshakeInterceptors();
		assertThat(interceptors, contains(instanceOf(FooTestInterceptor.class),
				instanceOf(BarTestInterceptor.class), instanceOf(OriginHandshakeInterceptor.class)));
		assertTrue(defaultSockJsService.getAllowedOrigins().contains("http://mydomain3.com"));
		assertTrue(defaultSockJsService.getAllowedOrigins().contains("http://mydomain4.com"));

		SimpUserRegistry userRegistry = this.appContext.getBean(SimpUserRegistry.class);
		assertNotNull(userRegistry);
		assertEquals(DefaultSimpUserRegistry.class, userRegistry.getClass());

		UserDestinationResolver userDestResolver = this.appContext.getBean(UserDestinationResolver.class);
		assertNotNull(userDestResolver);
		assertThat(userDestResolver, Matchers.instanceOf(DefaultUserDestinationResolver.class));
		DefaultUserDestinationResolver defaultUserDestResolver = (DefaultUserDestinationResolver) userDestResolver;
		assertEquals("/personal/", defaultUserDestResolver.getDestinationPrefix());

		UserDestinationMessageHandler userDestHandler = this.appContext.getBean(UserDestinationMessageHandler.class);
		assertNotNull(userDestHandler);

		SimpleBrokerMessageHandler brokerMessageHandler = this.appContext.getBean(SimpleBrokerMessageHandler.class);
		assertNotNull(brokerMessageHandler);
		Collection<String> prefixes = brokerMessageHandler.getDestinationPrefixes();
		assertEquals(Arrays.asList("/topic", "/queue"), new ArrayList<>(prefixes));
		assertNotNull(brokerMessageHandler.getTaskScheduler());
		assertArrayEquals(new long[] {15000, 15000}, brokerMessageHandler.getHeartbeatValue());

		List<Class<? extends MessageHandler>> subscriberTypes =
				Arrays.<Class<? extends MessageHandler>>asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, SimpleBrokerMessageHandler.class);
		testChannel("clientInboundChannel", subscriberTypes, 2);
		testExecutor("clientInboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Collections.singletonList(SubProtocolWebSocketHandler.class);
		testChannel("clientOutboundChannel", subscriberTypes, 1);
		testExecutor("clientOutboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(
				SimpleBrokerMessageHandler.class, UserDestinationMessageHandler.class);
		testChannel("brokerChannel", subscriberTypes, 1);
		try {
			this.appContext.getBean("brokerChannelExecutor", ThreadPoolTaskExecutor.class);
			fail("expected exception");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}

		assertNotNull(this.appContext.getBean("webSocketScopeConfigurer", CustomScopeConfigurer.class));

		DirectFieldAccessor subscriptionRegistryAccessor = new DirectFieldAccessor(brokerMessageHandler.getSubscriptionRegistry());
		String pathSeparator = (String) new DirectFieldAccessor(subscriptionRegistryAccessor.getPropertyValue("pathMatcher")).getPropertyValue("pathSeparator");
		assertEquals(".", pathSeparator);
	}

	@Test
	public void stompBrokerRelay() {
		loadBeanDefinitions("websocket-config-broker-relay.xml");

		HandlerMapping hm = this.appContext.getBean(HandlerMapping.class);
		assertNotNull(hm);
		assertThat(hm, Matchers.instanceOf(SimpleUrlHandlerMapping.class));

		SimpleUrlHandlerMapping suhm = (SimpleUrlHandlerMapping) hm;
		assertThat(suhm.getUrlMap().keySet(), Matchers.hasSize(1));
		assertThat(suhm.getUrlMap().values(), Matchers.hasSize(1));
		assertEquals(2, suhm.getOrder());

		HttpRequestHandler httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/foo/**");
		assertNotNull(httpRequestHandler);
		assertThat(httpRequestHandler, Matchers.instanceOf(SockJsHttpRequestHandler.class));
		SockJsHttpRequestHandler sockJsHttpRequestHandler = (SockJsHttpRequestHandler) httpRequestHandler;
		WebSocketHandler wsHandler = unwrapWebSocketHandler(sockJsHttpRequestHandler.getWebSocketHandler());
		assertNotNull(wsHandler);
		assertThat(wsHandler, Matchers.instanceOf(SubProtocolWebSocketHandler.class));
		assertNotNull(sockJsHttpRequestHandler.getSockJsService());

		UserDestinationResolver userDestResolver = this.appContext.getBean(UserDestinationResolver.class);
		assertNotNull(userDestResolver);
		assertThat(userDestResolver, Matchers.instanceOf(DefaultUserDestinationResolver.class));
		DefaultUserDestinationResolver defaultUserDestResolver = (DefaultUserDestinationResolver) userDestResolver;
		assertEquals("/user/", defaultUserDestResolver.getDestinationPrefix());

		StompBrokerRelayMessageHandler messageBroker = this.appContext.getBean(StompBrokerRelayMessageHandler.class);
		assertNotNull(messageBroker);
		assertEquals("clientlogin", messageBroker.getClientLogin());
		assertEquals("clientpass", messageBroker.getClientPasscode());
		assertEquals("syslogin", messageBroker.getSystemLogin());
		assertEquals("syspass", messageBroker.getSystemPasscode());
		assertEquals("relayhost", messageBroker.getRelayHost());
		assertEquals(1234, messageBroker.getRelayPort());
		assertEquals("spring.io", messageBroker.getVirtualHost());
		assertEquals(5000, messageBroker.getSystemHeartbeatReceiveInterval());
		assertEquals(5000, messageBroker.getSystemHeartbeatSendInterval());
		assertThat(messageBroker.getDestinationPrefixes(), Matchers.containsInAnyOrder("/topic","/queue"));

		List<Class<? extends MessageHandler>> subscriberTypes =
				Arrays.<Class<? extends MessageHandler>>asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, StompBrokerRelayMessageHandler.class);
		testChannel("clientInboundChannel", subscriberTypes, 2);
		testExecutor("clientInboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Collections.singletonList(SubProtocolWebSocketHandler.class);
		testChannel("clientOutboundChannel", subscriberTypes, 1);
		testExecutor("clientOutboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(
				StompBrokerRelayMessageHandler.class, UserDestinationMessageHandler.class);
		testChannel("brokerChannel", subscriberTypes, 1);
		try {
			this.appContext.getBean("brokerChannelExecutor", ThreadPoolTaskExecutor.class);
			fail("expected exception");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}

		String destination = "/topic/unresolved-user-destination";
		UserDestinationMessageHandler userDestHandler = this.appContext.getBean(UserDestinationMessageHandler.class);
		assertEquals(destination, userDestHandler.getBroadcastDestination());
		assertNotNull(messageBroker.getSystemSubscriptions());
		assertSame(userDestHandler, messageBroker.getSystemSubscriptions().get(destination));

		destination = "/topic/simp-user-registry";
		UserRegistryMessageHandler userRegistryHandler = this.appContext.getBean(UserRegistryMessageHandler.class);
		assertEquals(destination, userRegistryHandler.getBroadcastDestination());
		assertNotNull(messageBroker.getSystemSubscriptions());
		assertSame(userRegistryHandler, messageBroker.getSystemSubscriptions().get(destination));

		SimpUserRegistry userRegistry = this.appContext.getBean(SimpUserRegistry.class);
		assertEquals(MultiServerUserRegistry.class, userRegistry.getClass());

		String name = "webSocketMessageBrokerStats";
		WebSocketMessageBrokerStats stats = this.appContext.getBean(name, WebSocketMessageBrokerStats.class);
		String actual = stats.toString();
		String expected = "WebSocketSession\\[0 current WS\\(0\\)-HttpStream\\(0\\)-HttpPoll\\(0\\), " +
				"0 total, 0 closed abnormally \\(0 connect failure, 0 send limit, 0 transport error\\)\\], " +
				"stompSubProtocol\\[processed CONNECT\\(0\\)-CONNECTED\\(0\\)-DISCONNECT\\(0\\)\\], " +
				"stompBrokerRelay\\[0 sessions, relayhost:1234 \\(not available\\), processed CONNECT\\(0\\)-CONNECTED\\(0\\)-DISCONNECT\\(0\\)\\], " +
				"inboundChannel\\[pool size = \\d, active threads = \\d, queued tasks = \\d, completed tasks = \\d\\], " +
				"outboundChannelpool size = \\d, active threads = \\d, queued tasks = \\d, completed tasks = \\d\\], " +
				"sockJsScheduler\\[pool size = \\d, active threads = \\d, queued tasks = \\d, completed tasks = \\d\\]";

		assertTrue("\nExpected: " + expected.replace("\\", "") + "\n  Actual: " + actual, actual.matches(expected));
	}

	@Test
	public void annotationMethodMessageHandler() {
		loadBeanDefinitions("websocket-config-broker-simple.xml");

		SimpAnnotationMethodMessageHandler annotationMethodMessageHandler =
				this.appContext.getBean(SimpAnnotationMethodMessageHandler.class);

		assertNotNull(annotationMethodMessageHandler);
		MessageConverter messageConverter = annotationMethodMessageHandler.getMessageConverter();
		assertNotNull(messageConverter);
		assertTrue(messageConverter instanceof CompositeMessageConverter);

		CompositeMessageConverter compositeMessageConverter = this.appContext.getBean(CompositeMessageConverter.class);
		assertNotNull(compositeMessageConverter);

		SimpMessagingTemplate simpMessagingTemplate = this.appContext.getBean(SimpMessagingTemplate.class);
		assertNotNull(simpMessagingTemplate);
		assertEquals("/personal/", simpMessagingTemplate.getUserDestinationPrefix());

		List<MessageConverter> converters = compositeMessageConverter.getConverters();
		assertThat(converters.size(), Matchers.is(3));
		assertThat(converters.get(0), Matchers.instanceOf(StringMessageConverter.class));
		assertThat(converters.get(1), Matchers.instanceOf(ByteArrayMessageConverter.class));
		assertThat(converters.get(2), Matchers.instanceOf(MappingJackson2MessageConverter.class));

		ContentTypeResolver resolver = ((MappingJackson2MessageConverter) converters.get(2)).getContentTypeResolver();
		assertEquals(MimeTypeUtils.APPLICATION_JSON, ((DefaultContentTypeResolver) resolver).getDefaultMimeType());

		DirectFieldAccessor handlerAccessor = new DirectFieldAccessor(annotationMethodMessageHandler);
		String pathSeparator = (String) new DirectFieldAccessor(handlerAccessor.getPropertyValue("pathMatcher")).getPropertyValue("pathSeparator");
		assertEquals(".", pathSeparator);
	}

	@Test
	public void customChannels() {
		loadBeanDefinitions("websocket-config-broker-customchannels.xml");

		SimpAnnotationMethodMessageHandler annotationMethodMessageHandler =
				this.appContext.getBean(SimpAnnotationMethodMessageHandler.class);

		Validator validator = annotationMethodMessageHandler.getValidator();
		assertNotNull(validator);
		assertSame(this.appContext.getBean("myValidator"), validator);
		assertThat(validator, Matchers.instanceOf(TestValidator.class));

		List<Class<? extends MessageHandler>> subscriberTypes =
				Arrays.<Class<? extends MessageHandler>>asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, SimpleBrokerMessageHandler.class);

		testChannel("clientInboundChannel", subscriberTypes, 3);
		testExecutor("clientInboundChannel", 100, 200, 600);

		subscriberTypes = Collections.singletonList(SubProtocolWebSocketHandler.class);

		testChannel("clientOutboundChannel", subscriberTypes, 3);
		testExecutor("clientOutboundChannel", 101, 201, 601);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(SimpleBrokerMessageHandler.class,
				UserDestinationMessageHandler.class);

		testChannel("brokerChannel", subscriberTypes, 1);
		testExecutor("brokerChannel", 102, 202, 602);
	}

	// SPR-11623

	@Test
	public void customChannelsWithDefaultExecutor() {
		loadBeanDefinitions("websocket-config-broker-customchannels-default-executor.xml");

		testExecutor("clientInboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);
		testExecutor("clientOutboundChannel", Runtime.getRuntime().availableProcessors() * 2, Integer.MAX_VALUE, 60);
		assertFalse(this.appContext.containsBean("brokerChannelExecutor"));
	}

	@Test
	public void customArgumentAndReturnValueTypes() {
		loadBeanDefinitions("websocket-config-broker-custom-argument-and-return-value-types.xml");

		SimpAnnotationMethodMessageHandler handler = this.appContext.getBean(SimpAnnotationMethodMessageHandler.class);

		List<HandlerMethodArgumentResolver> customResolvers = handler.getCustomArgumentResolvers();
		assertEquals(2, customResolvers.size());
		assertTrue(handler.getArgumentResolvers().contains(customResolvers.get(0)));
		assertTrue(handler.getArgumentResolvers().contains(customResolvers.get(1)));

		List<HandlerMethodReturnValueHandler> customHandlers = handler.getCustomReturnValueHandlers();
		assertEquals(2, customHandlers.size());
		assertTrue(handler.getReturnValueHandlers().contains(customHandlers.get(0)));
		assertTrue(handler.getReturnValueHandlers().contains(customHandlers.get(1)));
	}

	@Test
	public void messageConverters() {
		loadBeanDefinitions("websocket-config-broker-converters.xml");

		CompositeMessageConverter compositeConverter = this.appContext.getBean(CompositeMessageConverter.class);
		assertNotNull(compositeConverter);

		assertEquals(4, compositeConverter.getConverters().size());
		assertEquals(StringMessageConverter.class, compositeConverter.getConverters().iterator().next().getClass());
	}

	@Test
	public void messageConvertersDefaultsOff() {
		loadBeanDefinitions("websocket-config-broker-converters-defaults-off.xml");

		CompositeMessageConverter compositeConverter = this.appContext.getBean(CompositeMessageConverter.class);
		assertNotNull(compositeConverter);

		assertEquals(1, compositeConverter.getConverters().size());
		assertEquals(StringMessageConverter.class, compositeConverter.getConverters().iterator().next().getClass());
	}


	private void testChannel(String channelName, List<Class<? extends  MessageHandler>> subscriberTypes,
			int interceptorCount) {

		AbstractSubscribableChannel channel = this.appContext.getBean(channelName, AbstractSubscribableChannel.class);

		for (Class<? extends  MessageHandler> subscriberType : subscriberTypes) {
			MessageHandler subscriber = this.appContext.getBean(subscriberType);
			assertNotNull("No subsription for " + subscriberType, subscriber);
			assertTrue(channel.hasSubscription(subscriber));
		}

		List<ChannelInterceptor> interceptors = channel.getInterceptors();
		assertEquals(interceptorCount, interceptors.size());
		assertEquals(ImmutableMessageChannelInterceptor.class, interceptors.get(interceptors.size()-1).getClass());
	}

	private void testExecutor(String channelName, int corePoolSize, int maxPoolSize, int keepAliveSeconds) {

		ThreadPoolTaskExecutor taskExecutor =
				this.appContext.getBean(channelName + "Executor", ThreadPoolTaskExecutor.class);

		assertEquals(corePoolSize, taskExecutor.getCorePoolSize());
		assertEquals(maxPoolSize, taskExecutor.getMaxPoolSize());
		assertEquals(keepAliveSeconds, taskExecutor.getKeepAliveSeconds());
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
	public void validate(Object target, Errors errors) { }
}
