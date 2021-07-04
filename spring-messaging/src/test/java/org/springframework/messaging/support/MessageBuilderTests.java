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

package org.springframework.messaging.support;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 */
public class MessageBuilderTests {

	@Test
	public void testSimpleMessageCreation() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		assertThat(message.getPayload()).isEqualTo("foo");
	}

	@Test
	public void testHeaderValues() {
		Message<String> message = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar")
				.setHeader("count", 123)
				.build();
		assertThat(message.getHeaders().get("foo", String.class)).isEqualTo("bar");
		assertThat(message.getHeaders().get("count", Integer.class)).isEqualTo(123);
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
		assertThat(message1.getPayload()).isEqualTo("test1");
		assertThat(message2.getPayload()).isEqualTo("test2");
		assertThat(message1.getHeaders().get("foo")).isEqualTo("1");
		assertThat(message2.getHeaders().get("foo")).isEqualTo("42");
		assertThat(message1.getHeaders().get("bar")).isEqualTo("2");
		assertThat(message2.getHeaders().get("bar")).isEqualTo("2");
	}

	@Test
	public void testIdHeaderValueReadOnly() {
		UUID id = UUID.randomUUID();
		assertThatIllegalArgumentException().isThrownBy(() ->
				MessageBuilder.withPayload("test").setHeader(MessageHeaders.ID, id));
	}

	@Test
	public void testTimestampValueReadOnly() {
		Long timestamp = 12345L;
		assertThatIllegalArgumentException().isThrownBy(() ->
				MessageBuilder.withPayload("test").setHeader(MessageHeaders.TIMESTAMP, timestamp).build());
	}

	@Test
	public void copyHeadersIfAbsent() {
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setHeader("foo", 123)
				.copyHeadersIfAbsent(message1.getHeaders())
				.build();
		assertThat(message2.getPayload()).isEqualTo("test2");
		assertThat(message2.getHeaders().get("foo")).isEqualTo(123);
	}

	@Test
	public void createFromMessage() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).build();
		assertThat(message2.getPayload()).isEqualTo("test");
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
	}

	@Test // gh-23417
	public void createErrorMessageFromErrorMessage() {
		Message<String> source = MessageBuilder.withPayload("test").setHeader("foo", "bar").build();
		RuntimeException ex = new RuntimeException();
		ErrorMessage errorMessage1 = new ErrorMessage(ex, Collections.singletonMap("baz", "42"), source);
		Message<Throwable> errorMessage2 = MessageBuilder.fromMessage(errorMessage1).build();
		assertThat(errorMessage2).isExactlyInstanceOf(ErrorMessage.class);
		ErrorMessage actual = (ErrorMessage) errorMessage2;
		assertThat(actual.getPayload()).isSameAs(ex);
		assertThat(actual.getHeaders().get("baz")).isEqualTo("42");
		assertThat(actual.getOriginalMessage()).isSameAs(source);
	}

	@Test
	public void createIdRegenerated() {
		Message<String> message1 = MessageBuilder.withPayload("test")
				.setHeader("foo", "bar").build();
		Message<String> message2 = MessageBuilder.fromMessage(message1).setHeader("another", 1).build();
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar");
		assertThat(message2.getHeaders().getId()).isNotSameAs(message1.getHeaders().getId());
	}

	@Test
	public void testRemove() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
				.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
				.removeHeader("foo")
				.build();
		assertThat(message2.getHeaders().containsKey("foo")).isFalse();
	}

	@Test
	public void testSettingToNullRemoves() {
		Message<Integer> message1 = MessageBuilder.withPayload(1)
				.setHeader("foo", "bar").build();
		Message<Integer> message2 = MessageBuilder.fromMessage(message1)
				.setHeader("foo", null)
				.build();
		assertThat(message2.getHeaders().containsKey("foo")).isFalse();
	}

	@Test
	public void testNotModifiedSameMessage() throws Exception {
		Message<?> original = MessageBuilder.withPayload("foo").build();
		Message<?> result = MessageBuilder.fromMessage(original).build();
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testContainsHeaderNotModifiedSameMessage() throws Exception {
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("bar", 42).build();
		Message<?> result = MessageBuilder.fromMessage(original).build();
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testSameHeaderValueAddedNotModifiedSameMessage() throws Exception {
		Message<?> original = MessageBuilder.withPayload("foo").setHeader("bar", 42).build();
		Message<?> result = MessageBuilder.fromMessage(original).setHeader("bar", 42).build();
		assertThat(result).isEqualTo(original);
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
		assertThat(result).isEqualTo(original);
	}

	@Test
	public void testBuildMessageWithMutableHeaders() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setLeaveMutable(true);
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("payload", headers);
		accessor.setHeader("foo", "bar");

		assertThat(headers.get("foo")).isEqualTo("bar");
		assertThat(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class)).isSameAs(accessor);
	}

	@Test
	public void testBuildMessageWithDefaultMutability() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		MessageHeaders headers = accessor.getMessageHeaders();
		Message<?> message = MessageBuilder.createMessage("foo", headers);

		assertThatIllegalStateException().isThrownBy(() ->
				accessor.setHeader("foo", "bar"))
				.withMessageContaining("Already immutable");

		assertThat(MessageHeaderAccessor.getAccessor(message, MessageHeaderAccessor.class)).isSameAs(accessor);
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
		assertThat(message.getHeaders().getId()).isNull();
		assertThat(message.getHeaders().getTimestamp()).isNull();
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

		assertThat(message1.getHeaders().get("foo")).isEqualTo("bar1");
		assertThat(message2.getHeaders().get("foo")).isEqualTo("bar2");
		assertThat(message3.getHeaders().get("foo")).isEqualTo("bar3");
	}
}
