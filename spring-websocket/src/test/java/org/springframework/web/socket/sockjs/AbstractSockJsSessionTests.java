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

package org.springframework.web.socket.sockjs;

import java.io.IOException;
import java.sql.Date;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;

import org.junit.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


/**
 * Test fixture for {@link AbstractSockJsSession}.
 *
 * @author Rossen Stoyanchev
 */
public class AbstractSockJsSessionTests extends BaseAbstractSockJsSessionTests<TestSockJsSession> {


	@Override
	protected TestSockJsSession initSockJsSession() {
		return new TestSockJsSession("1", this.sockJsConfig, this.webSocketHandler);
	}

	@Test
	public void getTimeSinceLastActive() throws Exception {

		Thread.sleep(1);

		long time1 = this.session.getTimeSinceLastActive();
		assertTrue(time1 > 0);

		Thread.sleep(1);

		long time2 = this.session.getTimeSinceLastActive();
		assertTrue(time2 > time1);

		this.session.delegateConnectionEstablished();

		Thread.sleep(1);

		this.session.setActive(false);
		assertTrue(this.session.getTimeSinceLastActive() > 0);

		this.session.setActive(true);
		assertEquals(0, this.session.getTimeSinceLastActive());
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
		this.session.delegateMessages(new String[] { msg1, msg2 });

		verify(this.webSocketHandler).handleMessage(this.session, new TextMessage(msg1));
		verify(this.webSocketHandler).handleMessage(this.session, new TextMessage(msg2));
		verifyNoMoreInteractions(this.webSocketHandler);
	}

	@Test
	public void delegateConnectionClosed() throws Exception {
		this.session.delegateConnectionEstablished();
		this.session.delegateConnectionClosed(CloseStatus.GOING_AWAY);

		assertClosed();
		assertEquals(1, this.session.getNumberOfLastActiveTimeUpdates());
		assertTrue(this.session.didCancelHeartbeat());
		verify(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.GOING_AWAY);
	}

	@Test
	public void closeWhenNotOpen() throws Exception {

		assertNew();

		this.session.close();
		assertNull("Close not ignored for a new session", this.session.getStatus());

		this.session.delegateConnectionEstablished();
		assertOpen();

		this.session.close();
		assertClosed();
		assertEquals(3000, this.session.getStatus().getCode());

		this.session.close(CloseStatus.SERVER_ERROR);
		assertEquals("Close should be ignored if already closed", 3000, this.session.getStatus().getCode());
	}

	@Test
	public void closeWhenNotActive() throws Exception {

		this.session.delegateConnectionEstablished();
		assertOpen();

		this.session.setActive(false);
		this.session.close();

		assertEquals(Collections.emptyList(), this.session.getSockJsFramesWritten());
	}

	@Test
	public void close() throws Exception {

		this.session.delegateConnectionEstablished();
		assertOpen();

		this.session.setActive(true);
		this.session.close();

		assertEquals(1, this.session.getSockJsFramesWritten().size());
		assertEquals(SockJsFrame.closeFrameGoAway(), this.session.getSockJsFramesWritten().get(0));

		assertEquals(1, this.session.getNumberOfLastActiveTimeUpdates());
		assertTrue(this.session.didCancelHeartbeat());

		assertEquals(new CloseStatus(3000, "Go away!"), this.session.getStatus());
		assertClosed();
		verify(this.webSocketHandler).afterConnectionClosed(this.session, new CloseStatus(3000, "Go away!"));
	}

	@Test
	public void closeWithWriteFrameExceptions() throws Exception {

		this.session.setExceptionOnWriteFrame(new IOException());

		this.session.delegateConnectionEstablished();
		this.session.setActive(true);
		this.session.close();

		assertEquals(new CloseStatus(3000, "Go away!"), this.session.getStatus());
		assertClosed();
	}

	@Test
	public void closeWithWebSocketHandlerExceptions() throws Exception {

		doThrow(new Exception()).when(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.NORMAL);

		this.session.delegateConnectionEstablished();
		this.session.setActive(true);
		this.session.close(CloseStatus.NORMAL);

		assertEquals(CloseStatus.NORMAL, this.session.getStatus());
		assertClosed();
	}

	@Test
	public void writeFrame() throws Exception {
		this.session.writeFrame(SockJsFrame.openFrame());

		assertEquals(1, this.session.getSockJsFramesWritten().size());
		assertEquals(SockJsFrame.openFrame(), this.session.getSockJsFramesWritten().get(0));
	}

	@Test
	public void writeFrameIoException() throws Exception {
		this.session.setExceptionOnWriteFrame(new IOException());
		this.session.delegateConnectionEstablished();
		try {
			this.session.writeFrame(SockJsFrame.openFrame());
			fail("expected exception");
		}
		catch (IOException ex) {
			assertEquals(CloseStatus.SERVER_ERROR, this.session.getStatus());
			verify(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.SERVER_ERROR);
		}
	}

	@Test
	public void writeFrameThrowable() throws Exception {
		this.session.setExceptionOnWriteFrame(new NullPointerException());
		this.session.delegateConnectionEstablished();
		try {
			this.session.writeFrame(SockJsFrame.openFrame());
			fail("expected exception");
		}
		catch (SockJsRuntimeException ex) {
			assertEquals(CloseStatus.SERVER_ERROR, this.session.getStatus());
			verify(this.webSocketHandler).afterConnectionClosed(this.session, CloseStatus.SERVER_ERROR);
		}
	}

	@Test
	public void sendHeartbeatWhenNotActive() throws Exception {
		this.session.setActive(false);
		this.session.sendHeartbeat();

		assertEquals(Collections.emptyList(), this.session.getSockJsFramesWritten());
	}

	@Test
	public void sendHeartbeat() throws Exception {
		this.session.setActive(true);
		this.session.sendHeartbeat();

		assertEquals(1, this.session.getSockJsFramesWritten().size());
		assertEquals(SockJsFrame.heartbeatFrame(), this.session.getSockJsFramesWritten().get(0));

		verify(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));
		verifyNoMoreInteractions(this.taskScheduler);
	}

	@Test
	public void scheduleHeartbeatNotActive() throws Exception {
		this.session.setActive(false);
		this.session.scheduleHeartbeat();

		verifyNoMoreInteractions(this.taskScheduler);
	}

	@Test
	public void scheduleAndCancelHeartbeat() throws Exception {

		ScheduledFuture<?> task = mock(ScheduledFuture.class);
		doReturn(task).when(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));

		this.session.setActive(true);
		this.session.scheduleHeartbeat();

		verify(this.taskScheduler).schedule(any(Runnable.class), any(Date.class));
		verifyNoMoreInteractions(this.taskScheduler);

		doReturn(false).when(task).isDone();

		this.session.cancelHeartbeat();

		verify(task).isDone();
		verify(task).cancel(false);
		verifyNoMoreInteractions(task);
	}

}
