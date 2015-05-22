/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.messaging;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompEncoder;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * A base class providing methods to create error messages and send them to a connected
 * WebSocket client through their session.
 *
 * @author Jaymes Bearden
 * @since 4.1.4
 */
public abstract class AbstractStompSubProtocolErrorHandler implements StompSubProtocolErrorHandler {

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    private StompEncoder stompEncoder = new StompEncoder();

    /**
     * Craft and return a Stomp ERROR frame
     * @return StompHeaderAccessor representing a Stomp ERROR
     */
    protected StompHeaderAccessor getStompErrorMessage() {
        return StompHeaderAccessor.create(StompCommand.ERROR);
    }

    /**
     * Encode and send the supplied errorHeaders to the client
     * @param session the client session
     * @param errorHeaders to send
     * @throws IOException
     */
    protected void sendErrorMessage(WebSocketSession session, StompHeaderAccessor errorHeaders) throws IOException {
        byte[] errorMessage = stompEncoder.encode(errorHeaders.getMessageHeaders(), EMPTY_PAYLOAD);
        session.sendMessage(new TextMessage(errorMessage));
    }
}
