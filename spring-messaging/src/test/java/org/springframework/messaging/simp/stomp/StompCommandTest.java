package org.springframework.messaging.simp.stomp;

import org.junit.Test;
import org.springframework.messaging.simp.SimpMessageType;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.*;

public class StompCommandTest {

	private static final Collection<StompCommand> destinationRequired = Arrays.asList(StompCommand.SEND, StompCommand.SUBSCRIBE, StompCommand.MESSAGE);
	private static final Collection<StompCommand> subscriptionIdRequired = Arrays.asList(StompCommand.SUBSCRIBE, StompCommand.UNSUBSCRIBE, StompCommand.MESSAGE);
	private static final Collection<StompCommand> contentLengthRequired = Arrays.asList(StompCommand.SEND, StompCommand.MESSAGE, StompCommand.ERROR);
	private static final Collection<StompCommand> bodyAllowed = Arrays.asList(StompCommand.SEND, StompCommand.MESSAGE, StompCommand.ERROR);

	private static final Map<StompCommand, SimpMessageType> messageTypes = new EnumMap<>(StompCommand.class);

	static {
		messageTypes.put(StompCommand.CONNECT, SimpMessageType.CONNECT);
		messageTypes.put(StompCommand.STOMP, SimpMessageType.CONNECT);
		messageTypes.put(StompCommand.SEND, SimpMessageType.MESSAGE);
		messageTypes.put(StompCommand.MESSAGE, SimpMessageType.MESSAGE);
		messageTypes.put(StompCommand.SUBSCRIBE, SimpMessageType.SUBSCRIBE);
		messageTypes.put(StompCommand.UNSUBSCRIBE, SimpMessageType.UNSUBSCRIBE);
		messageTypes.put(StompCommand.DISCONNECT, SimpMessageType.DISCONNECT);
	}

	@Test
	public void getMessageType() throws Exception {
		for (final Map.Entry<StompCommand, SimpMessageType> stompToSimp : messageTypes.entrySet()) {
			assertEquals(stompToSimp.getKey().getMessageType(), stompToSimp.getValue());
		}
	}

	@Test
	public void requiresDestination() throws Exception {
		for (final StompCommand stompCommand : StompCommand.values()) {
			if (destinationRequired.contains(stompCommand)) {
				assertTrue(stompCommand.requiresDestination());
			} else {
				assertFalse(stompCommand.requiresDestination());
			}
		}
	}

	@Test
	public void requiresSubscriptionId() throws Exception {
		for (final StompCommand stompCommand : StompCommand.values()) {
			if (subscriptionIdRequired.contains(stompCommand)) {
				assertTrue(stompCommand.requiresSubscriptionId());
			} else {
				assertFalse(stompCommand.requiresSubscriptionId());
			}
		}
	}

	@Test
	public void requiresContentLength() throws Exception {
		for (final StompCommand stompCommand : StompCommand.values()) {
			if (contentLengthRequired.contains(stompCommand)) {
				assertTrue(stompCommand.requiresContentLength());
			} else {
				assertFalse(stompCommand.requiresContentLength());
			}
		}
	}

	@Test
	public void isBodyAllowed() throws Exception {
		for (final StompCommand stompCommand : StompCommand.values()) {
			if (bodyAllowed.contains(stompCommand)) {
				assertTrue(stompCommand.isBodyAllowed());
			} else {
				assertFalse(stompCommand.isBodyAllowed());
			}
		}
	}
}
