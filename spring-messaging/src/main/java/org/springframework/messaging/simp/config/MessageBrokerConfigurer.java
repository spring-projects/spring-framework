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

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.handler.AbstractBrokerMessageHandler;

import reactor.util.Assert;


/**
 * A helper class for configuring message broker options.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageBrokerConfigurer {

	private final MessageChannel webSocketReplyChannel;

	private SimpleBrokerRegistration simpleBroker;

	private StompBrokerRelayRegistration stompRelay;

	private String[] annotationMethodDestinationPrefixes;


	public MessageBrokerConfigurer(MessageChannel webSocketReplyChannel) {
		Assert.notNull(webSocketReplyChannel);
		this.webSocketReplyChannel = webSocketReplyChannel;
	}

	public SimpleBrokerRegistration enableSimpleBroker(String... destinationPrefixes) {
		this.simpleBroker = new SimpleBrokerRegistration(this.webSocketReplyChannel, destinationPrefixes);
		return this.simpleBroker;
	}

	public StompBrokerRelayRegistration enableStompBrokerRelay(String... destinationPrefixes) {
		this.stompRelay = new StompBrokerRelayRegistration(this.webSocketReplyChannel, destinationPrefixes);
		return this.stompRelay;
	}

	public MessageBrokerConfigurer setAnnotationMethodDestinationPrefixes(String... destinationPrefixes) {
		this.annotationMethodDestinationPrefixes = destinationPrefixes;
		return this;
	}

	protected AbstractBrokerMessageHandler getSimpleBroker() {
		initSimpleBrokerIfNecessary();
		return (this.simpleBroker != null) ? this.simpleBroker.getMessageHandler() : null;
	}

	protected void initSimpleBrokerIfNecessary() {
		if ((this.simpleBroker == null) && (this.stompRelay == null)) {
			this.simpleBroker = new SimpleBrokerRegistration(this.webSocketReplyChannel, null);
		}
	}

	protected AbstractBrokerMessageHandler getStompBrokerRelay() {
		return (this.stompRelay != null) ? this.stompRelay.getMessageHandler() : null;
	}

	protected Collection<String> getAnnotationMethodDestinationPrefixes() {
		return (this.annotationMethodDestinationPrefixes != null)
				? Arrays.asList(this.annotationMethodDestinationPrefixes) : null;
	}
}
