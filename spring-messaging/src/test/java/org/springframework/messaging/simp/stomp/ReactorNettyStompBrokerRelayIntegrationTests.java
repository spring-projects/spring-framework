/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import java.util.Collection;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;

/**
 * Integration tests for {@link StompBrokerRelayMessageHandler} running against
 * ActiveMQ with {@link ReactorNettyTcpClient}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class ReactorNettyStompBrokerRelayIntegrationTests extends AbstractStompBrokerRelayIntegrationTests {

	@Override
	protected StompBrokerRelayMessageHandler createRelay(SubscribableChannel inboundChannel,
			MessageChannel outboundChannel, SubscribableChannel brokerChannel, Collection<String> destinationPrefixes,
			int port) {

		StompBrokerRelayMessageHandler handler =
				new StompBrokerRelayMessageHandler(inboundChannel, outboundChannel, brokerChannel, destinationPrefixes);
		handler.setRelayPort(port);
		return handler;
	}

	@Override
	protected TcpOperations<byte[]> initTcpClient(int port) {
		return new ReactorNettyTcpClient<>("127.0.0.1", port, new StompReactorNettyCodec());
	}

}
