/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.TestPrincipal;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SimpleBrokerMessageHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@SuppressWarnings("unchecked")
public class SimpleBrokerMessageHandlerTests {

	private SimpleBrokerMessageHandler messageHandler;

	@Mock
	private SubscribableChannel clientInboundChannel;

	@Mock
	private MessageChannel clientOutboundChannel;

	@Mock
	private SubscribableChannel brokerChannel;

	@Mock
	private TaskScheduler taskScheduler;

	@Captor
	ArgumentCaptor<Message<?>> messageCaptor;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.messageHandler = new SimpleBrokerMessageHandler(this.clientInboundChannel,
				this.clientOutboundChannel, this.brokerChannel, Collections.emptyList());
	}


	@Test
	public void subscribePublish() {
		this.messageHandler.start();

		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub3", "/bar"));

		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub3", "/bar"));

		this.messageHandler.handleMessage(createMessage("/foo", "message1"));
		this.messageHandler.handleMessage(createMessage("/bar", "message2"));

		verify(this.clientOutboundChannel, times(6)).send(this.messageCaptor.capture());
		assertTrue(messageCaptured("sess1", "sub1", "/foo"));
		assertTrue(messageCaptured("sess1", "sub2", "/foo"));
		assertTrue(messageCaptured("sess2", "sub1", "/foo"));
		assertTrue(messageCaptured("sess2", "sub2", "/foo"));
		assertTrue(messageCaptured("sess1", "sub3", "/bar"));
		assertTrue(messageCaptured("sess2", "sub3", "/bar"));
	}

	@Test
	public void subscribeDisconnectPublish() {
		String sess1 = "sess1";
		String sess2 = "sess2";

		this.messageHandler.start();

		this.messageHandler.handleMessage(createSubscriptionMessage(sess1, "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess1, "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess1, "sub3", "/bar"));

		this.messageHandler.handleMessage(createSubscriptionMessage(sess2, "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess2, "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage(sess2, "sub3", "/bar"));

		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT);
		headers.setSessionId(sess1);
		headers.setUser(new TestPrincipal("joe"));
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
		this.messageHandler.handleMessage(message);

		this.messageHandler.handleMessage(createMessage("/foo", "message1"));
		this.messageHandler.handleMessage(createMessage("/bar", "message2"));

		verify(this.clientOutboundChannel, times(4)).send(this.messageCaptor.capture());

		Message<?> captured = this.messageCaptor.getAllValues().get(0);
		assertEquals(SimpMessageType.DISCONNECT_ACK, SimpMessageHeaderAccessor.getMessageType(captured.getHeaders()));
		assertSame(message, captured.getHeaders().get(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER));
		assertEquals(sess1, SimpMessageHeaderAccessor.getSessionId(captured.getHeaders()));
		assertEquals("joe", SimpMessageHeaderAccessor.getUser(captured.getHeaders()).getName());

		assertTrue(messageCaptured(sess2, "sub1", "/foo"));
		assertTrue(messageCaptured(sess2, "sub2", "/foo"));
		assertTrue(messageCaptured(sess2, "sub3", "/bar"));
	}

	@Test
	public void connect() {
		this.messageHandler.start();

		String id = "sess1";
		Message<String> connectMessage = createConnectMessage(id, new TestPrincipal("joe"), null);
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.handleMessage(connectMessage);

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		Message<?> connectAckMessage = this.messageCaptor.getValue();

		SimpMessageHeaderAccessor connectAckHeaders = SimpMessageHeaderAccessor.wrap(connectAckMessage);
		assertEquals(connectMessage, connectAckHeaders.getHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER));
		assertEquals(id, connectAckHeaders.getSessionId());
		assertEquals("joe", connectAckHeaders.getUser().getName());
		assertArrayEquals(new long[] {10000, 10000},
				SimpMessageHeaderAccessor.getHeartbeat(connectAckHeaders.getMessageHeaders()));
	}

	@Test
	public void heartbeatValueWithAndWithoutTaskScheduler() {
		assertNull(this.messageHandler.getHeartbeatValue());
		this.messageHandler.setTaskScheduler(this.taskScheduler);

		assertNotNull(this.messageHandler.getHeartbeatValue());
		assertArrayEquals(new long[] {10000, 10000}, this.messageHandler.getHeartbeatValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void startWithHeartbeatValueWithoutTaskScheduler() {
		this.messageHandler.setHeartbeatValue(new long[] {10000, 10000});
		this.messageHandler.start();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void startAndStopWithHeartbeatValue() {
		ScheduledFuture future = mock(ScheduledFuture.class);
		when(this.taskScheduler.scheduleWithFixedDelay(any(Runnable.class), eq(15000L))).thenReturn(future);

		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.setHeartbeatValue(new long[] {15000, 16000});
		this.messageHandler.start();

		verify(this.taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(15000L));
		verifyNoMoreInteractions(this.taskScheduler, future);

		this.messageHandler.stop();

		verify(future).cancel(true);
		verifyNoMoreInteractions(future);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void startWithOneZeroHeartbeatValue() {
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.setHeartbeatValue(new long[] {0, 10000});
		this.messageHandler.start();

		verify(this.taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(10000L));
	}

	@Test
	public void readInactivity() throws Exception {
		this.messageHandler.setHeartbeatValue(new long[] {0, 1});
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.start();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(), eq(1L));
		Runnable heartbeatTask = taskCaptor.getValue();
		assertNotNull(heartbeatTask);

		String id = "sess1";
		TestPrincipal user = new TestPrincipal("joe");
		Message<String> connectMessage = createConnectMessage(id, user, new long[] {1, 0});
		this.messageHandler.handleMessage(connectMessage);

		Thread.sleep(10);
		heartbeatTask.run();

		verify(this.clientOutboundChannel, atLeast(2)).send(this.messageCaptor.capture());
		List<Message<?>> messages = this.messageCaptor.getAllValues();
		assertEquals(2, messages.size());

		MessageHeaders headers = messages.get(0).getHeaders();
		assertEquals(SimpMessageType.CONNECT_ACK, headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER));
		headers = messages.get(1).getHeaders();
		assertEquals(SimpMessageType.DISCONNECT_ACK, headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER));
		assertEquals(id, headers.get(SimpMessageHeaderAccessor.SESSION_ID_HEADER));
		assertEquals(user, headers.get(SimpMessageHeaderAccessor.USER_HEADER));
	}

	@Test
	public void writeInactivity() throws Exception {
		this.messageHandler.setHeartbeatValue(new long[] {1, 0});
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.start();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(), eq(1L));
		Runnable heartbeatTask = taskCaptor.getValue();
		assertNotNull(heartbeatTask);

		String id = "sess1";
		TestPrincipal user = new TestPrincipal("joe");
		Message<String> connectMessage = createConnectMessage(id, user, new long[] {0, 1});
		this.messageHandler.handleMessage(connectMessage);

		Thread.sleep(10);
		heartbeatTask.run();

		verify(this.clientOutboundChannel, times(2)).send(this.messageCaptor.capture());
		List<Message<?>> messages = this.messageCaptor.getAllValues();
		assertEquals(2, messages.size());

		MessageHeaders headers = messages.get(0).getHeaders();
		assertEquals(SimpMessageType.CONNECT_ACK, headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER));
		headers = messages.get(1).getHeaders();
		assertEquals(SimpMessageType.HEARTBEAT, headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER));
		assertEquals(id, headers.get(SimpMessageHeaderAccessor.SESSION_ID_HEADER));
		assertEquals(user, headers.get(SimpMessageHeaderAccessor.USER_HEADER));
	}

	@Test
	public void readWriteIntervalCalculation() throws Exception {
		this.messageHandler.setHeartbeatValue(new long[] {1, 1});
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.start();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(), eq(1L));
		Runnable heartbeatTask = taskCaptor.getValue();
		assertNotNull(heartbeatTask);

		String id = "sess1";
		TestPrincipal user = new TestPrincipal("joe");
		Message<String> connectMessage = createConnectMessage(id, user, new long[] {10000, 10000});
		this.messageHandler.handleMessage(connectMessage);

		Thread.sleep(10);
		heartbeatTask.run();

		verify(this.clientOutboundChannel, times(1)).send(this.messageCaptor.capture());
		List<Message<?>> messages = this.messageCaptor.getAllValues();
		assertEquals(1, messages.size());
		assertEquals(SimpMessageType.CONNECT_ACK,
				messages.get(0).getHeaders().get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER));
	}


	private Message<String> createSubscriptionMessage(String sessionId, String subscriptionId, String destination) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.SUBSCRIBE);
		headers.setSubscriptionId(subscriptionId);
		headers.setDestination(destination);
		headers.setSessionId(sessionId);
		return MessageBuilder.createMessage("", headers.getMessageHeaders());
	}

	private Message<String> createConnectMessage(String sessionId, Principal user, long[] heartbeat) {
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
		accessor.setSessionId(sessionId);
		accessor.setUser(user);
		accessor.setHeader(SimpMessageHeaderAccessor.HEART_BEAT_HEADER, heartbeat);
		return MessageBuilder.createMessage("", accessor.getMessageHeaders());
	}

	private Message<String> createMessage(String destination, String payload) {
		SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
		headers.setDestination(destination);
		return MessageBuilder.createMessage(payload, headers.getMessageHeaders());
	}

	private boolean messageCaptured(String sessionId, String subscriptionId, String destination) {
		for (Message<?> message : this.messageCaptor.getAllValues()) {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			if (sessionId.equals(headers.getSessionId())) {
				if (subscriptionId.equals(headers.getSubscriptionId())) {
					if (destination.equals(headers.getDestination())) {
						return true;
					}
				}
			}
		}
		return false;
	}

}
