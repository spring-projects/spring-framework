/*
 * Copyright 2002-2017 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.SerializationTestUtils;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test fixture for {@link MessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
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
	public void testRemoveHeader() {
		Message<?> message = new GenericMessage<>("payload", Collections.singletonMap("foo", "bar"));
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.removeHeader("foo");
		Map<String, Object> headers = accessor.toMap();
		assertFalse(headers.containsKey("foo"));
	}

	@Test
	public void testRemoveHeaderEvenIfNull() {
		Message<?> message = new GenericMessage<>("payload", Collections.singletonMap("foo", null));
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.removeHeader("foo");
		Map<String, Object> headers = accessor.toMap();
		assertFalse(headers.containsKey("foo"));
	}

	@Test
	public void removeHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		GenericMessage<String> message = new GenericMessage<>("payload", map);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);

		accessor.removeHeaders("fo*");

		MessageHeaders actual = accessor.getMessageHeaders();
		assertEquals(2, actual.size());
		assertNull(actual.get("foo"));
		assertEquals("baz", actual.get("bar"));
	}

	@Test
	public void copyHeaders() {
		Map<String, Object> map1 = new HashMap<>();
		map1.put("foo", "bar");
		GenericMessage<String> message = new GenericMessage<>("payload", map1);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);

		Map<String, Object> map2 = new HashMap<>();
		map2.put("foo", "BAR");
		map2.put("bar", "baz");
		accessor.copyHeaders(map2);

		MessageHeaders actual = accessor.getMessageHeaders();
		assertEquals(3, actual.size());
		assertEquals("BAR", actual.get("foo"));
		assertEquals("baz", actual.get("bar"));
	}

	@Test
	public void copyHeadersIfAbsent() {
		Map<String, Object> map1 = new HashMap<>();
		map1.put("foo", "bar");
		GenericMessage<String> message = new GenericMessage<>("payload", map1);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);

		Map<String, Object> map2 = new HashMap<>();
		map2.put("foo", "BAR");
		map2.put("bar", "baz");
		accessor.copyHeadersIfAbsent(map2);

		MessageHeaders actual = accessor.getMessageHeaders();
		assertEquals(3, actual.size());
		assertEquals("bar", actual.get("foo"));
		assertEquals("baz", actual.get("bar"));
	}

	@Test
	public void copyHeadersFromNullMap() {
		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.copyHeaders(null);
		headers.copyHeadersIfAbsent(null);

		assertEquals(1, headers.getMessageHeaders().size());
		assertEquals(Collections.singleton("id"), headers.getMessageHeaders().keySet());
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
		accessor.setIdGenerator(() -> id);
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
		accessor.setIdGenerator(() -> MessageHeaders.ID_VALUE_NONE);
		accessor.setEnableTimestamp(false);
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		assertNull(headers.getId());
		assertNull(headers.getTimestamp());

		final UUID id = new UUID(0L, 23L);
		accessor.setIdGenerator(() -> id);
		accessor.setEnableTimestamp(true);
		accessor.setImmutable();

		assertSame(id, accessor.getMessageHeaders().getId());
		assertNotNull(headers.getTimestamp());
	}

	@Test
	public void getShortLogMessagePayload() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);

		String expected = "headers={contentType=text/plain} payload=p";
		assertEquals(expected, accessor.getShortLogMessage("p"));
		assertEquals(expected, accessor.getShortLogMessage("p".getBytes(StandardCharsets.UTF_8)));
		assertEquals(expected, accessor.getShortLogMessage(new Object() {
			@Override
			public String toString() {
				return "p";
			}
		}));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 80; i++) {
			sb.append("a");
		}
		final String payload = sb.toString() + " > 80";

		String actual = accessor.getShortLogMessage(payload);
		assertEquals("headers={contentType=text/plain} payload=" + sb + "...(truncated)", actual);

		actual = accessor.getShortLogMessage(payload.getBytes(StandardCharsets.UTF_8));
		assertEquals("headers={contentType=text/plain} payload=" + sb + "...(truncated)", actual);

		actual = accessor.getShortLogMessage(new Object() {
			@Override
			public String toString() {
				return payload;
			}
		});
		assertThat(actual, startsWith("headers={contentType=text/plain} payload=" + getClass().getName() + "$"));
	}

	@Test
	public void getDetailedLogMessagePayload() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);

		String expected = "headers={contentType=text/plain} payload=p";
		assertEquals(expected, accessor.getDetailedLogMessage("p"));
		assertEquals(expected, accessor.getDetailedLogMessage("p".getBytes(StandardCharsets.UTF_8)));
		assertEquals(expected, accessor.getDetailedLogMessage(new Object() {
			@Override
			public String toString() {
				return "p";
			}
		}));

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 80; i++) {
			sb.append("a");
		}
		final String payload = sb.toString() + " > 80";

		String actual = accessor.getDetailedLogMessage(payload);
		assertEquals("headers={contentType=text/plain} payload=" + sb + " > 80", actual);

		actual = accessor.getDetailedLogMessage(payload.getBytes(StandardCharsets.UTF_8));
		assertEquals("headers={contentType=text/plain} payload=" + sb + " > 80", actual);

		actual = accessor.getDetailedLogMessage(new Object() {
			@Override
			public String toString() {
				return payload;
			}
		});
		assertEquals("headers={contentType=text/plain} payload=" + sb + " > 80", actual);
	}

	@Test
	public void serializeMutableHeaders() throws Exception {
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");
		Message<String> message = new GenericMessage<>("test", headers);
		MessageHeaderAccessor mutableAccessor = MessageHeaderAccessor.getMutableAccessor(message);
		mutableAccessor.setContentType(MimeTypeUtils.TEXT_PLAIN);

		message = new GenericMessage<>(message.getPayload(), mutableAccessor.getMessageHeaders());
		Message<?> output = (Message<?>) SerializationTestUtils.serializeAndDeserialize(message);
		assertEquals("test", output.getPayload());
		assertEquals("bar", output.getHeaders().get("foo"));
		assertNotNull(output.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}


	public static class TestMessageHeaderAccessor extends MessageHeaderAccessor {

		public TestMessageHeaderAccessor() {
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
