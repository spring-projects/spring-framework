/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class StompCommandTests {

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
	void getMessageType() {
		for (StompCommand stompCommand : StompCommand.values()) {
			SimpMessageType simp = messageTypes.get(stompCommand);
			if (simp == null) {
				simp = SimpMessageType.OTHER;
			}
			assertThat(stompCommand.getMessageType()).isSameAs(simp);
		}
	}

	@Test
	void requiresDestination() {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.requiresDestination()).isEqualTo(destinationRequired.contains(stompCommand));
		}
	}

	@Test
	void requiresSubscriptionId() {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.requiresSubscriptionId()).isEqualTo(subscriptionIdRequired.contains(stompCommand));
		}
	}

	@Test
	void requiresContentLength() {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.requiresContentLength()).isEqualTo(contentLengthRequired.contains(stompCommand));
		}
	}

	@Test
	void isBodyAllowed() {
		for (StompCommand stompCommand : StompCommand.values()) {
			assertThat(stompCommand.isBodyAllowed()).isEqualTo(bodyAllowed.contains(stompCommand));
		}
	}

}
