/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.messaging.stomp.support;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.SocketFactory;

import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.web.messaging.MessageType;
import org.springframework.web.messaging.PubSubHeaders;
import org.springframework.web.messaging.converter.CompositeMessageConverter;
import org.springframework.web.messaging.converter.MessageConverter;
import org.springframework.web.messaging.service.AbstractPubSubMessageHandler;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompRelayPubSubMessageHandler extends AbstractPubSubMessageHandler {


	private final StompMessageConverter stompMessageConverter = new StompMessageConverter();

	private MessageConverter payloadConverter;

	private final TaskExecutor taskExecutor;

	private Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();


	/**
	 * @param executor
	 */
	public StompRelayPubSubMessageHandler(SubscribableChannel publishChannel, MessageChannel clientChannel,
			TaskExecutor executor) {

		super(publishChannel, clientChannel);
		this.taskExecutor = executor; // For now, a naive way to manage socket reading
		this.payloadConverter = new CompositeMessageConverter(null);
	}


	public void setMessageConverters(List<MessageConverter> converters) {
		this.payloadConverter = new CompositeMessageConverter(converters);
	}

	@Override
	protected Collection<MessageType> getSupportedMessageTypes() {
		return null;
	}

	@Override
	public void handleConnect(Message<?> message) {

		String sessionId = (String) message.getHeaders().get(PubSubHeaders.SESSION_ID);

		RelaySession session = new RelaySession();
		this.relaySessions.put(sessionId, session);

		try {
			Socket socket = SocketFactory.getDefault().createSocket("127.0.0.1", 61613);
			session.setSocket(socket);

			forwardMessage(message, StompCommand.CONNECT);

			RelayReadTask readTask = new RelayReadTask(sessionId, session);
			this.taskExecutor.execute(readTask);
		}
		catch (Throwable t) {
			t.printStackTrace();
			clearRelaySession(sessionId);
		}
	}

	private void forwardMessage(Message<?> message, StompCommand command) {

		StompHeaders stompHeaders = StompHeaders.fromMessageHeaders(message.getHeaders());
		String sessionId = stompHeaders.getSessionId();
		RelaySession session = StompRelayPubSubMessageHandler.this.relaySessions.get(sessionId);
		Assert.notNull(session, "RelaySession not found");

		try {
			stompHeaders.setStompCommandIfNotSet(StompCommand.SEND);

			MediaType contentType = stompHeaders.getContentType();
			byte[] payload = this.payloadConverter.convertToPayload(message.getPayload(), contentType);
			Message<byte[]> byteMessage = new GenericMessage<byte[]>(payload, stompHeaders.toMessageHeaders());

			if (logger.isTraceEnabled()) {
				logger.trace("Forwarding STOMP " + stompHeaders.getStompCommand() + " message");
			}

			byte[] bytes = this.stompMessageConverter.fromMessage(byteMessage);
			session.getOutputStream().write(bytes);
			session.getOutputStream().flush();
		}
		catch (Exception ex) {
			logger.error("Couldn't forward message " + message, ex);
			clearRelaySession(sessionId);
		}
	}

	private void clearRelaySession(String stompSessionId) {
		RelaySession relaySession = this.relaySessions.remove(stompSessionId);
		if (relaySession != null) {
			// TODO: raise failure event so client session can be closed
			try {
				relaySession.getSocket().close();
			}
			catch (IOException e) {
				// ignore
			}
		}
	}

	@Override
	public void handlePublish(Message<?> message) {
		forwardMessage(message, StompCommand.SEND);
	}

	@Override
	public void handleSubscribe(Message<?> message) {
		forwardMessage(message, StompCommand.SUBSCRIBE);
	}

	@Override
	public void handleUnsubscribe(Message<?> message) {
		forwardMessage(message, StompCommand.UNSUBSCRIBE);
	}

	@Override
	public void handleDisconnect(Message<?> message) {
		forwardMessage(message, StompCommand.DISCONNECT);
	}

	@Override
	public void handleOther(Message<?> message) {
		StompCommand command = (StompCommand) message.getHeaders().get(PubSubHeaders.PROTOCOL_MESSAGE_TYPE);
		Assert.notNull(command, "Expected STOMP command: " + message.getHeaders());
		forwardMessage(message, command);
	}

/*	@Override
	public void handleClientConnectionClosed(String sessionId) {
		if (logger.isDebugEnabled()) {
			logger.debug("Client connection closed for STOMP session=" + sessionId + ". Clearing relay session.");
		}
		clearRelaySession(sessionId);
	}
*/

	private final static class RelaySession {

		private Socket socket;

		private InputStream inputStream;

		private OutputStream outputStream;


		public void setSocket(Socket socket) throws IOException {
			this.socket = socket;
			this.inputStream = new BufferedInputStream(socket.getInputStream());
			this.outputStream = new BufferedOutputStream(socket.getOutputStream());
		}

		public Socket getSocket() {
			return this.socket;
		}

		public InputStream getInputStream() {
			return this.inputStream;
		}

		public OutputStream getOutputStream() {
			return this.outputStream;
		}
	}

	private final class RelayReadTask implements Runnable {

		private final String sessionId;

		private final RelaySession session;

		private RelayReadTask(String sessionId, RelaySession session) {
			this.sessionId = sessionId;
			this.session = session;
		}

		@Override
		public void run() {
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				while (!session.getSocket().isClosed()) {
					int b = session.getInputStream().read();
					if (b == -1) {
						break;
					}
					else if (b == 0x00) {
						byte[] bytes = out.toByteArray();
						Message<byte[]> message = stompMessageConverter.toMessage(bytes, sessionId);
						getClientChannel().send(message);
						out.reset();
					}
					else {
						out.write(b);
					}
				}
				logger.debug("Socket closed, STOMP session=" + sessionId);
				sendErrorMessage("Lost connection");
			}
			catch (IOException e) {
				logger.error("Socket error: " + e.getMessage());
				clearRelaySession(sessionId);
				sendErrorMessage("Lost connection");
			}
		}

		private void sendErrorMessage(String message) {
			StompHeaders stompHeaders = StompHeaders.create(StompCommand.ERROR);
			stompHeaders.setMessage(message);
			stompHeaders.setSessionId(this.sessionId);
			Message<byte[]> errorMessage = new GenericMessage<byte[]>(new byte[0], stompHeaders.toMessageHeaders());
			getClientChannel().send(errorMessage);
		}
	}

}
