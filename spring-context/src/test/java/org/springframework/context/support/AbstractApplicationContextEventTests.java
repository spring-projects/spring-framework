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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.AbstractApplicationEventMulticaster;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.context.support.AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;

/**
 * Tests for event publishing / listening support in {@code AbstractApplicationContext},
 * including advanced scenarios: with {@code PayloadApplicationEvent} having a simple payload
 * or a generified payload, with/without early firing of event (before a multicaster is ready),
 * with/without a parent...
 */
class AbstractApplicationContextEventTests {

	@Test
	void cannotPublishPayloadEventWithInnerEventType() {
		AbstractApplicationContext ctx = new StaticApplicationContext();
		PayloadApplicationEvent<String> payloadApplicationEvent = new PayloadApplicationEvent<>(this, "message");

		assertThatIllegalArgumentException().isThrownBy(() -> ctx.publishEvent(payloadApplicationEvent, ResolvableType.forClass(String.class)))
				.withMessage("Cannot publish a PayloadApplicationEvent with a non-matching eventType, got java.lang.String");
	}

	@Test
	void cannotPublishPayloadEventWithInconsistentEventType() {
		AbstractApplicationContext ctx = new StaticApplicationContext();
		PayloadApplicationEvent<String> payloadApplicationEvent = new PayloadApplicationEvent<>(this, "message");
		ResolvableType inconsistentType = ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, Integer.class);

