/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.adapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;
import jakarta.websocket.Session;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

/**
 * Spring {@link WebSocketSession} adapter for a standard Java (JSR 356)
 * {@link jakarta.websocket.Session}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class StandardWebSocketSession extends AbstractListenerWebSocketSession<Session> {

	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		this(session, info, factory, null);
	}

	public StandardWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
			Sinks.@Nullable Empty<Void> completionSink) {

		super(session, session.getId(), info, factory, completionSink);
	}


	@Override
	protected boolean canSuspendReceiving() {
		return false;
	}

	@Override
	protected void suspendReceiving() {
		// no-op
	}

	@Override
	protected void resumeReceiving() {
		// no-op
	}

	@Override
	protected boolean sendMessage(WebSocketMessage message) throws IOException {
		DataBuffer dataBuffer = message.getPayload();
		RemoteEndpoint.Async remote = getDelegate().getAsyncRemote();
		if (WebSocketMessage.Type.TEXT.equals(message.getType())) {
			getSendProcessor().setReadyToSend(false);
			String text = dataBuffer.toString(StandardCharsets.UTF_8);
			remote.sendText(text, new SendProcessorCallback());
		}
		else {
			if (WebSocketMessage.Type.BINARY.equals(message.getType())) {
				getSendProcessor().setReadyToSend(false);
			}
			try (DataBuffer.ByteBufferIterator iterator = dataBuffer.readableByteBuffers()) {
				while (iterator.hasNext()) {
					ByteBuffer byteBuffer = iterator.next();
					switch (message.getType()) {
						case BINARY -> remote.sendBinary(byteBuffer, new SendProcessorCallback());
						case PING -> remote.sendPing(byteBuffer);
						case PONG -> remote.sendPong(byteBuffer);
						default -> throw new IllegalArgumentException("Unexpected message type: " + message.getType());
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean isOpen() {
		return getDelegate().isOpen();
	}

	@Override
	public Mono<Void> close(CloseStatus status) {
		try {
			CloseReason.CloseCode code = CloseCodes.getCloseCode(status.getCode());
			getDelegate().close(new CloseReason(code, status.getReason()));
		}
		catch (IOException ex) {
			return Mono.error(ex);
		}
		return Mono.empty();
	}


	private final class SendProcessorCallback implements SendHandler {

		@Override
		public void onResult(SendResult result) {
			if (result.isOK()) {
				getSendProcessor().setReadyToSend(true);
				getSendProcessor().onWritePossible();
			}
			else {
				getSendProcessor().cancel();
				getSendProcessor().onError(result.getException());
			}
		}

	}

}
