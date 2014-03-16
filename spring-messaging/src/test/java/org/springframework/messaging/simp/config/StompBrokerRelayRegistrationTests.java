/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.springframework.messaging.simp.config.StompBrokerRelayRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class StompBrokerRelayRegistrationTests {


	@Test
	public void test() {

		SubscribableChannel clientInboundChannel = new StubMessageChannel();
		MessageChannel clientOutboundChannel = new StubMessageChannel();
		SubscribableChannel brokerChannel = new StubMessageChannel();

		String[] destinationPrefixes = new String[] { "/foo", "/bar" };

		StompBrokerRelayRegistration registration = new StompBrokerRelayRegistration(
				clientInboundChannel, clientOutboundChannel, destinationPrefixes);

		registration.setClientLogin("clientlogin");
		registration.setClientPasscode("clientpasscode");
		registration.setSystemLogin("syslogin");
		registration.setSystemPasscode("syspasscode");
		registration.setSystemHeartbeatReceiveInterval(123);
		registration.setSystemHeartbeatSendInterval(456);
		registration.setVirtualHost("example.org");

		StompBrokerRelayMessageHandler relayMessageHandler = registration.getMessageHandler(brokerChannel);

		assertEquals(Arrays.asList(destinationPrefixes),
				new ArrayList<String>(relayMessageHandler.getDestinationPrefixes()));

		assertEquals("clientlogin", relayMessageHandler.getClientLogin());
		assertEquals("clientpasscode", relayMessageHandler.getClientPasscode());
		assertEquals("syslogin", relayMessageHandler.getSystemLogin());
		assertEquals("syspasscode", relayMessageHandler.getSystemPasscode());
		assertEquals(123, relayMessageHandler.getSystemHeartbeatReceiveInterval());
		assertEquals(456, relayMessageHandler.getSystemHeartbeatSendInterval());
		assertEquals("example.org", relayMessageHandler.getVirtualHost());
	}

}
