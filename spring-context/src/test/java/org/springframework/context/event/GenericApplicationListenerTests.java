/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