		assertThatIllegalArgumentException().isThrownBy(() -> ctx.publishEvent(payloadApplicationEvent, inconsistentType))
				.withMessage("Cannot publish a PayloadApplicationEvent with a non-matching eventType, got org.springframework.context.PayloadApplicationEvent<java.lang.Integer>");
	}

	@Nested
	class NoParent {

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void simpleType(boolean earlyPublishing) {
			String payload = "String event";
			final String expectedResolvableType = "org.springframework.context.PayloadApplicationEvent<java.lang.String>";

			assertPublishing(payload, expectedResolvableType, earlyPublishing, null);
		}

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void generifiedTypeResolvedFromInstance(boolean earlyPublishing) {
			List<String> payload = List.of("String", "List", "event");
			final String expectedResolvableType = "org.springframework.context.PayloadApplicationEvent<java.util.ImmutableCollections$ListN<?>>";

			assertPublishing(payload, expectedResolvableType, earlyPublishing, null);
		}

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void generifiedTypeWithExplicitResolvableType(boolean earlyPublishing) {
			List<String> payload = List.of("String", "List", "event");
			final String expectedResolvableType = "org.springframework.context.PayloadApplicationEvent<java.util.List<java.lang.String>>";

			assertPublishing(payload, expectedResolvableType, earlyPublishing, ResolvableType.forClassWithGenerics(List.class, String.class));
		}

		private <T> void assertPublishing(T payload, String expectedResolvableType, boolean earlyPublishing, @Nullable ResolvableType explicitEventType) {
			AbstractApplicationContext ctx = new StaticApplicationContext();
			TestMulticaster testMulticaster = new TestMulticaster();
			ctx.getBeanFactory().registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, testMulticaster);

			if (earlyPublishing) {
				ctx.prepareRefresh();
				assertThatIllegalStateException().as("before refresh").isThrownBy(ctx::getApplicationEventMulticaster);
			}
			else {
				ctx.refresh();
				assertThat(ctx.getApplicationEventMulticaster()).as("multicaster immediately ready")
						.isSameAs(testMulticaster);
			}

			if (explicitEventType != null) {
				ctx.publishEvent(payload, explicitEventType);
			}
			else {
				ctx.publishEvent(payload);
			}

			if (earlyPublishing) {
				//simulate the parts of refresh() that set up the multicaster and send early events
				ctx.initApplicationEventMulticaster();
				ctx.registerListeners();
				assertThat(ctx.getApplicationEventMulticaster()).as("after refresh")
						.isSameAs(testMulticaster);
			}

			assertThat(testMulticaster.events).singleElement()
					.isInstanceOfSatisfying(PayloadApplicationEvent.class, pae -> {
						assertThat(pae.getPayload()).isSameAs(payload);
						assertThat(pae.getResolvableType()).hasToString(expectedResolvableType);
					});
			assertThat(testMulticaster.listenerLookupTypes).singleElement()
					.hasToString(expectedResolvableType);
		}
	}

	@Nested
	class WithParent {

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void simpleType(boolean earlyPublishing) {
			String payload = "String event";
			final String expectedResolvableType = "org.springframework.context.PayloadApplicationEvent<java.lang.String>";

			assertPublishing(payload, expectedResolvableType, earlyPublishing, null);
		}

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void generifiedTypeResolvedFromInstance(boolean earlyPublishing) {
			List<String> payload = List.of("String", "List", "event");
			String expectedResolvableType = "org.springframework.context.PayloadApplicationEvent<java.util.ImmutableCollections$ListN<?>>";

			assertPublishing(payload, expectedResolvableType, earlyPublishing, null);
		}

		@ParameterizedTest
		@ValueSource(booleans = {true, false})
		void generifiedTypeWithExplicitResolvableType(boolean earlyPublishing) {
			List<String> payload = List.of("String", "List", "event");
			final String expectedResolvableType = "org.springframework.context.PayloadApplicationEvent<java.util.List<java.lang.String>>";

			assertPublishing(payload, expectedResolvableType, earlyPublishing, ResolvableType.forClassWithGenerics(List.class, String.class));
		}

		private <T> void assertPublishing(T payload, String expectedResolvableType, boolean earlyPublishing, @Nullable ResolvableType explicitEventType) {
			AbstractApplicationContext parentCtx = new StaticApplicationContext();
			TestMulticaster parentMulticaster = new TestMulticaster();
			parentCtx.getBeanFactory().registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, parentMulticaster);
			AbstractApplicationContext ctx = new StaticApplicationContext(parentCtx);
			TestMulticaster childMulticaster = new TestMulticaster();
			ctx.getBeanFactory().registerSingleton(APPLICATION_EVENT_MULTICASTER_BEAN_NAME, childMulticaster);

			assertThat(parentCtx.getBeanFactory()).as("parent and child beanFactories")
					.isNotSameAs(ctx.getBeanFactory());
			assertThat(parentCtx.getBeanFactory().getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME))
					.as("parent and child multicasters")
					.isNotSameAs(ctx.getBeanFactory().getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME));

			if (earlyPublishing) {
				parentCtx.prepareRefresh();
				ctx.prepareRefresh();
				assertThatIllegalStateException().as("before refresh").isThrownBy(ctx::getApplicationEventMulticaster);
			}
			else {
				parentCtx.refresh();
				ctx.refresh();
				assertThat(ctx.getApplicationEventMulticaster()).as("multicaster immediately ready")
						.isSameAs(childMulticaster)
						.isNotSameAs(parentCtx.getApplicationEventMulticaster());
			}

			if (explicitEventType != null) {
				ctx.publishEvent(payload, explicitEventType);
			}
			else {
				ctx.publishEvent(payload);
			}

			if (earlyPublishing) {
				//simulate the parts of refresh() that set up the multicaster and send early events
				parentCtx.initApplicationEventMulticaster();
				parentCtx.registerListeners();
				ctx.initApplicationEventMulticaster();
				ctx.registerListeners();
				assertThat(ctx.getApplicationEventMulticaster()).as("after refresh")
						.isSameAs(childMulticaster)
						.isNotSameAs(parentCtx.getApplicationEventMulticaster());
			}

			assertThat(parentMulticaster.events).singleElement()
					.isInstanceOfSatisfying(PayloadApplicationEvent.class, pae -> {
						assertThat(pae.getPayload()).isSameAs(payload);
						assertThat(pae.getResolvableType()).hasToString(expectedResolvableType);
					});
			assertThat(parentMulticaster.listenerLookupTypes).singleElement()
					.hasToString(expectedResolvableType);

			assertThat(childMulticaster.events).singleElement()
					.isInstanceOfSatisfying(PayloadApplicationEvent.class, pae -> {
						assertThat(pae.getPayload()).isSameAs(payload);
						assertThat(pae.getResolvableType()).hasToString(expectedResolvableType);
					});
			assertThat(childMulticaster.listenerLookupTypes).singleElement()
					.hasToString(expectedResolvableType);
		}
	}

	private static class TestMulticaster extends AbstractApplicationEventMulticaster {

		private List<ApplicationEvent> events = new ArrayList<>();
		private List<ResolvableType> listenerLookupTypes = new ArrayList<>();

		@Override
		public void multicastEvent(ApplicationEvent event) {
			multicastEvent(event, null);
		}

		@Override
		public void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType) {
			//simulate the behavior of SimpleApplicationEventMulticaster
			if (eventType == null) {
				eventType = ResolvableType.forInstance(event);
			}
			if (event instanceof PayloadApplicationEvent<?>) {
				this.events.add(event);
				this.listenerLookupTypes.add(eventType);
			} // ignore other "standard" ApplicationEvents
		}
	}

}
