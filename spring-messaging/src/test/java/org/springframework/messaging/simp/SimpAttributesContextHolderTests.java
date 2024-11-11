/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.messaging.simp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SimpAttributesContextHolder}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class SimpAttributesContextHolderTests {

	private SimpAttributes simpAttributes;


	@BeforeEach
	void setUp() {
		Map<String, Object> map = new ConcurrentHashMap<>();
		this.simpAttributes = new SimpAttributes("session1", map);
	}

	@AfterEach
	void tearDown() {
		SimpAttributesContextHolder.resetAttributes();
	}


	@Test
	void resetAttributes() {
		SimpAttributesContextHolder.setAttributes(this.simpAttributes);
		assertThat(SimpAttributesContextHolder.getAttributes()).isSameAs(this.simpAttributes);

		SimpAttributesContextHolder.resetAttributes();
		assertThat(SimpAttributesContextHolder.getAttributes()).isNull();
	}

	@Test
	void getAttributes() {
		assertThat(SimpAttributesContextHolder.getAttributes()).isNull();

		SimpAttributesContextHolder.setAttributes(this.simpAttributes);
		assertThat(SimpAttributesContextHolder.getAttributes()).isSameAs(this.simpAttributes);
	}

	@Test
	void setAttributes() {
		SimpAttributesContextHolder.setAttributes(this.simpAttributes);
		assertThat(SimpAttributesContextHolder.getAttributes()).isSameAs(this.simpAttributes);

		SimpAttributesContextHolder.setAttributes(null);
		assertThat(SimpAttributesContextHolder.getAttributes()).isNull();
	}

	@Test
	void setAttributesFromMessage() {

		String sessionId = "session1";
		ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
		headerAccessor.setSessionId(sessionId);
		headerAccessor.setSessionAttributes(map);
		Message<?> message = MessageBuilder.createMessage("", headerAccessor.getMessageHeaders());

		SimpAttributesContextHolder.setAttributesFromMessage(message);

		SimpAttributes attrs = SimpAttributesContextHolder.getAttributes();
		assertThat(attrs).isNotNull();
		assertThat(attrs.getSessionId()).isEqualTo(sessionId);

		attrs.setAttribute("name1", "value1");
		assertThat(map.get("name1")).isEqualTo("value1");
	}

	@Test
	void setAttributesFromMessageWithMissingSessionId() {
		assertThatIllegalStateException().isThrownBy(() ->
				SimpAttributesContextHolder.setAttributesFromMessage(new GenericMessage<>("")))
			.withMessageStartingWith("No session id in");
	}

	@Test
	void setAttributesFromMessageWithMissingSessionAttributes() {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
		headerAccessor.setSessionId("session1");
		Message<?> message = MessageBuilder.createMessage("", headerAccessor.getMessageHeaders());
		assertThatIllegalStateException().isThrownBy(() ->
				SimpAttributesContextHolder.setAttributesFromMessage(message))
			.withMessageStartingWith("No session attributes in");
	}

	@Test
	void currentAttributes() {
		SimpAttributesContextHolder.setAttributes(this.simpAttributes);
		assertThat(SimpAttributesContextHolder.currentAttributes()).isSameAs(this.simpAttributes);
	}

	@Test
	void currentAttributesNone() {
		assertThatIllegalStateException().isThrownBy(SimpAttributesContextHolder::currentAttributes)
			.withMessageStartingWith("No thread-bound SimpAttributes found");
	}

}
