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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.web.messaging.PubSubChannelRegistry;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AbstractPubSubChannelRegistry implements PubSubChannelRegistry, InitializingBean {

	private SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientInputChannel;

	private SubscribableChannel<Message<?>, MessageHandler<Message<?>>> clientOutputChannel;

	private SubscribableChannel<Message<?>, MessageHandler<Message<?>>> messageBrokerChannel;


	public void setClientInputChannel(SubscribableChannel<Message<?>, MessageHandler<Message<?>>> channel) {
		this.clientInputChannel = channel;
	}

	@Override
	public SubscribableChannel<Message<?>, MessageHandler<Message<?>>> getClientInputChannel() {
		return this.clientInputChannel;
	}

	@Override
	public SubscribableChannel<Message<?>, MessageHandler<Message<?>>> getClientOutputChannel() {
		return this.clientOutputChannel;
	}

	public void setClientOutputChannel(SubscribableChannel<Message<?>, MessageHandler<Message<?>>> channel) {
		this.clientOutputChannel = channel;
	}

	@Override
	public SubscribableChannel<Message<?>, MessageHandler<Message<?>>> getMessageBrokerChannel() {
		return this.messageBrokerChannel;
	}

	public void setMessageBrokerChannel(SubscribableChannel<Message<?>, MessageHandler<Message<?>>> channel) {
		this.messageBrokerChannel = channel;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.clientInputChannel, "clientInputChannel is required");
		Assert.notNull(this.clientOutputChannel, "clientOutputChannel is required");
		Assert.notNull(this.messageBrokerChannel, "messageBrokerChannel is required");
	}

}
