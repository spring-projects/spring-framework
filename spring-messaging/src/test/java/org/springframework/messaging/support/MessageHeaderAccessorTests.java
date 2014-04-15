/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test fixture for {@link MessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 */
public class MessageHeaderAccessorTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void newEmptyHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		assertEquals(0, accessor.toMap().size());
	}

	@Test
	public void existingHeaders() throws InterruptedException {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		GenericMessage<String> message = new GenericMessage<>("payload", map);

		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		MessageHeaders actual = accessor.getMessageHeaders();

		assertEquals(3, actual.size());
		assertEquals("bar", actual.get("foo"));
		assertEquals("baz", actual.get("bar"));
	}

	@Test
	public void existingHeadersModification() throws InterruptedException {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		GenericMessage<String> message = new GenericMessage<>("payload", map);

		Thread.sleep(50);

		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.setHeader("foo", "BAR");
		MessageHeaders actual = accessor.getMessageHeaders();

		assertEquals(3, actual.size());
		assertNotEquals(message.getHeaders().getId(), actual.getId());
		assertEquals("BAR", actual.get("foo"));
		assertEquals("baz", actual.get("bar"));
	}

	@Test
	public void copyHeadersFromNullMap() {
		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.copyHeaders(null);
		headers.copyHeadersIfAbsent(null);

		assertEquals(1, headers.getMessageHeaders().size());
		assertEquals(new HashSet<>(Arrays.asList("id")), headers.getMessageHeaders().keySet());
	}

	@Test
	public void toMap() {

		MessageHeaderAccessor accessor = new MessageHeaderAccessor();

		accessor.setHeader("foo", "bar1");
		Map<String, Object> map1 = accessor.toMap();

		accessor.setHeader("foo", "bar2");
		Map<String, Object> map2 = accessor.toMap();

		accessor.setHeader("foo", "bar3");
		Map<String, Object> map3 = accessor.toMap();

		assertEquals(1, map1.size());
		assertEquals(1, map2.size());
		assertEquals(1, map3.size());

		assertEquals("bar1", map1.get("foo"));
		assertEquals("bar2", map2.get("foo"));
		assertEquals("bar3", map3.get("foo"));
	}

	@Test
	public void leaveMutable() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);

		accessor.setHeader("foo", "baz");

		assertEquals("baz", headers.get("foo"));
		assertSame(accessor, MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class));
	}

	@Test
	public void leaveMutableDefaultBehavior() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Already immutable");
		accessor.setLeaveMutable(true);

		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Already immutable");
		accessor.setHeader("foo", "baz");

		assertEquals("bar", headers.get("foo"));
		assertSame(accessor, MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class));
	}

	@Test
	public void getAccessor() {
		MessageHeaderAccessor expected = new MessageHeaderAccessor();
		Message<?> message = MessageBuilder.createMessage("payload", expected.getMessageHeaders());
		assertSame(expected, MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class));
	}

	@Test
	public void getMutableAccessorSameInstance() {
		TestMessageHeaderAccessor expected = new TestMessageHeaderAccessor();
		expected.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage("payload", expected.getMessageHeaders());

		MessageHeaderAccessor actual = MessageHeaderAccessor.getMutableAccessor(message);
		assertNotNull(actual);
		assertTrue(actual.isMutable());
		assertSame(expected, actual);
	}

	@Test
	public void getMutableAccessorNewInstance() {
		Message<?> message = MessageBuilder.withPayload("payload").build();

		MessageHeaderAccessor actual = MessageHeaderAccessor.getMutableAccessor(message);
		assertNotNull(actual);
		assertTrue(actual.isMutable());
	}

	@Test
	public void getMutableAccessorNewInstanceMatchingType() {
		TestMessageHeaderAccessor expected = new TestMessageHeaderAccessor();
		Message<?> message = MessageBuilder.createMessage("payload", expected.getMessageHeaders());

		MessageHeaderAccessor actual = MessageHeaderAccessor.getMutableAccessor(message);
		assertNotNull(actual);
		assertTrue(actual.isMutable());
		assertEquals(TestMessageHeaderAccessor.class, actual.getClass());
	}

	@Test
	public void timestampEnabled() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setEnableTimestamp(true);
		assertNotNull(accessor.getMessageHeaders().getTimestamp());
	}

	@Test
	public void timestampDefaultBehavior() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		assertNull(accessor.getMessageHeaders().getTimestamp());
	}

	@Test
	public void idGeneratorCustom() {
		final UUID id = new UUID(0L, 23L);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setIdGenerator(new IdGenerator() {
			@Override
			public UUID generateId() {
				return id;
			}
		});
		assertSame(id, accessor.getMessageHeaders().getId());
	}

	@Test
	public void idGeneratorDefaultBehavior() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		assertNotNull(accessor.getMessageHeaders().getId());
	}


	@Test
	public void idTimestampWithMutableHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setIdGenerator(new IdGenerator() {
			@Override
			public UUID generateId() {
				return MessageHeaders.ID_VALUE_NONE;
			}
		});
		accessor.setEnableTimestamp(false);
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		assertNull(headers.getId());
		assertNull(headers.getTimestamp());

		final UUID id = new UUID(0L, 23L);
		accessor.setIdGenerator(new IdGenerator() {
			@Override
			public UUID generateId() {
				return id;
			}
		});
		accessor.setEnableTimestamp(true);
		accessor.setImmutable();

		assertSame(id, accessor.getMessageHeaders().getId());
		assertNotNull(headers.getTimestamp());
	}


	public static class TestMessageHeaderAccessor extends MessageHeaderAccessor {

		private TestMessageHeaderAccessor() {
		}

		private TestMessageHeaderAccessor(Message<?> message) {
			super(message);
		}

		public static TestMessageHeaderAccessor wrap(Message<?> message) {
			return new TestMessageHeaderAccessor(message);
		}

		@Override
		protected TestMessageHeaderAccessor createAccessor(Message<?> message) {
			return wrap(message);
		}
	}

}
