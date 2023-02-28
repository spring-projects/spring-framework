/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.messaging.simp.broker;

import java.security.Principal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.testfixture.security.TestPrincipal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link SimpleBrokerMessageHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class SimpleBrokerMessageHandlerTests {

	private SimpleBrokerMessageHandler messageHandler;


	@Mock
	private SubscribableChannel clientInChannel;

	@Mock
	private MessageChannel clientOutChannel;

	@Mock
	private SubscribableChannel brokerChannel;

	@Mock
	private TaskScheduler taskScheduler;

	@Captor
	ArgumentCaptor<Message<?>> messageCaptor;


	@BeforeEach
	public void setup() {
		this.messageHandler = new SimpleBrokerMessageHandler(
				this.clientInChannel, this.clientOutChannel, this.brokerChannel, Collections.emptyList());
	}


	@Test
	public void subscribePublish() {
		startSession("sess1");
		startSession("sess2");

		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess1", "sub3", "/bar"));

		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub1", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub2", "/foo"));
		this.messageHandler.handleMessage(createSubscriptionMessage("sess2", "sub3", "/bar"));

		this.messageHandler.handleMessage(createMessage("/foo", "message1"));
		this.messageHandler.handleMessage(createMessage("/bar", "message2"));

		verify(this.clientOutChannel, times(6)).send(this.messageCaptor.capture());
		assertThat(messageCaptured("sess1", "sub1", "/foo")).isTrue();
		assertThat(messageCaptured("sess1", "sub2", "/foo")).isTrue();
		assertThat(messageCaptured("sess2", "sub1", "/foo")).isTrue();
		assertThat(messageCaptured("sess2", "sub2", "/foo")).isTrue();
		assertThat(messageCaptured("sess1", "sub3", "/bar")).isTrue();
		assertThat(messageCaptured("sess2", "sub3", "/bar")).isTrue();
	}

	@Test
	public void subscribeDisconnectPublish() {
		String sess1 = "sess1";
		String sess2 = "sess2";

		startSession(sess1);
		startSession(sess2);

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

		verify(this.clientOutChannel, times(4)).send(this.messageCaptor.capture());

		Message<?> captured = this.messageCaptor.getAllValues().get(2);
		assertThat(SimpMessageHeaderAccessor.getMessageType(captured.getHeaders())).isEqualTo(SimpMessageType.DISCONNECT_ACK);
		assertThat(captured.getHeaders().get(SimpMessageHeaderAccessor.DISCONNECT_MESSAGE_HEADER)).isSameAs(message);
		assertThat(SimpMessageHeaderAccessor.getSessionId(captured.getHeaders())).isEqualTo(sess1);
		assertThat(SimpMessageHeaderAccessor.getUser(captured.getHeaders()).getName()).isEqualTo("joe");

		assertThat(messageCaptured(sess2, "sub1", "/foo")).isTrue();
		assertThat(messageCaptured(sess2, "sub2", "/foo")).isTrue();
		assertThat(messageCaptured(sess2, "sub3", "/bar")).isTrue();
	}

	@Test
	public void connect() {
		String id = "sess1";

		Message<String> connectMessage = startSession(id);
		Message<?> connectAckMessage = this.messageCaptor.getValue();

		SimpMessageHeaderAccessor connectAckHeaders = SimpMessageHeaderAccessor.wrap(connectAckMessage);
		assertThat(connectAckHeaders.getHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER)).isEqualTo(connectMessage);
		assertThat(connectAckHeaders.getSessionId()).isEqualTo(id);
		assertThat(connectAckHeaders.getUser().getName()).isEqualTo("joe");
		assertThat(SimpMessageHeaderAccessor.getHeartbeat(connectAckHeaders.getMessageHeaders())).isEqualTo(new long[] {10000, 10000});
	}

	@Test
	public void heartbeatValueWithAndWithoutTaskScheduler() {
		assertThat(this.messageHandler.getHeartbeatValue()).isNull();
		this.messageHandler.setTaskScheduler(this.taskScheduler);

		assertThat(this.messageHandler.getHeartbeatValue()).isNotNull();
		assertThat(this.messageHandler.getHeartbeatValue()).isEqualTo(new long[] {10000, 10000});
	}

	@Test
	public void startWithHeartbeatValueWithoutTaskScheduler() {
		this.messageHandler.setHeartbeatValue(new long[] {10000, 10000});
		assertThatIllegalArgumentException().isThrownBy(
				this.messageHandler::start);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void startAndStopWithHeartbeatValue() {
		ScheduledFuture future = mock();
		given(this.taskScheduler.scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofMillis(15000)))).willReturn(future);

		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.setHeartbeatValue(new long[] {15000, 16000});
		this.messageHandler.start();

		verify(this.taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofMillis(15000)));
		verifyNoMoreInteractions(this.taskScheduler, future);

		this.messageHandler.stop();

		verify(future).cancel(true);
		verifyNoMoreInteractions(future);
	}

	@Test
	public void startWithOneZeroHeartbeatValue() {
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.setHeartbeatValue(new long[] {0, 10000});
		this.messageHandler.start();

		verify(this.taskScheduler).scheduleWithFixedDelay(any(Runnable.class), eq(Duration.ofMillis(10000)));
	}

	@Test
	public void readInactivity() throws Exception {
		this.messageHandler.setHeartbeatValue(new long[] {0, 1});
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.start();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(), eq(Duration.ofMillis(1)));
		Runnable heartbeatTask = taskCaptor.getValue();
		assertThat(heartbeatTask).isNotNull();

		String id = "sess1";
		TestPrincipal user = new TestPrincipal("joe");
		Message<String> connectMessage = createConnectMessage(id, user, new long[] {1, 0});
		this.messageHandler.handleMessage(connectMessage);

		Thread.sleep(10);
		heartbeatTask.run();

		verify(this.clientOutChannel, atLeast(2)).send(this.messageCaptor.capture());
		List<Message<?>> messages = this.messageCaptor.getAllValues();
		assertThat(messages).hasSize(2);

		MessageHeaders headers = messages.get(0).getHeaders();
		assertThat(headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER)).isEqualTo(SimpMessageType.CONNECT_ACK);
		headers = messages.get(1).getHeaders();
		assertThat(headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER)).isEqualTo(SimpMessageType.DISCONNECT_ACK);
		assertThat(headers.get(SimpMessageHeaderAccessor.SESSION_ID_HEADER)).isEqualTo(id);
		assertThat(headers.get(SimpMessageHeaderAccessor.USER_HEADER)).isEqualTo(user);
	}

	@Test
	public void writeInactivity() throws Exception {
		this.messageHandler.setHeartbeatValue(new long[] {1, 0});
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.start();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(), eq(Duration.ofMillis(1)));
		Runnable heartbeatTask = taskCaptor.getValue();
		assertThat(heartbeatTask).isNotNull();

		String id = "sess1";
		TestPrincipal user = new TestPrincipal("joe");
		Message<String> connectMessage = createConnectMessage(id, user, new long[] {0, 1});
		this.messageHandler.handleMessage(connectMessage);

		Thread.sleep(10);
		heartbeatTask.run();

		verify(this.clientOutChannel, times(2)).send(this.messageCaptor.capture());
		List<Message<?>> messages = this.messageCaptor.getAllValues();
		assertThat(messages).hasSize(2);

		MessageHeaders headers = messages.get(0).getHeaders();
		assertThat(headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER)).isEqualTo(SimpMessageType.CONNECT_ACK);
		headers = messages.get(1).getHeaders();
		assertThat(headers.get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER)).isEqualTo(SimpMessageType.HEARTBEAT);
		assertThat(headers.get(SimpMessageHeaderAccessor.SESSION_ID_HEADER)).isEqualTo(id);
		assertThat(headers.get(SimpMessageHeaderAccessor.USER_HEADER)).isEqualTo(user);
	}

	@Test
	public void readWriteIntervalCalculation() throws Exception {
		this.messageHandler.setHeartbeatValue(new long[] {1, 1});
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.start();

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.taskScheduler).scheduleWithFixedDelay(taskCaptor.capture(), eq(Duration.ofMillis(1)));
		Runnable heartbeatTask = taskCaptor.getValue();
		assertThat(heartbeatTask).isNotNull();

		String id = "sess1";
		TestPrincipal user = new TestPrincipal("joe");
		Message<String> connectMessage = createConnectMessage(id, user, new long[] {10000, 10000});
		this.messageHandler.handleMessage(connectMessage);

		Thread.sleep(10);
		heartbeatTask.run();

		verify(this.clientOutChannel, times(1)).send(this.messageCaptor.capture());
		List<Message<?>> messages = this.messageCaptor.getAllValues();
		assertThat(messages).hasSize(1);
		assertThat(messages.get(0).getHeaders().get(SimpMessageHeaderAccessor.MESSAGE_TYPE_HEADER)).isEqualTo(SimpMessageType.CONNECT_ACK);
	}


	private Message<String> startSession(String id) {
		this.messageHandler.start();

		Message<String> connectMessage = createConnectMessage(id, new TestPrincipal("joe"), null);
		this.messageHandler.setTaskScheduler(this.taskScheduler);
		this.messageHandler.handleMessage(connectMessage);

		verify(this.clientOutChannel, times(1)).send(this.messageCaptor.capture());
		reset(this.clientOutChannel);
		return connectMessage;
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
