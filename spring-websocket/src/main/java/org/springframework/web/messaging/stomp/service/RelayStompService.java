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

package org.springframework.web.messaging.stomp.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.SocketFactory;

import org.springframework.core.task.TaskExecutor;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompHeaders;
import org.springframework.web.messaging.stomp.StompMessage;
import org.springframework.web.messaging.stomp.support.StompMessageConverter;

import reactor.core.Reactor;
import reactor.fn.Event;
import reactor.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RelayStompService extends AbstractStompService {


	private Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();

	private final StompMessageConverter messageConverter = new StompMessageConverter();

	private final TaskExecutor taskExecutor;


	public RelayStompService(Reactor reactor, TaskExecutor executor) {
		super(reactor);
		this.taskExecutor = executor; // For now, a naive way to manage socket reading
	}


	protected void processConnect(StompMessage stompMessage, final Object replyTo) {

		final String stompSessionId = stompMessage.getSessionId();

		final RelaySession session = new RelaySession();
		this.relaySessions.put(stompSessionId, session);

		try {
			Socket socket = SocketFactory.getDefault().createSocket("127.0.0.1", 61613);
			session.setSocket(socket);

			relayStompMessage(stompMessage);

			taskExecutor.execute(new RelayReadTask(stompSessionId, replyTo, session));
		}
		catch (Throwable t) {
			t.printStackTrace();
			clearRelaySession(stompSessionId);
		}
	}

	private void relayStompMessage(StompMessage stompMessage) {
		RelaySession session = RelayStompService.this.relaySessions.get(stompMessage.getSessionId());
		Assert.notNull(session, "RelaySession not found");
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Forwarding: " + stompMessage);
			}
			byte[] bytes = messageConverter.fromStompMessage(stompMessage);
			session.getOutputStream().write(bytes);
			session.getOutputStream().flush();
		}
		catch (Exception e) {
			e.printStackTrace();
			clearRelaySession(stompMessage.getSessionId());
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
	protected void processSubscribe(StompMessage message, Object replyTo) {
		relayStompMessage(message);
	}

	@Override
	protected void processSend(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processDisconnect(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processAck(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processNack(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processBegin(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processCommit(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processAbort(StompMessage message) {
		relayStompMessage(message);
	}

	@Override
	protected void processConnectionClosed(String sessionId) {
		if (logger.isDebugEnabled()) {
			logger.debug("Client connection closed for STOMP session=" + sessionId + ". Clearing relay session.");
		}
		clearRelaySession(sessionId);
	}


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

		private final String stompSessionId;
		private final Object replyTo;
		private final RelaySession session;

		private RelayReadTask(String stompSessionId, Object replyTo, RelaySession session) {
			this.stompSessionId = stompSessionId;
			this.replyTo = replyTo;
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
						StompMessage message = RelayStompService.this.messageConverter.toStompMessage(bytes);
						getReactor().notify(replyTo, Event.wrap(message));
						out.reset();
					}
					else {
						out.write(b);
					}
				}
				logger.debug("Socket closed, STOMP session=" + stompSessionId);
				sendErrorMessage("Lost connection");
			}
			catch (IOException e) {
				logger.error("Socket error: " + e.getMessage());
				clearRelaySession(stompSessionId);
			}
		}

		private void sendErrorMessage(String message) {
			StompHeaders headers = new StompHeaders();
			headers.setMessage(message);
			StompMessage errorMessage = new StompMessage(StompCommand.ERROR, headers);
			getReactor().notify(replyTo, Event.wrap(errorMessage));
		}
	}

}
