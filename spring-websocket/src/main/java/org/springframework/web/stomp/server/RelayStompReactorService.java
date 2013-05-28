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

package org.springframework.web.stomp.server;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.stomp.StompCommand;
import org.springframework.web.stomp.StompHeaders;
import org.springframework.web.stomp.StompMessage;
import org.springframework.web.stomp.support.StompMessageConverter;

import reactor.Fn;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.util.Assert;


/**
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class RelayStompReactorService {

	private static final Log logger = LogFactory.getLog(RelayStompReactorService.class);


	private final Reactor reactor;

	private Map<String, RelaySession> relaySessions = new ConcurrentHashMap<String, RelaySession>();

	private final StompMessageConverter converter = new StompMessageConverter();

	private final TaskExecutor taskExecutor;


	public RelayStompReactorService(Reactor reactor, TaskExecutor executor) {
		this.reactor = reactor;
		this.taskExecutor = executor; // For now, a naively way to manage socket reading

		this.reactor.on(Fn.$(StompCommand.CONNECT), new ConnectConsumer());
		this.reactor.on(Fn.$(StompCommand.SUBSCRIBE), new RelayConsumer());
		this.reactor.on(Fn.$(StompCommand.SEND), new RelayConsumer());
		this.reactor.on(Fn.$(StompCommand.DISCONNECT), new RelayConsumer());

		this.reactor.on(Fn.$("CONNECTION_CLOSED"), new Consumer<Event<String>>() {
			@Override
			public void accept(Event<String> event) {
				if (logger.isDebugEnabled()) {
					logger.debug("CONNECTION_CLOSED, STOMP session=" + event.getData() + ". Clearing relay session");
				}
				clearRelaySession(event.getData());
			}
		});
	}

	private void relayStompMessage(RelaySession session, StompMessage stompMessage) throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("Forwarding: " + stompMessage);
		}
		byte[] bytes = converter.fromStompMessage(stompMessage);
		session.getOutputStream().write(bytes);
		session.getOutputStream().flush();
	}

	private RelaySession getRelaySession(String stompSessionId) {
		RelaySession session = RelayStompReactorService.this.relaySessions.get(stompSessionId);
		Assert.notNull(session, "RelaySession not found");
		return session;
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


	private final class ConnectConsumer implements Consumer<Event<StompMessage>> {

		@Override
		public void accept(Event<StompMessage> event) {

			StompMessage stompMessage = event.getData();
			final Object replyTo = event.getReplyTo();
			final String stompSessionId = stompMessage.getStompSessionId();

			final RelaySession session = new RelaySession();
			relaySessions.put(stompSessionId, session);

			try {
				Socket socket = SocketFactory.getDefault().createSocket("127.0.0.1", 61613);
				session.setSocket(socket);

				relayStompMessage(session, stompMessage);

				taskExecutor.execute(new RelayReadTask(stompSessionId, replyTo, session));
			}
			catch (Throwable t) {
				t.printStackTrace();
				clearRelaySession(stompSessionId);
			}
		}
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
						StompMessage message = RelayStompReactorService.this.converter.toStompMessage(bytes);
						RelayStompReactorService.this.reactor.notify(replyTo, Fn.event(message));
						out.reset();
					}
					else {
						out.write(b);
					}
				}
				logger.debug("Socket closed, STOMP session=" + stompSessionId);
				sendLostConnectionErrorMessage();
			}
			catch (IOException e) {
				e.printStackTrace();
				clearRelaySession(stompSessionId);
			}
		}

		private void sendLostConnectionErrorMessage() {
			StompHeaders headers = new StompHeaders();
			headers.setMessage("Lost connection");
			StompMessage errorMessage = new StompMessage(StompCommand.ERROR, headers);
			RelayStompReactorService.this.reactor.notify(replyTo, Fn.event(errorMessage));
		}
	}

	private class RelayConsumer implements Consumer<Event<StompMessage>> {

		@Override
		public void accept(Event<StompMessage> event) {
			StompMessage stompMessage = event.getData();
			RelaySession session = getRelaySession(stompMessage.getStompSessionId());
			try {
				relayStompMessage(session, stompMessage);
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				clearRelaySession(stompMessage.getStompSessionId());
			}
		}
	}

}
