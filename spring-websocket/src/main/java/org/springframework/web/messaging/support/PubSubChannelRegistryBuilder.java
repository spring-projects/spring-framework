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

package org.springframework.web.messaging.support;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.web.messaging.PubSubChannelRegistry;
import org.springframework.web.messaging.PubSubChannelRegistryAware;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class PubSubChannelRegistryBuilder {

	private SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientInputChannel;

	private SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientOutputChannel;

	private SubscribableChannel<Message<?>, MessageHandler<Message<?>>> messageBrokerChannel;

	private Set<MessageHandler<Message<?>>> messageHandlers = new HashSet<MessageHandler<Message<?>>>();


	public PubSubChannelRegistryBuilder(
			SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientOutputChannel,
			MessageHandler<Message<?>> clientGateway) {

		Assert.notNull(clientOutputChannel, "clientOutputChannel is required");
		Assert.notNull(clientGateway, "clientGateway is required");

		this.clientOutputChannel = clientOutputChannel;
		this.clientOutputChannel.subscribe(clientGateway);
		this.messageHandlers.add(clientGateway);
	}


	public static PubSubChannelRegistryBuilder clientGateway(
			SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientOutputChannel,
			MessageHandler<Message<?>> clientGateway) {

		return new PubSubChannelRegistryBuilder(clientOutputChannel, clientGateway);
	}


	public PubSubChannelRegistryBuilder clientMessageHandlers(
			SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientInputChannel,
			List<MessageHandler<Message<?>>> handlers) {

		Assert.notNull(clientInputChannel, "clientInputChannel is required");
		this.clientInputChannel = clientInputChannel;

		for (MessageHandler<Message<?>> handler : handlers) {
			this.clientInputChannel.subscribe(handler);
			this.messageHandlers.add(handler);
		}

		return this;
	}

	public PubSubChannelRegistryBuilder messageBrokerGateway(
			SubscribableChannel<Message<?>, MessageHandler<Message<?>>> messageBrokerChannel,
			MessageHandler<Message<?>> messageBrokerGateway) {

		Assert.notNull(messageBrokerChannel, "messageBrokerChannel is required");
		Assert.notNull(messageBrokerGateway, "messageBrokerGateway is required");

		this.messageBrokerChannel = messageBrokerChannel;
		this.messageBrokerChannel.subscribe(messageBrokerGateway);
		this.messageHandlers.add(messageBrokerGateway);

		return this;
	}

	public PubSubChannelRegistry build() {

		PubSubChannelRegistry registry = new PubSubChannelRegistry() {

			@Override
			public MessageChannel<Message<?>> getClientInputChannel() {
				return clientInputChannel;
			}

			@Override
			public MessageChannel<Message<?>> getClientOutputChannel() {
				return clientOutputChannel;
			}

			@Override
			public MessageChannel<Message<?>> getMessageBrokerChannel() {
				return messageBrokerChannel;
			}
		};

		for (MessageHandler<Message<?>> handler : this.messageHandlers) {
			if (handler instanceof PubSubChannelRegistryAware) {
				((PubSubChannelRegistryAware) handler).setPubSubChannelRegistry(registry);
			}
		}

		return registry;
	}

}
