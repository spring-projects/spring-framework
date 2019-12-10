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

package org.springframework.messaging.simp.stomp;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.simp.SimpMessageType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class StompCommandTests {

	private static final Collection<StompCommand> destinationRequired =
			Arrays.asList(StompCommand.SEND, StompCommand.SUBSCRIBE, StompCommand.MESSAGE);

	private static final Collection<StompCommand> subscriptionIdRequired =
			Arrays.asList(StompCommand.SUBSCRIBE, StompCommand.UNSUBSCRIBE, StompCommand.MESSAGE);

	private static final Collection<StompCommand> contentLengthRequired =
			Arrays.asList(StompCommand.SEND, StompCommand.MESSAGE, StompCommand.ERROR);

	private static final Collection<StompCommand> bodyAllowed =
			Arrays.asList(StompCommand.SEND, StompCommand.MESSAGE, StompCommand.ERROR);

	private static final Map<StompCommand, SimpMessageType> messageTypes =
			new EnumMap<>(StompCommand.class);

	static {
		messageTypes.put(StompCommand.STOMP, SimpMessageType.CONNECT);
		messageTypes.put(StompCommand.CONNECT, SimpMessageType.CONNECT);
		messageTypes.put(StompCommand.DISCONNECT, SimpMessageType.DISCONNECT);
		messageTypes.put(StompCommand.SUBSCRIBE, SimpMessageType.SUBSCRIBE);
		messageTypes.put(StompCommand.UNSUBSCRIBE, SimpMessageType.UNSUBSCRIBE);
		messageTypes.put(StompCommand.SEND, SimpMessageType.MESSAGE);
		messageTypes.put(StompCommand.MESSAGE, SimpMessageType.MESSAGE);
	}


	@Test
	public void getMessageType() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			SimpMessageType simp = messageTypes.get(stompCommand);
			if (simp == null) {
				simp = SimpMessageType.OTHER;
			}
			assertThat(stompCommand.getMessageType()).isSameAs(simp);
		}
	}

	@Test
	public void requiresDestination() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.requiresDestination()).isEqualTo(destinationRequired.contains(stompCommand));
		}
	}

	@Test
	public void requiresSubscriptionId() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.requiresSubscriptionId()).isEqualTo(subscriptionIdRequired.contains(stompCommand));
		}
	}

	@Test
	public void requiresContentLength() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.requiresContentLength()).isEqualTo(contentLengthRequired.contains(stompCommand));
		}
	}

	@Test
	public void isBodyAllowed() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.isBodyAllowed()).isEqualTo(bodyAllowed.contains(stompCommand));
		}
	}

}
