package org.springframework.messaging.simp.raw;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.websocket.SubProtocolHandler;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bridge between raw web socket connections and the messaging infrastructure. Allows you to uniformly handle multiple
 * web socket protocols. You may have STOMP and raw in the same application, if required.
 */
public class RawProtocolHandler implements SubProtocolHandler {

    private final Log logger = LogFactory.getLog(RawProtocolHandler.class);

    private boolean ignoreLastNumberPathElement = true;

    private Pattern destinationPattern = Pattern.compile("/websocket/(.*)/-?\\d+");

    @Override
    public List<String> getSupportedProtocols() {
        return Collections.emptyList();
    }

    @Override
    public void handleMessageFromClient(WebSocketSession session, WebSocketMessage webSocketMessage, MessageChannel outputChannel) throws Exception {
        try {
            Object payload = null;
            if (webSocketMessage instanceof TextMessage) {
                payload = ((TextMessage)webSocketMessage).getPayload();
            }
            if (webSocketMessage instanceof BinaryMessage) {
                payload = ((BinaryMessage)webSocketMessage).getByteArray();
            }

            // this should not really happen unless there is a new subtype of WebSocketMessage
            if (payload == null) throw new IllegalArgumentException("Unexpected WebSocketMessage type " + webSocketMessage);

            if (logger.isTraceEnabled()) {
                logger.trace("Processing raw webSocketMessage: " + webSocketMessage);
            }

            try {
                RawHeaderAccessor headers = new RawHeaderAccessor(SimpMessageType.MESSAGE);
                Matcher matcher = destinationPattern.matcher(session.getUri().getPath());
                if (matcher.find() && matcher.groupCount() > 0) {
                    headers.setDestination(matcher.group(1));
                }
                headers.setSessionId(session.getId());
                headers.setUser(session.getPrincipal());
                Message<Object> message = MessageBuilder.withPayloadAndHeaders(payload, headers).build();

                /* TODO: Need to do anything here?
                if (SimpMessageType.CONNECT.equals(headers.getMessageType())) {
                }
                */

                outputChannel.send(message);

            }
            catch (Throwable t) {
                logger.error("Terminating STOMP session due to failure to send webSocketMessage: ", t);
                sendErrorMessage(session, t);
            }

            // TODO: send RECEIPT webSocketMessage if incoming webSocketMessage has "receipt" header
            // http://stomp.github.io/stomp-specification-1.2.html#Header_receipt

        }
        catch (Throwable error) {
            sendErrorMessage(session, error);
        }
    }


    protected void sendErrorMessage(WebSocketSession session, Throwable error) {
        /* TODO: Implement me
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.ERROR);
        headers.setMessage(error.getMessage());
        Message<?> message = MessageBuilder.withPayloadAndHeaders(new byte[0], headers).build();
        byte[] bytes = this.stompMessageConverter.fromMessage(message);
        try {
            session.sendMessage(new TextMessage(new String(bytes, Charset.forName("UTF-8"))));
        }
        catch (Throwable t) {
            // ignore
        }
        */
    }

    @Override
    public void handleMessageToClient(WebSocketSession session, Message<?> message) throws Exception {
        RawHeaderAccessor headers = RawHeaderAccessor.wrap(message);
        headers.setCommandIfNotSet(StompCommand.MESSAGE);

        if (StompCommand.CONNECTED.equals(headers.getCommand())) {
            // Ignore for now since we already sent it
            return;
        }

        if (StompCommand.MESSAGE.equals(headers.getCommand()) && (headers.getSubscriptionId() == null)) {
            // TODO: failed message delivery mechanism
            logger.error("Ignoring message, no subscriptionId header: " + message);
            return;
        }

        if (!(message.getPayload() instanceof byte[])) {
            // TODO: failed message delivery mechanism
            logger.error("Ignoring message, expected byte[] content: " + message);
            return;
        }

        try {
            WebSocketMessage<?> webSocketMessage;
            if (message.getPayload() instanceof byte[]) {
                webSocketMessage = new BinaryMessage((byte[])message.getPayload());
            } else {
                webSocketMessage = new TextMessage(message.getPayload().toString());
            }
            session.sendMessage(webSocketMessage);
        }
        catch (Throwable t) {
            sendErrorMessage(session, t);
        }
        finally {
            if (StompCommand.ERROR.equals(headers.getCommand())) {
                try {
                    session.close(CloseStatus.PROTOCOL_ERROR);
                }
                catch (IOException e) {
                }
            }
        }
    }

    @Override
    public String resolveSessionId(Message<?> message) {
        RawHeaderAccessor headers = RawHeaderAccessor.wrap(message);
        return headers.getSessionId();
    }

    @Override
    public void afterSessionStarted(WebSocketSession session, MessageChannel outputChannel) throws Exception {
        // noop
    }

    @Override
    public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus, MessageChannel outputChannel) throws Exception {
        // noop
    }

    /**
     * Sets the patterns that extracts the "path" from the raw WS URI. A good example may be <code>/websocket/(.*)/d+</code>
     *
     * This enables you to map the WS requests to <code>@Controller</code>-annotated classes; and to deal with STOMP as
     * well as raw WS messages interchangeably.
     *
     * @param destinationPattern the destinationPattern to be used
     */
    public void setDestinationPattern(Pattern destinationPattern) {
        Assert.notNull(destinationPattern, "The 'destinationPattern' must not be null.");

        this.destinationPattern = destinationPattern;
    }

    /**
     * Sets the URI prefix that should be dropped from the raw WS URI. This is a more convenient approach than constructing
     * the {@link Pattern} and calling {@link #setDestinationPattern(java.util.regex.Pattern)}.
     *
     * @param uriPrefix the URI prefix to drop
     */
    public void setUriPrefix(String uriPrefix) {
        String pattern;
        if (this.ignoreLastNumberPathElement) {
            pattern = String.format("%s(.*)/-?\\d+", uriPrefix);
        } else {
            pattern = String.format("%s(.*)", uriPrefix);
        }

        this.destinationPattern = Pattern.compile(pattern);
    }
}
