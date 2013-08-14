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

package org.springframework.messaging.simp.stomp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import reactor.core.Environment;
import reactor.function.Consumer;
import reactor.tcp.TcpConnection;
import reactor.tcp.TcpServer;
import reactor.tcp.encoding.DelimitedCodec;
import reactor.tcp.encoding.StandardCodecs;
import reactor.tcp.netty.NettyTcpServer;
import reactor.tcp.spec.TcpServerSpec;

/**
 * @author Andy Wilkinson
 */
class TestStompBroker implements SmartLifecycle {

	private final StompMessageConverter messageConverter = new StompMessageConverter();

	private final List<Message<?>> messages = new ArrayList<Message<?>>();

	private final Object messageMonitor = new Object();

	private final Object subscriberMonitor = new Object();

	private final Map<String, Set<Subscription>> subscribers = new HashMap<String, Set<Subscription>>();

	private final AtomicLong messageIdCounter = new AtomicLong();

	private final int port;

	private volatile Environment environment;

	private volatile TcpServer tcpServer;

	private volatile boolean running;

	TestStompBroker(int port) {
		this.port = port;
	}

	public void start() {
		this.environment = new Environment();

		this.tcpServer = new TcpServerSpec<String, String>(NettyTcpServer.class)
				.env(this.environment)
				.codec(new DelimitedCodec<String, String>((byte) 0, true, StandardCodecs.STRING_CODEC))
				.listen(port)
				.consume(new Consumer<TcpConnection<String, String>>() {

					@Override
					public void accept(final TcpConnection<String, String> connection) {
						connection.consume(new Consumer<String>() {
							@Override
							public void accept(String stompFrame) {
								if (!StringUtils.isEmpty(stompFrame)) {
									handleMessage(messageConverter.toMessage(stompFrame), connection);
								}
							}
						});
					}
				})
				.get();

		this.tcpServer.start();
		this.running = true;
	}

	public void stop() {
		try {
			this.tcpServer.shutdown().await();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		this.running = false;
	}

	private void handleMessage(Message<?> message, TcpConnection<String, String> connection) {
		StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
		if (headers.getCommand() == StompCommand.CONNECT) {
			StompHeaderAccessor responseHeaders = StompHeaderAccessor.create(StompCommand.CONNECTED);
			MessageBuilder<byte[]> response = MessageBuilder.withPayloadAndHeaders(new byte[0], responseHeaders);
			connection.send(new String(messageConverter.fromMessage(response.build())));
		}
		else if (headers.getCommand() == StompCommand.SUBSCRIBE) {
			String destination = headers.getDestination();
			synchronized (this.subscriberMonitor) {
				Set<Subscription> subscribers = this.subscribers.get(destination);
				if (subscribers == null) {
					subscribers = new HashSet<Subscription>();
					this.subscribers.put(destination, subscribers);
				}
				String subscriptionId = headers.getFirstNativeHeader(StompHeaderAccessor.STOMP_ID_HEADER);
				subscribers.add(new Subscription(subscriptionId, connection));
			}
		}
		else if (headers.getCommand() == StompCommand.SEND) {
			String destination = headers.getDestination();
			synchronized (this.subscriberMonitor) {
				Set<Subscription> subscriptions = this.subscribers.get(destination);
				if (subscriptions != null) {
					for (Subscription subscription: subscriptions) {
						StompHeaderAccessor outboundHeaders = StompHeaderAccessor.create(StompCommand.MESSAGE);
						outboundHeaders.setSubscriptionId(subscription.subscriptionId);
						outboundHeaders.setMessageId(Long.toString(messageIdCounter.incrementAndGet()));
						Message<?> outbound =
								MessageBuilder.withPayloadAndHeaders(message.getPayload(), outboundHeaders).build();
						subscription.tcpConnection.send(new String(this.messageConverter.fromMessage(outbound)));
					}
				}
			}
		}
		addMessage(message);
	}

	private void addMessage(Message<?> message) {
		synchronized (this.messageMonitor) {
			this.messages.add(message);
			this.messageMonitor.notifyAll();
		}
	}

	public List<Message<?>> awaitMessages(int messageCount) throws InterruptedException {
		synchronized (this.messageMonitor) {
			while (this.messages.size() < messageCount) {
				this.messageMonitor.wait();
			}
			return this.messages;
		}
	}

	private static final class Subscription {

		private final String subscriptionId;

		private final TcpConnection<String, String> tcpConnection;

		public Subscription(String subscriptionId, TcpConnection<String, String> tcpConnection) {
			this.subscriptionId = subscriptionId;
			this.tcpConnection = tcpConnection;
		}

	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		return Integer.MIN_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}
}
