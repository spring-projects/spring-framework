/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompSession.Receiptable;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link DefaultStompSession}.
 *
 * @author Rossen Stoyanchev
 */
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultStompSessionTests {

	private DefaultStompSession session;

	private StompHeaders connectHeaders;


	@Mock
	private StompSessionHandler sessionHandler;

	@Mock
	private TcpConnection<byte[]> connection;

	@Captor
	private ArgumentCaptor<Message<byte[]>> messageCaptor;


	@BeforeEach
	public void setUp() {
		this.connectHeaders = new StompHeaders();
		this.session = new DefaultStompSession(this.sessionHandler, this.connectHeaders);
		this.session.setMessageConverter(
				new CompositeMessageConverter(
						Arrays.asList(new StringMessageConverter(), new ByteArrayMessageConverter())));

		SettableListenableFuture<Void> future = new SettableListenableFuture<>();
		future.set(null);
		given(this.connection.send(this.messageCaptor.capture())).willReturn(future);
	}


	@Test
	public void afterConnected() {
		assertThat(this.session.isConnected()).isFalse();
		this.connectHeaders.setHost("my-host");
		this.connectHeaders.setHeartbeat(new long[] {11, 12});

		this.session.afterConnected(this.connection);

		assertThat(this.session.isConnected()).isTrue();
		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(accessor.getHost()).isEqualTo("my-host");
		assertThat(accessor.getAcceptVersion()).containsExactly("1.1", "1.2");
		assertThat(accessor.getHeartbeat()).isEqualTo(new long[] {11, 12});
	}

	@Test // SPR-16844
	public void afterConnectedWithSpecificVersion() {
		assertThat(this.session.isConnected()).isFalse();
		this.connectHeaders.setAcceptVersion("1.1");

		this.session.afterConnected(this.connection);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.CONNECT);
		assertThat(accessor.getAcceptVersion()).containsExactly("1.1");
	}

	@Test
	public void afterConnectFailure() {
		IllegalStateException exception = new IllegalStateException("simulated exception");
		this.session.afterConnectFailure(exception);
		verify(this.sessionHandler).handleTransportError(this.session, exception);
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void handleConnectedFrame() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		this.connectHeaders.setHeartbeat(new long[] {10000, 10000});

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setVersion("1.2");
		accessor.setHeartbeat(10000, 10000);
		accessor.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		verify(this.sessionHandler).afterConnected(this.session, stompHeaders);
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void heartbeatValues() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		this.connectHeaders.setHeartbeat(new long[] {10000, 10000});

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setVersion("1.2");
		accessor.setHeartbeat(20000, 20000);
		accessor.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		ArgumentCaptor<Long> writeInterval = ArgumentCaptor.forClass(Long.class);
		verify(this.connection).onWriteInactivity(any(Runnable.class), writeInterval.capture());
		assertThat((long) writeInterval.getValue()).isEqualTo(20000);

		ArgumentCaptor<Long> readInterval = ArgumentCaptor.forClass(Long.class);
		verify(this.connection).onReadInactivity(any(Runnable.class), readInterval.capture());
		assertThat((long) readInterval.getValue()).isEqualTo(60000);
	}

	@Test
	public void heartbeatNotSupportedByServer() {
		this.session.afterConnected(this.connection);
		verify(this.connection).send(any());

		this.connectHeaders.setHeartbeat(new long[] {10000, 10000});

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setVersion("1.2");
		accessor.setHeartbeat(0, 0);
		accessor.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		verifyNoMoreInteractions(this.connection);
	}

	@Test
	public void heartbeatTasks() {
		this.session.afterConnected(this.connection);
		verify(this.connection).send(any());

		this.connectHeaders.setHeartbeat(new long[] {10000, 10000});

		StompHeaderAccessor connected = StompHeaderAccessor.create(StompCommand.CONNECTED);
		connected.setVersion("1.2");
		connected.setHeartbeat(10000, 10000);
		connected.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], connected.getMessageHeaders()));

		ArgumentCaptor<Runnable> writeTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
		ArgumentCaptor<Runnable> readTaskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(this.connection).onWriteInactivity(writeTaskCaptor.capture(), any(Long.class));
		verify(this.connection).onReadInactivity(readTaskCaptor.capture(), any(Long.class));

		Runnable writeTask = writeTaskCaptor.getValue();
		Runnable readTask = readTaskCaptor.getValue();
		assertThat(writeTask).isNotNull();
		assertThat(readTask).isNotNull();

		writeTask.run();
		StompHeaderAccessor accessor = StompHeaderAccessor.createForHeartbeat();
		Message<byte[]> message = MessageBuilder.createMessage(new byte[] {'\n'}, accessor.getMessageHeaders());
		verify(this.connection).send(eq(message));
		verifyNoMoreInteractions(this.connection);

		reset(this.sessionHandler);
		readTask.run();
		verify(this.sessionHandler).handleTransportError(same(this.session), any(IllegalStateException.class));
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void handleErrorFrame() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
		accessor.setContentType(new MimeType("text", "plain", StandardCharsets.UTF_8));
		accessor.addNativeHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		String payload = "Oops";

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		given(this.sessionHandler.getPayloadType(stompHeaders)).willReturn(String.class);

		this.session.handleMessage(MessageBuilder.createMessage(
				payload.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders()));

		verify(this.sessionHandler).getPayloadType(stompHeaders);
		verify(this.sessionHandler).handleFrame(stompHeaders, payload);
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void handleErrorFrameWithEmptyPayload() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
		accessor.addNativeHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		verify(this.sessionHandler).handleFrame(stompHeaders, null);
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void handleErrorFrameWithConversionException() {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
		accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
		accessor.addNativeHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		byte[] payload = "{'foo':'bar'}".getBytes(StandardCharsets.UTF_8);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		given(this.sessionHandler.getPayloadType(stompHeaders)).willReturn(Map.class);

		this.session.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		verify(this.sessionHandler).getPayloadType(stompHeaders);
		verify(this.sessionHandler).handleException(same(this.session), same(StompCommand.ERROR),
				eq(stompHeaders), same(payload), any(MessageConversionException.class));
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void handleMessageFrame() {
		this.session.afterConnected(this.connection);

		StompFrameHandler frameHandler = mock(StompFrameHandler.class);
		String destination = "/topic/foo";
		Subscription subscription = this.session.subscribe(destination, frameHandler);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setDestination(destination);
		accessor.setSubscriptionId(subscription.getSubscriptionId());
		accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);
		accessor.setMessageId("1");
		accessor.setLeaveMutable(true);
		String payload = "sample payload";

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		given(frameHandler.getPayloadType(stompHeaders)).willReturn(String.class);

		this.session.handleMessage(MessageBuilder.createMessage(payload.getBytes(StandardCharsets.UTF_8),
				accessor.getMessageHeaders()));

		verify(frameHandler).getPayloadType(stompHeaders);
		verify(frameHandler).handleFrame(stompHeaders, payload);
		verifyNoMoreInteractions(frameHandler);
	}

	@Test
	public void handleMessageFrameWithConversionException() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		StompFrameHandler frameHandler = mock(StompFrameHandler.class);
		String destination = "/topic/foo";
		Subscription subscription = this.session.subscribe(destination, frameHandler);

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
		accessor.setDestination(destination);
		accessor.setSubscriptionId(subscription.getSubscriptionId());
		accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
		accessor.setMessageId("1");
		accessor.setLeaveMutable(true);
		byte[] payload = "{'foo':'bar'}".getBytes(StandardCharsets.UTF_8);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		given(frameHandler.getPayloadType(stompHeaders)).willReturn(Map.class);

		this.session.handleMessage(MessageBuilder.createMessage(payload, accessor.getMessageHeaders()));

		verify(frameHandler).getPayloadType(stompHeaders);
		verifyNoMoreInteractions(frameHandler);

		verify(this.sessionHandler).handleException(same(this.session), same(StompCommand.MESSAGE),
				eq(stompHeaders), same(payload), any(MessageConversionException.class));
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void handleFailure() {
		IllegalStateException exception = new IllegalStateException("simulated exception");
		this.session.handleFailure(exception);

		verify(this.sessionHandler).handleTransportError(this.session, exception);
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void afterConnectionClosed() {
		this.session.afterConnectionClosed();

		verify(this.sessionHandler).handleTransportError(same(this.session), any(ConnectionLostException.class));
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void send() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String destination = "/topic/foo";
		String payload = "sample payload";
		this.session.send(destination, payload);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.SEND);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(2);

		assertThat(stompHeaders.getDestination()).isEqualTo(destination);
		assertThat(stompHeaders.getContentType()).isEqualTo(new MimeType("text", "plain", StandardCharsets.UTF_8));
		// StompEncoder isn't involved
		assertThat(stompHeaders.getContentLength()).isEqualTo(-1);
		assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo(payload);
	}

	@Test
	public void sendWithReceipt() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		this.session.setTaskScheduler(mock(TaskScheduler.class));
		this.session.setAutoReceipt(true);
		this.session.send("/topic/foo", "sample payload");

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getReceipt()).isNotNull();

		StompHeaders stompHeaders = new StompHeaders();
		stompHeaders.setDestination("/topic/foo");
		stompHeaders.setReceipt("my-receipt");
		this.session.send(stompHeaders, "sample payload");

		message = this.messageCaptor.getValue();
		accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getReceipt()).isEqualTo("my-receipt");
	}

	@Test // gh-23358
	public void sendByteArray() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected());

		String destination = "/topic/foo";
		String payload = "sample payload";
		this.session.send(destination, payload.getBytes(StandardCharsets.UTF_8));

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(2);

		assertThat(stompHeaders.getDestination()).isEqualTo(destination);
		assertThat(stompHeaders.getContentType()).isEqualTo(MimeTypeUtils.APPLICATION_OCTET_STREAM);
		assertThat(new String(message.getPayload(), StandardCharsets.UTF_8)).isEqualTo(payload);
	}

	@Test
	public void sendWithConversionException() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		StompHeaders stompHeaders = new StompHeaders();
		stompHeaders.setDestination("/topic/foo");
		stompHeaders.setContentType(MimeTypeUtils.APPLICATION_JSON);
		String payload = "{'foo':'bar'}";

		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.session.send(stompHeaders, payload));
	}

	@Test
	public void sendWithExecutionException() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		IllegalStateException exception = new IllegalStateException("simulated exception");
		SettableListenableFuture<Void> future = new SettableListenableFuture<>();
		future.setException(exception);

		given(this.connection.send(any())).willReturn(future);
		assertThatExceptionOfType(MessageDeliveryException.class).isThrownBy(() ->
				this.session.send("/topic/foo", "sample payload".getBytes(StandardCharsets.UTF_8)))
			.withCause(exception);
	}

	@Test
	public void subscribe() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String destination = "/topic/foo";
		StompFrameHandler frameHandler = mock(StompFrameHandler.class);
		Subscription subscription = this.session.subscribe(destination, frameHandler);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.SUBSCRIBE);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(2);
		assertThat(stompHeaders.getDestination()).isEqualTo(destination);
		assertThat(stompHeaders.getId()).isEqualTo(subscription.getSubscriptionId());
	}

	@Test
	public void subscribeWithHeaders() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String subscriptionId = "123";
		String destination = "/topic/foo";

		StompHeaders stompHeaders = new StompHeaders();
		stompHeaders.setId(subscriptionId);
		stompHeaders.setDestination(destination);
		StompFrameHandler frameHandler = mock(StompFrameHandler.class);

		Subscription subscription = this.session.subscribe(stompHeaders, frameHandler);
		assertThat(subscription.getSubscriptionId()).isEqualTo(subscriptionId);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.SUBSCRIBE);

		stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(2);
		assertThat(stompHeaders.getDestination()).isEqualTo(destination);
		assertThat(stompHeaders.getId()).isEqualTo(subscriptionId);
	}

	@Test
	public void unsubscribe() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String destination = "/topic/foo";
		StompFrameHandler frameHandler = mock(StompFrameHandler.class);
		Subscription subscription = this.session.subscribe(destination, frameHandler);
		subscription.unsubscribe();

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.UNSUBSCRIBE);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(1);
		assertThat(stompHeaders.getId()).isEqualTo(subscription.getSubscriptionId());
	}

	@Test // SPR-15131
	public void unsubscribeWithCustomHeader() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String headerName = "durable-subscription-name";
		String headerValue = "123";

		StompHeaders subscribeHeaders = new StompHeaders();
		subscribeHeaders.setDestination("/topic/foo");
		subscribeHeaders.set(headerName, headerValue);
		StompFrameHandler frameHandler = mock(StompFrameHandler.class);
		Subscription subscription = this.session.subscribe(subscribeHeaders, frameHandler);

		StompHeaders unsubscribeHeaders = new StompHeaders();
		unsubscribeHeaders.set(headerName,  subscription.getSubscriptionHeaders().getFirst(headerName));
		subscription.unsubscribe(unsubscribeHeaders);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.UNSUBSCRIBE);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(2);
		assertThat(stompHeaders.getId()).isEqualTo(subscription.getSubscriptionId());
		assertThat(stompHeaders.getFirst(headerName)).isEqualTo(headerValue);
	}

	@Test
	public void ack() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String messageId = "123";
		this.session.acknowledge(messageId, true);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.ACK);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(1);
		assertThat(stompHeaders.getId()).isEqualTo(messageId);
	}

	@Test
	public void nack() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		String messageId = "123";
		this.session.acknowledge(messageId, false);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		assertThat(accessor.getCommand()).isEqualTo(StompCommand.NACK);

		StompHeaders stompHeaders = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(stompHeaders.size()).as(stompHeaders.toString()).isEqualTo(1);
		assertThat(stompHeaders.getId()).isEqualTo(messageId);
	}

	@Test
	public void receiptReceived() {
		this.session.afterConnected(this.connection);
		this.session.setTaskScheduler(mock(TaskScheduler.class));

		AtomicReference<Boolean> received = new AtomicReference<>();
		AtomicReference<StompHeaders> receivedHeaders = new AtomicReference<>();

		StompHeaders headers = new StompHeaders();
		headers.setDestination("/topic/foo");
		headers.setReceipt("my-receipt");
		Subscription subscription = this.session.subscribe(headers, mock(StompFrameHandler.class));
		subscription.addReceiptTask(receiptHeaders -> {
			received.set(true);
			receivedHeaders.set(receiptHeaders);
		});

		assertThat((Object) received.get()).isNull();

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
		accessor.setReceiptId("my-receipt");
		accessor.setNativeHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		assertThat(received.get()).isNotNull();
		assertThat(received.get()).isTrue();
		assertThat(receivedHeaders.get()).isNotNull();
		assertThat(receivedHeaders.get().get("foo").size()).isEqualTo(1);
		assertThat(receivedHeaders.get().get("foo").get(0)).isEqualTo("bar");
	}

	@Test
	public void receiptReceivedBeforeTaskAdded() {
		this.session.afterConnected(this.connection);
		this.session.setTaskScheduler(mock(TaskScheduler.class));

		AtomicReference<Boolean> received = new AtomicReference<>();
		AtomicReference<StompHeaders> receivedHeaders = new AtomicReference<>();

		StompHeaders headers = new StompHeaders();
		headers.setDestination("/topic/foo");
		headers.setReceipt("my-receipt");
		Subscription subscription = this.session.subscribe(headers, mock(StompFrameHandler.class));

		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
		accessor.setReceiptId("my-receipt");
		accessor.setNativeHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		this.session.handleMessage(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()));

		subscription.addReceiptTask(receiptHeaders -> {
			received.set(true);
			receivedHeaders.set(receiptHeaders);
		});

		assertThat(received.get()).isNotNull();
		assertThat(received.get()).isTrue();
		assertThat(receivedHeaders.get()).isNotNull();
		assertThat(receivedHeaders.get().get("foo").size()).isEqualTo(1);
		assertThat(receivedHeaders.get().get("foo").get(0)).isEqualTo("bar");
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void receiptNotReceived() {
		TaskScheduler taskScheduler = mock(TaskScheduler.class);

		this.session.afterConnected(this.connection);
		this.session.setTaskScheduler(taskScheduler);

		AtomicReference<Boolean> notReceived = new AtomicReference<>();

		ScheduledFuture future = mock(ScheduledFuture.class);
		given(taskScheduler.schedule(any(Runnable.class), any(Date.class))).willReturn(future);

		StompHeaders headers = new StompHeaders();
		headers.setDestination("/topic/foo");
		headers.setReceipt("my-receipt");
		Receiptable receiptable = this.session.send(headers, "payload");
		receiptable.addReceiptLostTask(() -> notReceived.set(true));

		ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
		verify(taskScheduler).schedule(taskCaptor.capture(), (Date) notNull());
		Runnable scheduledTask = taskCaptor.getValue();
		assertThat(scheduledTask).isNotNull();

		assertThat((Object) notReceived.get()).isNull();

		scheduledTask.run();
		assertThat(notReceived.get()).isTrue();
		verify(future).cancel(true);
		verifyNoMoreInteractions(future);
	}

	@Test
	public void disconnect() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		this.session.disconnect();
		assertThat(this.session.isConnected()).isFalse();
		verifyNoMoreInteractions(this.sessionHandler);
	}

	@Test
	public void disconnectWithHeaders() {
		this.session.afterConnected(this.connection);
		assertThat(this.session.isConnected()).isTrue();

		StompHeaders headers = new StompHeaders();
		headers.add("foo", "bar");

		this.session.disconnect(headers);

		Message<byte[]> message = this.messageCaptor.getValue();
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		headers = StompHeaders.readOnlyStompHeaders(accessor.getNativeHeaders());
		assertThat(headers.size()).as(headers.toString()).isEqualTo(1);
		assertThat(headers.get("foo").size()).isEqualTo(1);
		assertThat(headers.get("foo").get(0)).isEqualTo("bar");

		assertThat(this.session.isConnected()).isFalse();
		verifyNoMoreInteractions(this.sessionHandler);
	}

}
