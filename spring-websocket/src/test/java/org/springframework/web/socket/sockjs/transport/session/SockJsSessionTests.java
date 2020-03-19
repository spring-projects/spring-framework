/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport.session;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.handler.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.sockjs.SockJsMessageDeliveryException;
import org.springframework.web.socket.sockjs.SockJsTransportFailureException;
import org.springframework.web.socket.sockjs.frame.SockJsFrame;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test fixture for {@link AbstractSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class SockJsSessionTests extends AbstractSockJsSessionTests<TestSockJsSession> {


	@Override
	protected TestSockJsSession initSockJsSession() {
		return new TestSockJsSession("1", this.sockJsConfig, this.webSocketHandler, Collections.emptyMap());
	}


	@Test
	public void getTimeSinceLastActive() throws Exception {
		Thread.sleep(1);

		long time1 = this.session.getTimeSinceLastActive();
		assertThat(time1 > 0).isTrue();

		Thread.sleep(1);

		long time2 = this.session.getTimeSinceLastActive();
		assertThat(time2 > time1).isTrue();

		this.session.delegateConnectionEstablished();

		Thread.sleep(1);

		this.session.setActive(false);
		assertThat(this.session.getTimeSinceLastActive() > 0).isTrue();

		this.session.setActive(true);
		assertThat(this.session.getTimeSinceLastActive()).isEqualTo(0);
	}

	@Test
	public void delegateConnectionEstablished() throws Exception {
		assertNew();
		this.session.delegateConnectionEstablished();
		assertOpen();
		verify(this.webSocketHandler).afterConnectionEstablished(this.session);
	}

	@Test
	public void delegateError() throws Exception {
		Exception ex = new Exception();
		this.session.delegateError(ex);
		verify(this.webSocketHandler).handleTransportError(this.session, ex);
	}

	@Test
	public void delegateMessages() throws Exception {

		String msg1 = "message 1";
		String msg2 = "message 2";

		this.session.delegateMessages(msg1, msg2);

		verify(this.webSocketHandler).handleMessage(this.session, new TextMessage(msg1));
		verify(this.webSocketHandler).handleMessage(this.session, new TextMessage(msg2));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void delegateMessagesWithError() throws Exception {

		TestSockJsSession session = new TestSockJsSession("1", this.sockJsConfig,
				new ExceptionWebSocketHandlerDecorator(this.webSocketHandler), Collections.emptyMap());

		String msg1 = "message 1";
		String msg2 = "message 2";
		String msg3 = "message 3";

		willThrow(new IOException()).given(this.webSocketHandler).handleMessage(session, new TextMessage(msg2));

		session.delegateConnectionEstablished();

		assertThatExceptionOfType(SockJsMessageDeliveryException.class)
				.isThrownBy(() -> session.delegateMessages(msg1, msg2, msg3))
				.satisfies(ex -> assertThat(ex.getUndeliveredMessages()).containsExactly(msg3));

		verify(this.webSocketHandler).afterConnectionEstablished(session);
		verify(this.webSocketHandler).handleMessage(session, new TextMessage(msg1));
		verify(this.webSocketHandler).handleMessage(session, new TextMessage(msg2));
		verify(this.webSocketHandler).afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test // gh-23828
	public void delegateMessagesEmptyAfterConnectionClosed() throws Exception {

		TestSockJsSession session = new TestSockJsSession("1", this.sockJsConfig,
				new ExceptionWebSocketHandlerDecorator(this.webSocketHandler), Collections.emptyMap());

		session.delegateConnectionEstablished();
		session.close(CloseStatus.NORMAL);
		session.delegateMessages("", " ", "\n");

		// No exception for empty messages

		verify(this.webSocketHandler).afterConnectionEstablished(session);
		verify(this.webSocketHandler).afterConnectionClosed(session, CloseStatus.NORMAL);
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void delegateConnectionClosed() throws Exception {
		this.session.delegateConnectionEstablished();
		this.session.delegateConnectionClosed(CloseStatus.GOING_AWAY);

		assertClosed();
		assertThat(this.session.getNumberOfLastActiveTimeUpdates()).isEqualTo(1);
		verify(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.GOING_AWAY);
	}

	@Test
	public void closeWhenNotOpen() throws Exception {
		assertNew();

		this.session.close();
		assertThat(this.session.getCloseStatus()).as("Close not ignored for a new session").isNull();

		this.session.delegateConnectionEstablished();
		assertOpen();

		this.session.close();
		assertClosed();
		assertThat(this.session.getCloseStatus().getCode()).isEqualTo(3000);

		this.session.close(CloseStatus.SERVER_ERROR);
		assertThat(this.session.getCloseStatus().getCode()).as("Should ignore close if already closed").isEqualTo(3000);
	}

	@Test
	public void closeWhenNotActive() throws Exception {
		this.session.delegateConnectionEstablished();
		assertOpen();

		this.session.setActive(false);
		this.session.close();

		assertThat(this.session.getSockJsFramesWritten()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void close() throws Exception {
		this.session.delegateConnectionEstablished();
		assertOpen();

		this.session.setActive(true);
		this.session.close();

		assertThat(this.session.getSockJsFramesWritten().size()).isEqualTo(1);
		assertThat(this.session.getSockJsFramesWritten().get(0)).isEqualTo(SockJsFrame.closeFrameGoAway());

		assertThat(this.session.getNumberOfLastActiveTimeUpdates()).isEqualTo(1);
		assertThat(this.session.didCancelHeartbeat()).isTrue();

		assertThat(this.session.getCloseStatus()).isEqualTo(new CloseStatus(3000, "Go away!"));
		assertClosed();
		verify(this.webSocketHandler).afterConnectionClosed(this.session, new CloseStatus(3000, "Go away!"));
	}

	@Test
	public void closeWithWriteFrameExceptions() throws Exception {
		this.session.setExceptionOnWrite(new IOException());

		this.session.delegateConnectionEstablished();
		this.session.setActive(true);
		this.session.close();

		assertThat(this.session.getCloseStatus()).isEqualTo(new CloseStatus(3000, "Go away!"));
		assertClosed();
	}

	@Test
	public void closeWithWebSocketHandlerExceptions() throws Exception {
		willThrow(new Exception()).given(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.NORMAL);

		this.session.delegateConnectionEstablished();
		this.session.setActive(true);
		this.session.close(CloseStatus.NORMAL);

		assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.NORMAL);
		assertClosed();
	}

	@Test
	public void tryCloseWithWebSocketHandlerExceptions() throws Exception {
		this.session.delegateConnectionEstablished();
		this.session.setActive(true);
		this.session.tryCloseWithSockJsTransportError(new Exception(), CloseStatus.BAD_DATA);

		assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.BAD_DATA);
		assertClosed();
	}

	@Test
	public void writeFrame() {
		this.session.writeFrame(SockJsFrame.openFrame());

		assertThat(this.session.getSockJsFramesWritten().size()).isEqualTo(1);
		assertThat(this.session.getSockJsFramesWritten().get(0)).isEqualTo(SockJsFrame.openFrame());
	}

	@Test
	public void writeFrameIoException() throws Exception {
		this.session.setExceptionOnWrite(new IOException());
		this.session.delegateConnectionEstablished();

		assertThatExceptionOfType(SockJsTransportFailureException.class).isThrownBy(() ->
				this.session.writeFrame(SockJsFrame.openFrame()));
		assertThat(this.session.getCloseStatus()).isEqualTo(CloseStatus.SERVER_ERROR);
		verify(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.SERVER_ERROR);
	}

	@Test
	public void sendHeartbeat() {
		this.session.setActive(true);
		this.session.sendHeartbeat();

		assertThat(this.session.getSockJsFramesWritten().size()).isEqualTo(1);
		assertThat(this.session.getSockJsFramesWritten().get(0)).isEqualTo(SockJsFrame.heartbeatFrame());

		verify(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));
		verifyNoMoreInteractions(this.taskScheduler);
	}

	@Test
	public void scheduleHeartbeatNotActive() {
		this.session.setActive(false);
		this.session.scheduleHeartbeat();

		verifyNoMoreInteractions(this.taskScheduler);
	}

	@Test
	public void sendHeartbeatWhenDisabled() {
		this.session.disableHeartbeat();
		this.session.setActive(true);
		this.session.sendHeartbeat();

		assertThat(this.session.getSockJsFramesWritten()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void scheduleAndCancelHeartbeat() {
		ScheduledFuture<?> task = mock(ScheduledFuture.class);
		willReturn(task).given(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));

		this.session.setActive(true);
		this.session.scheduleHeartbeat();

		verify(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));
		verifyNoMoreInteractions(this.taskScheduler);

		given(task.isCancelled()).willReturn(false);
		given(task.cancel(false)).willReturn(true);

		this.session.cancelHeartbeat();

		verify(task).cancel(false);
		verifyNoMoreInteractions(task);
	}

}
