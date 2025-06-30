/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.io.SerializationTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture for {@link MessageHeaderAccessor}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 */
class MessageHeaderAccessorTests {

	@Test
	void newEmptyHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		assertThat(accessor.toMap()).isEmpty();
	}

	@Test
	void fromEmptyMap() {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.fromMap(Collections.emptyMap());
		assertThat(accessor.toMap()).isEmpty();
	}

	@Test
	void fromNullMap() {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.fromMap(null);
		assertThat(accessor.toMap()).isEmpty();
	}

	@Test
	void fromEmptyMessageHeaders() {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.fromMessageHeaders(new MessageHeaders(Collections.emptyMap()));
		assertThat(accessor.toMap()).isEmpty();
	}

	@Test
	void fromNullMessageHeaders() {
		MessageHeaderAccessor accessor = MessageHeaderAccessor.fromMessageHeaders(null);
		assertThat(accessor.toMap()).isEmpty();
	}

	@Test
	void existingHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		GenericMessage<String> message = new GenericMessage<>("payload", map);

		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		MessageHeaders actual = accessor.getMessageHeaders();

		assertThat(actual).hasSize(3);
		assertThat(actual.get("foo")).isEqualTo("bar");
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void fromMapWithExistingHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		MessageHeaderAccessor accessor = MessageHeaderAccessor.fromMap(map);
		MessageHeaders actual = accessor.getMessageHeaders();

		assertThat(actual).hasSize(3);
		assertThat(actual.get("foo")).isEqualTo("bar");
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void fromMessageHeaderWithExistingHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		MessageHeaderAccessor accessor = MessageHeaderAccessor.fromMessageHeaders(new MessageHeaders(map));
		MessageHeaders actual = accessor.getMessageHeaders();

		assertThat(actual).hasSize(3);
		assertThat(actual.get("foo")).isEqualTo("bar");
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void existingHeadersModification() throws InterruptedException {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		GenericMessage<String> message = new GenericMessage<>("payload", map);

		Thread.sleep(50);

		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.setHeader("foo", "BAR");
		MessageHeaders actual = accessor.getMessageHeaders();

		assertThat(actual).hasSize(3);
		assertThat(actual.getId()).isNotEqualTo(message.getHeaders().getId());
		assertThat(actual.get("foo")).isEqualTo("BAR");
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void testRemoveHeader() {
		Message<?> message = new GenericMessage<>("payload", Collections.singletonMap("foo", "bar"));
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.removeHeader("foo");
		Map<String, Object> headers = accessor.toMap();
		assertThat(headers.containsKey("foo")).isFalse();
	}

	@Test
	void testRemoveHeaderEvenIfNull() {
		Message<?> message = new GenericMessage<>("payload", Collections.singletonMap("foo", null));
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);
		accessor.removeHeader("foo");
		Map<String, Object> headers = accessor.toMap();
		assertThat(headers.containsKey("foo")).isFalse();
	}

	@Test
	void removeHeaders() {
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("bar", "baz");
		GenericMessage<String> message = new GenericMessage<>("payload", map);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);

		accessor.removeHeaders("fo*");

		MessageHeaders actual = accessor.getMessageHeaders();
		assertThat(actual).hasSize(2);
		assertThat(actual.get("foo")).isNull();
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void copyHeaders() {
		Map<String, Object> map1 = new HashMap<>();
		map1.put("foo", "bar");
		GenericMessage<String> message = new GenericMessage<>("payload", map1);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);

		Map<String, Object> map2 = new HashMap<>();
		map2.put("foo", "BAR");
		map2.put("bar", "baz");
		accessor.copyHeaders(map2);

		MessageHeaders actual = accessor.getMessageHeaders();
		assertThat(actual).hasSize(3);
		assertThat(actual.get("foo")).isEqualTo("BAR");
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void copyHeadersIfAbsent() {
		Map<String, Object> map1 = new HashMap<>();
		map1.put("foo", "bar");
		GenericMessage<String> message = new GenericMessage<>("payload", map1);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor(message);

		Map<String, Object> map2 = new HashMap<>();
		map2.put("foo", "BAR");
		map2.put("bar", "baz");
		accessor.copyHeadersIfAbsent(map2);

		MessageHeaders actual = accessor.getMessageHeaders();
		assertThat(actual).hasSize(3);
		assertThat(actual.get("foo")).isEqualTo("bar");
		assertThat(actual.get("bar")).isEqualTo("baz");
	}

	@Test
	void copyHeadersFromNullMap() {
		MessageHeaderAccessor headers = new MessageHeaderAccessor();
		headers.copyHeaders(null);
		headers.copyHeadersIfAbsent(null);

		assertThat(headers.getMessageHeaders()).containsOnlyKeys(Collections.singleton("id"));
	}

	@Test
	void toMap() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();

		accessor.setHeader("foo", "bar1");
		Map<String, Object> map1 = accessor.toMap();

		accessor.setHeader("foo", "bar2");
		Map<String, Object> map2 = accessor.toMap();

		accessor.setHeader("foo", "bar3");
		Map<String, Object> map3 = accessor.toMap();

		assertThat(map1).hasSize(1);
		assertThat(map2).hasSize(1);
		assertThat(map3).hasSize(1);

		assertThat(map1.get("foo")).isEqualTo("bar1");
		assertThat(map2.get("foo")).isEqualTo("bar2");
		assertThat(map3.get("foo")).isEqualTo("bar3");
	}

	@Test
	void leaveMutable() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);

		accessor.setHeader("foo", "baz");

		assertThat(headers.get("foo")).isEqualTo("baz");
		assertThat(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class)).isSameAs(accessor);
	}

	@Test
	void leaveMutableDefaultBehavior() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);

		assertThatIllegalStateException().isThrownBy(() ->
				accessor.setLeaveMutable(true))
			.withMessageContaining("Already immutable");

		assertThatIllegalStateException().isThrownBy(() ->
				accessor.setHeader("foo", "baz"))
			.withMessageContaining("Already immutable");

		assertThat(headers.get("foo")).isEqualTo("bar");
		assertThat(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class)).isSameAs(accessor);
	}

	@Test
	void getAccessor() {
		MessageHeaderAccessor expected = new MessageHeaderAccessor();
		Message<?> message = MessageBuilder.createMessage("payload", expected.getMessageHeaders());
		assertThat(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class)).isSameAs(expected);
	}

	@Test
	void getMutableAccessorSameInstance() {
		TestMessageHeaderAccessor expected = new TestMessageHeaderAccessor();
		expected.setLeaveMutable(true);
		Message<?> message = MessageBuilder.createMessage("payload", expected.getMessageHeaders());

		MessageHeaderAccessor actual = MessageHeaderAccessor.getMutableAccessor(message);
		assertThat(actual).isNotNull();
		assertThat(actual.isMutable()).isTrue();
		assertThat(actual).isSameAs(expected);
	}

	@Test
	void getMutableAccessorNewInstance() {
		Message<?> message = MessageBuilder.withPayload("payload").build();

		MessageHeaderAccessor actual = MessageHeaderAccessor.getMutableAccessor(message);
		assertThat(actual).isNotNull();
		assertThat(actual.isMutable()).isTrue();
	}

	@Test
	void getMutableAccessorNewInstanceMatchingType() {
		TestMessageHeaderAccessor expected = new TestMessageHeaderAccessor();
		Message<?> message = MessageBuilder.createMessage("payload", expected.getMessageHeaders());

		MessageHeaderAccessor actual = MessageHeaderAccessor.getMutableAccessor(message);
		assertThat(actual).isNotNull();
		assertThat(actual.isMutable()).isTrue();
		assertThat(actual.getClass()).isEqualTo(TestMessageHeaderAccessor.class);
	}

	@Test
	void timestampEnabled() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setEnableTimestamp(true);
		assertThat(accessor.getMessageHeaders().getTimestamp()).isNotNull();
	}

	@Test
	void timestampDefaultBehavior() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		assertThat(accessor.getMessageHeaders().getTimestamp()).isNull();
	}

	@Test
	void idGeneratorCustom() {
		final UUID id = new UUID(0L, 23L);
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setIdGenerator(() -> id);
		assertThat(accessor.getMessageHeaders().getId()).isSameAs(id);
	}

	@Test
	void idGeneratorDefaultBehavior() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		assertThat(accessor.getMessageHeaders().getId()).isNotNull();
	}


	@Test
	void idTimestampWithMutableHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setIdGenerator(() -> MessageHeaders.ID_VALUE_NONE);
		accessor.setEnableTimestamp(false);
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();

		assertThat(headers.getId()).isNull();
		assertThat(headers.getTimestamp()).isNull();

		final UUID id = new UUID(0L, 23L);
		accessor.setIdGenerator(() -> id);
		accessor.setEnableTimestamp(true);
		accessor.setImmutable();

		assertThat(accessor.getMessageHeaders().getId()).isSameAs(id);
		assertThat(headers.getTimestamp()).isNotNull();
	}

	@Test
	void getShortLogMessagePayload() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);

		String expected = "headers={contentType=text/plain} payload=p";
		assertThat(accessor.getShortLogMessage("p")).isEqualTo(expected);
		assertThat(accessor.getShortLogMessage("p".getBytes(StandardCharsets.UTF_8))).isEqualTo(expected);
		assertThat(accessor.getShortLogMessage(new Object() {
			@Override
			public String toString() {
				return "p";
			}
		})).isEqualTo(expected);

		StringBuilder sb = new StringBuilder();
		sb.append("a".repeat(80));
		final String payload = sb + " > 80";

		String actual = accessor.getShortLogMessage(payload);
		assertThat(actual).isEqualTo("headers={contentType=text/plain} payload=" + sb + "...(truncated)");

		actual = accessor.getShortLogMessage(payload.getBytes(StandardCharsets.UTF_8));
		assertThat(actual).isEqualTo("headers={contentType=text/plain} payload=" + sb + "...(truncated)");

		actual = accessor.getShortLogMessage(new Object() {
			@Override
			public String toString() {
				return payload;
			}
		});
		assertThat(actual).startsWith("headers={contentType=text/plain} payload=" + getClass().getName() + "$");
	}

	@Test
	void getDetailedLogMessagePayload() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setContentType(MimeTypeUtils.TEXT_PLAIN);

		String expected = "headers={contentType=text/plain} payload=p";
		assertThat(accessor.getDetailedLogMessage("p")).isEqualTo(expected);
		assertThat(accessor.getDetailedLogMessage("p".getBytes(StandardCharsets.UTF_8))).isEqualTo(expected);
		assertThat(accessor.getDetailedLogMessage(new Object() {
			@Override
			public String toString() {
				return "p";
			}
		})).isEqualTo(expected);

		StringBuilder sb = new StringBuilder();
		sb.append("a".repeat(80));
		final String payload = sb + " > 80";

		String actual = accessor.getDetailedLogMessage(payload);
		assertThat(actual).isEqualTo("headers={contentType=text/plain} payload=" + sb + " > 80");

		actual = accessor.getDetailedLogMessage(payload.getBytes(StandardCharsets.UTF_8));
		assertThat(actual).isEqualTo("headers={contentType=text/plain} payload=" + sb + " > 80");

		actual = accessor.getDetailedLogMessage(new Object() {
			@Override
			public String toString() {
				return payload;
			}
		});
		assertThat(actual).isEqualTo("headers={contentType=text/plain} payload=" + sb + " > 80");
	}

	@Test
	void serializeMutableHeaders() throws Exception {
		Map<String, Object> headers = new HashMap<>();
		headers.put("foo", "bar");
		Message<String> message = new GenericMessage<>("test", headers);
		MessageHeaderAccessor mutableAccessor = MessageHeaderAccessor.getMutableAccessor(message);
		mutableAccessor.setContentType(MimeTypeUtils.TEXT_PLAIN);

		message = new GenericMessage<>(message.getPayload(), mutableAccessor.getMessageHeaders());
		Message<?> output = SerializationTestUtils.serializeAndDeserialize(message);
		assertThat(output.getPayload()).isEqualTo("test");
		assertThat(output.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(output.getHeaders().get(MessageHeaders.CONTENT_TYPE)).isNotNull();
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
