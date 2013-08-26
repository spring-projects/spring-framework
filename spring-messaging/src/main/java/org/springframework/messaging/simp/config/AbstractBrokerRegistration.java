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

package org.springframework.messaging.simp.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.handler.AbstractBrokerMessageHandler;

import reactor.util.Assert;


/**
 * Base class for message broker registration classes.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractBrokerRegistration {

	private final MessageChannel webSocketReplyChannel;

	private final String[] destinationPrefixes;


	public AbstractBrokerRegistration(MessageChannel webSocketReplyChannel, String[] destinationPrefixes) {
		Assert.notNull(webSocketReplyChannel, "");
		this.webSocketReplyChannel = webSocketReplyChannel;
		this.destinationPrefixes = destinationPrefixes;
	}


	protected MessageChannel getWebSocketReplyChannel() {
		return this.webSocketReplyChannel;
	}

	protected Collection<String> getDestinationPrefixes() {
		return (this.destinationPrefixes != null)
				? Arrays.<String>asList(this.destinationPrefixes) : Collections.<String>emptyList();
	}

	protected abstract AbstractBrokerMessageHandler getMessageHandler();

}
