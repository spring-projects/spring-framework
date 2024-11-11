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

package org.springframework.context.event;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GenericApplicationListener}.
 *
 * @author Stephane Nicoll
 */
class GenericApplicationListenerTests extends AbstractApplicationEventListenerTests {

	@Test
	void forEventTypeWithStrictTypeMatching() {
		GenericApplicationListener listener = GenericApplicationListener
				.forEventType(StringEvent.class, event -> {});
		assertThat(listener.supportsEventType(ResolvableType.forClass(StringEvent.class))).isTrue();
	}

	@Test
	void forEventTypeWithSubClass() {
		GenericApplicationListener listener = GenericApplicationListener
				.forEventType(GenericTestEvent.class, event -> {});
		assertThat(listener.supportsEventType(ResolvableType.forClass(StringEvent.class))).isTrue();
	}

	@Test
	void forEventTypeWithSuperClass() {
		GenericApplicationListener listener = GenericApplicationListener
				.forEventType(StringEvent.class, event -> {});
		assertThat(listener.supportsEventType(ResolvableType.forClass(GenericTestEvent.class))).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	void forEventTypeInvokesConsumer() {
		Consumer<StringEvent> consumer = mock(Consumer.class);
		GenericApplicationListener listener = GenericApplicationListener
				.forEventType(StringEvent.class, consumer);
		StringEvent event = new StringEvent(this, "one");
		StringEvent event2 = new StringEvent(this, "two");
		listener.onApplicationEvent(event);
		listener.onApplicationEvent(event2);
		InOrder ordered = inOrder(consumer);
		ordered.verify(consumer).accept(event);
		ordered.verify(consumer).accept(event2);
	}

}
