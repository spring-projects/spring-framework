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
import org.springframework.messaging.simp.handler.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.util.Assert;

/**
 * A registry for configuring message broker options.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MessageBrokerRegistry {

	private final MessageChannel clientOutboundChannel;

	private SimpleBrokerRegistration simpleBroker;

	private StompBrokerRelayRegistration stompRelay;

	private String[] applicationDestinationPrefixes;

	private String userDestinationPrefix;

	private ChannelRegistration brokerChannelRegistration = new ChannelRegistration();


	public MessageBrokerRegistry(MessageChannel clientOutboundChannel) {
		Assert.notNull(clientOutboundChannel);
		this.clientOutboundChannel = clientOutboundChannel;
	}

	/**
	 * Enable a simple message broker and configure one or more prefixes to filter
	 * destinations targeting the broker (e.g. destinations prefixed with "/topic").
	 */
	public SimpleBrokerRegistration enableSimpleBroker(String... destinationPrefixes) {
		this.simpleBroker = new SimpleBrokerRegistration(this.clientOutboundChannel, destinationPrefixes);
		return this.simpleBroker;
	}

	/**
	 * Enable a STOMP broker relay and configure the destination prefixes supported by the
	 * message broker. Check the STOMP documentation of the message broker for supported
	 * destinations.
	 */
	public StompBrokerRelayRegistration enableStompBrokerRelay(String... destinationPrefixes) {
		this.stompRelay = new StompBrokerRelayRegistration(this.clientOutboundChannel, destinationPrefixes);
		return this.stompRelay;
	}

	/**
	 * Configure one or more prefixes to filter destinations targeting application
	 * annotated methods. For example destinations prefixed with "/app" may be
	 * processed by annotated methods while other destinations may target the
	 * message broker (e.g. "/topic", "/queue").
	 * <p>When messages are processed, the matching prefix is removed from the destination
	 * in order to form the lookup path. This means annotations should not contain the
	 * destination prefix.
	 * <p>Prefixes that do not have a trailing slash will have one automatically appended.
	 */
	public MessageBrokerRegistry setApplicationDestinationPrefixes(String... prefixes) {
		this.applicationDestinationPrefixes = prefixes;
		return this;
	}

	/**
	 * Configure the prefix used to identify user destinations. User destinations
	 * provide the ability for a user to subscribe to queue names unique to their
	 * session as well as for others to send messages to those unique,
	 * user-specific queues.
	 * <p>For example when a user attempts to subscribe to "/user/queue/position-updates",
	 * the destination may be translated to "/queue/position-updatesi9oqdfzo" yielding a
	 * unique queue name that does not collide with any other user attempting to do the same.
	 * Subsequently when messages are sent to "/user/{username}/queue/position-updates",
	 * the destination is translated to "/queue/position-updatesi9oqdfzo".
	 * <p>The default prefix used to identify such destinations is "/user/".
	 */
	public MessageBrokerRegistry setUserDestinationPrefix(String destinationPrefix) {
		this.userDestinationPrefix = destinationPrefix;
		return this;
	}

	/**
	 * Customize the channel used to send messages from the application to the message
	 * broker. By default messages from the application to the message broker are sent
	 * synchronously, which means application code sending a message will find out
	 * if the message cannot be sent through an exception. However, this can be changed
	 * if the broker channel is configured here with task executor properties.
	 */
	public ChannelRegistration configureBrokerChannel() {
		return this.brokerChannelRegistration;
	}


	protected SimpleBrokerMessageHandler getSimpleBroker() {
		initSimpleBrokerIfNecessary();
		return (this.simpleBroker != null) ? this.simpleBroker.getMessageHandler() : null;
	}

	protected void initSimpleBrokerIfNecessary() {
		if ((this.simpleBroker == null) && (this.stompRelay == null)) {
			this.simpleBroker = new SimpleBrokerRegistration(this.clientOutboundChannel, null);
		}
	}

	protected StompBrokerRelayMessageHandler getStompBrokerRelay() {
		return (this.stompRelay != null) ? this.stompRelay.getMessageHandler() : null;
	}

	protected Collection<String> getApplicationDestinationPrefixes() {
		return (this.applicationDestinationPrefixes != null)
				? Arrays.asList(this.applicationDestinationPrefixes) : null;
	}

	protected String getUserDestinationPrefix() {
		return this.userDestinationPrefix;
	}

	protected ChannelRegistration getBrokerChannelRegistration() {
		return this.brokerChannelRegistration;
	}
}
