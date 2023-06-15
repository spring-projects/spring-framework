/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import org.junit.jupiter.api.Disabled;

import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNetty2TcpClient;

/**
 * Integration tests for {@link StompBrokerRelayMessageHandler} running against
 * ActiveMQ with {@link ReactorNetty2TcpClient}.
 *
 * @author Rossen Stoyanchev
 */
@Disabled("gh-29287 :: Disabled because they fail too frequently")
public class ReactorNetty2StompBrokerRelayIntegrationTests extends AbstractStompBrokerRelayIntegrationTests {

	@Override
	protected TcpOperations<byte[]> initTcpClient(int port) {
		return new ReactorNetty2TcpClient<>("127.0.0.1", port, new StompTcpMessageCodec());
	}

}
