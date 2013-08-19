package org.springframework.messaging.simp.raw;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.List;

/**
 * Simple implementation of the {@link SimpMessageHeaderAccessor} that handles raw WS messages.
 */
public class RawHeaderAccessor extends SimpMessageHeaderAccessor {
    public static final String COMMAND_HEADER = "stompCommand";

    /**
     * Create {@link RawHeaderAccessor} from the headers of an existing {@link org.springframework.messaging.Message}.
     */
    public static RawHeaderAccessor wrap(Message<?> message) {
        return new RawHeaderAccessor(message);
    }

    public RawHeaderAccessor(Message<?> message) {
        super(message);
    }

    protected RawHeaderAccessor(SimpMessageType messageType) {
        super(messageType, Collections.<String, List<String>>emptyMap());
    }

    public void setCommandIfNotSet(StompCommand command) {
        if (getCommand() == null) {
            setHeader(COMMAND_HEADER, command);
        }
    }

    public StompCommand getCommand() {
        return (StompCommand) getHeader(COMMAND_HEADER);
    }

}
