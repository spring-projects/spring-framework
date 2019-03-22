/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Test;

import org.springframework.messaging.simp.SimpMessageType;

import static org.junit.Assert.*;

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
			assertSame(simp, stompCommand.getMessageType());
		}
	}

	@Test
	public void requiresDestination() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertEquals(destinationRequired.contains(stompCommand), stompCommand.requiresDestination());
		}
	}

	@Test
	public void requiresSubscriptionId() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertEquals(subscriptionIdRequired.contains(stompCommand), stompCommand.requiresSubscriptionId());
		}
	}

	@Test
	public void requiresContentLength() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertEquals(contentLengthRequired.contains(stompCommand), stompCommand.requiresContentLength());
		}
	}

	@Test
	public void isBodyAllowed() throws Exception {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertEquals(bodyAllowed.contains(stompCommand), stompCommand.isBodyAllowed());
		}
	}

}
