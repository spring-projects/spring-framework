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

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.user.DefaultUserDestinationResolver;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationMessageHandler;
import org.springframework.messaging.simp.user.UserDestinationResolver;
import org.springframework.messaging.simp.user.UserSessionRegistry;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;

import static org.junit.Assert.*;

/**
 * Test fixture for MessageBrokerBeanDefinitionParser.
 * See test configuration files websocket-config-broker-*.xml.
 *
 * @author Brian Clozel
 */
public class MessageBrokerBeanDefinitionParserTests {

	private GenericWebApplicationContext appContext;

	@Before
	public void setup() {
		this.appContext = new GenericWebApplicationContext();
	}

	@Test
	public void simpleBroker() {
		loadBeanDefinitions("websocket-config-broker-simple.xml");

		HandlerMapping hm = this.appContext.getBean(HandlerMapping.class);
		assertNotNull(hm);
		assertThat(hm, Matchers.instanceOf(SimpleUrlHandlerMapping.class));

		SimpleUrlHandlerMapping suhm = (SimpleUrlHandlerMapping) hm;
		assertThat(suhm.getUrlMap().keySet(), Matchers.hasSize(4));
		assertThat(suhm.getUrlMap().values(), Matchers.hasSize(4));

		HttpRequestHandler httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/foo");
		assertNotNull(httpRequestHandler);
		assertThat(httpRequestHandler, Matchers.instanceOf(WebSocketHttpRequestHandler.class));
		WebSocketHttpRequestHandler wsHttpRequestHandler = (WebSocketHttpRequestHandler) httpRequestHandler;
		WebSocketHandler wsHandler = unwrapWebSocketHandler(wsHttpRequestHandler.getWebSocketHandler());
		assertNotNull(wsHandler);
		assertThat(wsHandler, Matchers.instanceOf(SubProtocolWebSocketHandler.class));
		SubProtocolWebSocketHandler subProtocolWsHandler = (SubProtocolWebSocketHandler) wsHandler;
		assertEquals(Arrays.asList("v10.stomp", "v11.stomp", "v12.stomp"), subProtocolWsHandler.getSubProtocols());

		StompSubProtocolHandler stompHandler =
				(StompSubProtocolHandler) subProtocolWsHandler.getProtocolHandlerMap().get("v12.stomp");
		assertNotNull(stompHandler);

		httpRequestHandler = (HttpRequestHandler) suhm.getUrlMap().get("/test/**");
		assertNotNull(httpRequestHandler);
		assertThat(httpRequestHandler, Matchers.instanceOf(SockJsHttpRequestHandler.class));
		SockJsHttpRequestHandler sockJsHttpRequestHandler = (SockJsHttpRequestHandler) httpRequestHandler;
		wsHandler = unwrapWebSocketHandler(sockJsHttpRequestHandler.getWebSocketHandler());
		assertNotNull(wsHandler);
		assertThat(wsHandler, Matchers.instanceOf(SubProtocolWebSocketHandler.class));
		assertNotNull(sockJsHttpRequestHandler.getSockJsService());

		UserSessionRegistry userSessionRegistry = this.appContext.getBean(UserSessionRegistry.class);
		assertNotNull(userSessionRegistry);

		UserDestinationResolver userDestResolver = this.appContext.getBean(UserDestinationResolver.class);
		assertNotNull(userDestResolver);
		assertThat(userDestResolver, Matchers.instanceOf(DefaultUserDestinationResolver.class));
		DefaultUserDestinationResolver defaultUserDestResolver = (DefaultUserDestinationResolver) userDestResolver;
		assertEquals("/personal/", defaultUserDestResolver.getDestinationPrefix());

		assertSame(stompHandler.getUserSessionRegistry(), defaultUserDestResolver.getUserSessionRegistry());

		UserDestinationMessageHandler userDestHandler = this.appContext.getBean(UserDestinationMessageHandler.class);
		assertNotNull(userDestHandler);

		List<Class<? extends MessageHandler>> subscriberTypes =
				Arrays.<Class<? extends MessageHandler>>asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, SimpleBrokerMessageHandler.class);
		testChannel("clientInboundChannel", subscriberTypes, 0);
		testExecutor("clientInboundChannel", 1, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(SubProtocolWebSocketHandler.class);
		testChannel("clientOutboundChannel", subscriberTypes, 0);
		testExecutor("clientOutboundChannel", 1, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(
				SimpleBrokerMessageHandler.class, UserDestinationMessageHandler.class);
		testChannel("brokerChannel", subscriberTypes, 0);
		try {
			this.appContext.getBean("brokerChannelExecutor", ThreadPoolTaskExecutor.class);
			fail("expected exception");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
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
		assertEquals("login", messageBroker.getSystemLogin());
		assertEquals("pass", messageBroker.getSystemPasscode());
		assertEquals("relayhost", messageBroker.getRelayHost());
		assertEquals(1234, messageBroker.getRelayPort());
		assertEquals("spring.io", messageBroker.getVirtualHost());
		assertEquals(5000, messageBroker.getSystemHeartbeatReceiveInterval());
		assertEquals(5000, messageBroker.getSystemHeartbeatSendInterval());
		assertThat(messageBroker.getDestinationPrefixes(), Matchers.containsInAnyOrder("/topic","/queue"));

		List<Class<? extends MessageHandler>> subscriberTypes =
				Arrays.<Class<? extends MessageHandler>>asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, StompBrokerRelayMessageHandler.class);
		testChannel("clientInboundChannel", subscriberTypes, 0);
		testExecutor("clientInboundChannel", 1, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(SubProtocolWebSocketHandler.class);
		testChannel("clientOutboundChannel", subscriberTypes, 0);
		testExecutor("clientOutboundChannel", 1, Integer.MAX_VALUE, 60);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(
				StompBrokerRelayMessageHandler.class, UserDestinationMessageHandler.class);
		testChannel("brokerChannel", subscriberTypes, 0);
		try {
			this.appContext.getBean("brokerChannelExecutor", ThreadPoolTaskExecutor.class);
			fail("expected exception");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
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
		assertEquals("/personal", simpMessagingTemplate.getUserDestinationPrefix());

	}

	@Test
	public void customChannels() {
		loadBeanDefinitions("websocket-config-broker-customchannels.xml");

		List<Class<? extends MessageHandler>> subscriberTypes =
				Arrays.<Class<? extends MessageHandler>>asList(SimpAnnotationMethodMessageHandler.class,
						UserDestinationMessageHandler.class, SimpleBrokerMessageHandler.class);

		testChannel("clientInboundChannel", subscriberTypes, 1);
		testExecutor("clientInboundChannel", 100, 200, 600);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(SubProtocolWebSocketHandler.class);

		testChannel("clientOutboundChannel", subscriberTypes, 2);
		testExecutor("clientOutboundChannel", 101, 201, 601);

		subscriberTypes = Arrays.<Class<? extends MessageHandler>>asList(SimpleBrokerMessageHandler.class,
				UserDestinationMessageHandler.class);

		testChannel("brokerChannel", subscriberTypes, 0);
		testExecutor("brokerChannel", 102, 202, 602);
	}

	private void testChannel(String channelName, List<Class<? extends  MessageHandler>> subscriberTypes,
			int interceptorCount) {

		AbstractSubscribableChannel channel = this.appContext.getBean(channelName, AbstractSubscribableChannel.class);

		for (Class<? extends  MessageHandler> subscriberType : subscriberTypes) {
			MessageHandler subscriber = this.appContext.getBean(subscriberType);
			assertNotNull("No subsription for " + subscriberType, subscriber);
			assertTrue(channel.hasSubscription(subscriber));
		}

		assertEquals(interceptorCount, channel.getInterceptors().size());
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
		this.appContext.refresh();
	}

	private WebSocketHandler unwrapWebSocketHandler(WebSocketHandler handler) {
		return (handler instanceof WebSocketHandlerDecorator) ?
				((WebSocketHandlerDecorator) handler).getLastHandler() : handler;
	}

}