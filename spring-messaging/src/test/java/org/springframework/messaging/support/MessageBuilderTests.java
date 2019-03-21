/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 */
public class MessageBuilderTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void testSimpleMessageCreation() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		assertEquals("foo", message.getPayload());
	}

	@Test
	public void testHeaderValues() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar")
				.setHeader("count", 123)
				.build();
		assertEquals("bar", message.getHeaders().get("foo", String.class));
		assertEquals(new Integer(123), message.getHeaders().get("count", Integer.class));
	}

	@Test
	public void testCopiedHeaderValues() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "1")
				.setHeader("bar", "2")
				.build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.copyHeaders(message1.getHeaders())
				.setHeader("foo", "42")
				.setHeaderIfAbsent("bar", "99")
				.build();
		assertEquals("test1", message1.getPayload());
		assertEquals("test2", message2.getPayload());
		assertEquals("1", message1.getHeaders().get("foo"));
		assertEquals("42", message2.getHeaders().get("foo"));
		assertEquals("2", message1.getHeaders().get("bar"));
		assertEquals("2", message2.getHeaders().get("bar"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIdHeaderValueReadOnly() {
		UUID id = UUID.randomUUID();
		MessageBuilder.withPayload("test").setHeader(MessageHeaders.ID, id);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTimestampValueReadOnly() {
		Long timestamp = 12345L;
		MessageBuilder.withPayload("test").setHeader(MessageHeaders.TIMESTAMP, timestamp).build();
	}

	@Test
	public void copyHeadersIfAbsent() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setHeader("foo", 123)
				.copyHeadersIfAbsent(message1.getHeaders())
				.build();
		assertEquals("test2", message2.getPayload());
		assertEquals(123, message2.getHeaders().get("foo"));
	}

	@Test
	public void createFromMessage() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).build();
		assertEquals("test", message2.getPayload());
		assertEquals("bar", message2.getHeaders().get("foo"));
	}

	@Test
	public void createIdRegenerated() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).setHeader("another", 1).build();
		assertEquals("bar", message2.getHeaders().get("foo"));
		assertNotSame(message1.getHeaders().getId(), message2.getHeaders().getId());
	}

	@Test
	public void testRemove() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
			.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
			.removeHeader("foo")
			.build();
		assertFalse(message2.getHeaders().containsKey("foo"));
	}

	@Test
	public void testSettingToNullRemoves() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
			.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
			.setHeader("foo", null)
			.build();
		assertFalse(message2.getHeaders().containsKey("foo"));
	}

	@Test
	public void testNotModifiedSameMessage() throws Exception {
		Message<?> original = MessageBuilder.withPayload("foo").build();
		Message<?> result = MessageBuilder.fromMessage(original).build();
		assertEquals(original, result);
	}

	@Test
	public void testContainsHeaderNotModifiedSameMessage() throws Exception {
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("bar", 42).build();
		Message<?> result = MessageBuilder.fromMessage(original).build();
		assertEquals(original, result);
	}

	@Test
	public void testSameHeaderValueAddedNotModifiedSameMessage() throws Exception {
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("bar", 42).build();
		Message<?> result = MessageBuilder.fromMessage(original).setHeader("bar", 42).build();
		assertEquals(original, result);
	}

	@Test
	public void testCopySameHeaderValuesNotModifiedSameMessage() throws Exception {
		Date current = new Date();
		Map<String, Object> originalHeaders = new HashMap<>();
		originalHeaders.put("b", "xyz");
		originalHeaders.put("c", current);
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("a", 123).copyHeaders(originalHeaders).build();
		Map<String, Object> newHeaders = new HashMap<>();
		newHeaders.put("a", 123);
		newHeaders.put("b", "xyz");
		newHeaders.put("c", current);
		Message<?> result = MessageBuilder.fromMessage(original).copyHeaders(newHeaders).build();
		assertEquals(original, result);
	}

	@Test
	public void testBuildMessageWithMutableHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);
		accessor.setHeader("foo", "bar");

		assertEquals("bar", headers.get("foo"));
		assertSame(accessor, MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class));
	}

	@Test
	public void testBuildMessageWithDefaultMutability() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("foo", headers);

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Already immutable");
		accessor.setHeader("foo", "bar");

		assertSame(accessor, MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class));
	}

	@Test
	public void testBuildMessageWithoutIdAndTimestamp() {
		MessageHeaderAccessor headerAccessor = new MessageHeaderAccessor();
		headerAccessor.setIdGenerator(new IdGenerator() {
			@Override
			public UUID generateId() {
				return MessageHeaders.ID_VALUE_NONE;
			}
		});
		Message<?> message = MessageBuilder.createMessage("foo", headerAccessor.getMessageHeaders());
		assertNull(message.getHeaders().getId());
		assertNull(message.getHeaders().getTimestamp());
	}

	@Test
	public void testBuildMultipleMessages() {
		MessageHeaderAccessor headerAccessor = new MessageHeaderAccessor();
		MessageBuilder<?> messageBuilder = MessageBuilder.withPayload("payload").setHeaders(headerAccessor);

		headerAccessor.setHeader("foo", "bar1");
		Message<?> message1 = messageBuilder.build();

		headerAccessor.setHeader("foo", "bar2");
		Message<?> message2 = messageBuilder.build();

		headerAccessor.setHeader("foo", "bar3");
		Message<?> message3 = messageBuilder.build();

		assertEquals("bar1", message1.getHeaders().get("foo"));
		assertEquals("bar2", message2.getHeaders().get("foo"));
		assertEquals("bar3", message3.getHeaders().get("foo"));
	}
}
