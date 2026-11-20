/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.util.Assert;

/**
 * Base class for message broker registration classes.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractBrokerRegistration {

	private final SubscribableChannel clientInboundChannel;

	private final MessageChannel clientOutboundChannel;

	private final List<String> destinationPrefixes;


	/**
	 * Create a new broker registration.
	 * @param clientInboundChannel the inbound channel
	 * @param clientOutboundChannel the outbound channel
	 * @param destinationPrefixes the destination prefixes
	 */
	public AbstractBrokerRegistration(SubscribableChannel clientInboundChannel,
			MessageChannel clientOutboundChannel, String @Nullable [] destinationPrefixes) {

		Assert.notNull(clientInboundChannel, "'clientInboundChannel' must not be null");
		Assert.notNull(clientOutboundChannel, "'clientOutboundChannel' must not be null");

		this.clientInboundChannel = clientInboundChannel;
		this.clientOutboundChannel = clientOutboundChannel;

		this.destinationPrefixes = (destinationPrefixes != null ?
				Arrays.asList(destinationPrefixes) : Collections.emptyList());
	}


	protected SubscribableChannel getClientInboundChannel() {
		return this.clientInboundChannel;
	}

	protected MessageChannel getClientOutboundChannel() {
		return this.clientOutboundChannel;
	}

	protected Collection<String> getDestinationPrefixes() {
		return this.destinationPrefixes;
	}


	protected abstract AbstractBrokerMessageHandler getMessageHandler(SubscribableChannel brokerChannel);

}
