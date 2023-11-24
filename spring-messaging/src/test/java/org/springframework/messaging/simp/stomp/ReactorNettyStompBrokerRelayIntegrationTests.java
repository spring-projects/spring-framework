/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.messaging.tcp.TcpOperations;
import org.springframework.messaging.tcp.reactor.ReactorNettyTcpClient;

/**
 * Integration tests for {@link StompBrokerRelayMessageHandler} running against
 * ActiveMQ with {@link ReactorNettyTcpClient}.
 *
 * @author Rossen Stoyanchev
 */
public class ReactorNettyStompBrokerRelayIntegrationTests extends AbstractStompBrokerRelayIntegrationTests {

	@Override
	protected TcpOperations<byte[]> initTcpClient(int port) {
		return new ReactorNettyTcpClient<>("127.0.0.1", port, new StompReactorNettyCodec());
	}

}
