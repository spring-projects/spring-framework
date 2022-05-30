/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Unit tests for
 * {@link org.springframework.messaging.simp.config.StompBrokerRelayRegistration}.
 *
 * @author Rossen Stoyanchev
 */
public class StompBrokerRelayRegistrationTests {

	@Test
	public void test() {

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
