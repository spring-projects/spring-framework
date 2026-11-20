/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.config;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.StubMessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StompBrokerRelayRegistration}.
 *
 * @author Rossen Stoyanchev
 */
class StompBrokerRelayRegistrationTests {

	@Test
	void test() {

		SubscribableChannel inChannel = new StubMessageChannel();
		MessageChannel outChannel = new StubMessageChannel();
		String[] prefixes = new String[] { "/foo", "/bar" };

		StompBrokerRelayRegistration registration = new StompBrokerRelayRegistration(inChannel, outChannel, prefixes);
		registration.setClientLogin("clientlogin");
		registration.setClientPasscode("clientpasscode");
		registration.setSystemLogin("syslogin");
		registration.setSystemPasscode("syspasscode");
		registration.setSystemHeartbeatReceiveInterval(123);
		registration.setSystemHeartbeatSendInterval(456);
		registration.setVirtualHost("example.org");

		StompBrokerRelayMessageHandler handler = registration.getMessageHandler(new StubMessageChannel());

		assertThat(StringUtils.toStringArray(handler.getDestinationPrefixes())).isEqualTo(prefixes);
		assertThat(handler.getClientLogin()).isEqualTo("clientlogin");
		assertThat(handler.getClientPasscode()).isEqualTo("clientpasscode");
		assertThat(handler.getSystemLogin()).isEqualTo("syslogin");
		assertThat(handler.getSystemPasscode()).isEqualTo("syspasscode");
		assertThat(handler.getSystemHeartbeatReceiveInterval()).isEqualTo(123);
		assertThat(handler.getSystemHeartbeatSendInterval()).isEqualTo(456);
		assertThat(handler.getVirtualHost()).isEqualTo("example.org");
	}

}
