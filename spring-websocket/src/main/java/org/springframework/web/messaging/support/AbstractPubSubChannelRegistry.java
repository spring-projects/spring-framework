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
public class AbstractPubSubChannelRegistry<M extends Message<?>, H extends MessageHandler<M>> implements PubSubChannelRegistry<M, H>, InitializingBean {

	private SubscribableChannel<M, H> clientInputChannel;

	private SubscribableChannel<M, H> clientOutputChannel;

	private SubscribableChannel<M, H> messageBrokerChannel;


	@Override
	public SubscribableChannel<M, H> getClientInputChannel() {
		return this.clientInputChannel;
	}

	public void setClientInputChannel(SubscribableChannel<M, H> channel) {
		this.clientInputChannel = channel;
	}

	@Override
	public SubscribableChannel<M, H> getClientOutputChannel() {
		return this.clientOutputChannel;
	}

	public void setClientOutputChannel(SubscribableChannel<M, H> channel) {
		this.clientOutputChannel = channel;
	}

	@Override
	public SubscribableChannel<M, H> getMessageBrokerChannel() {
		return this.messageBrokerChannel;
	}

	public void setMessageBrokerChannel(SubscribableChannel<M, H> channel) {
		this.messageBrokerChannel = channel;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.clientInputChannel, "clientInputChannel is required");
		Assert.notNull(this.clientOutputChannel, "clientOutputChannel is required");
		Assert.notNull(this.messageBrokerChannel, "messageBrokerChannel is required");
	}

}
