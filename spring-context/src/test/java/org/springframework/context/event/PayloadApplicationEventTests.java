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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class PayloadApplicationEventTests {

	@Test
	void payloadApplicationEventWithNoTypeUsesInstance() {
		NumberHolder<Integer> payload = new NumberHolder<>(42);
		PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this, payload);
		assertThat(event.getResolvableType()).satisfies(eventType -> {
			assertThat(eventType.toClass()).isEqualTo(PayloadApplicationEvent.class);
			assertThat(eventType.getGenerics())
				.hasSize(1)
				.allSatisfy(bodyType -> {
					assertThat(bodyType.toClass()).isEqualTo(NumberHolder.class);
					assertThat(bodyType.hasUnresolvableGenerics()).isTrue();
				});
		});
	}

	@Test
	void payloadApplicationEventWithType() {
		NumberHolder<Integer> payload = new NumberHolder<>(42);
		ResolvableType payloadType = ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class);
		PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this, payload, payloadType);
		assertThat(event.getResolvableType()).satisfies(eventType -> {
			assertThat(eventType.toClass()).isEqualTo(PayloadApplicationEvent.class);
			assertThat(eventType.getGenerics())
				.hasSize(1)
				.allSatisfy(bodyType -> {
					assertThat(bodyType.toClass()).isEqualTo(NumberHolder.class);
					assertThat(bodyType.hasUnresolvableGenerics()).isFalse();
					assertThat(bodyType.getGenerics()[0].toClass()).isEqualTo(Integer.class);
				});
		});
	}

	@Test
	@SuppressWarnings("resource")
	void testEventClassWithPayloadType() {
		ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(NumberHolderListener.class);

		PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this,
				new NumberHolder<>(42), ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		ac.publishEvent(event);
		assertThat(ac.getBean(NumberHolderListener.class).events).contains(event.getPayload());
		ac.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testEventClassWithPayloadTypeOnParentContext() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(NumberHolderListener.class);
		ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
		ac.refresh();

		PayloadApplicationEvent<NumberHolder<Integer>> event = new PayloadApplicationEvent<>(this,
				new NumberHolder<>(42), ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
		ac.publishEvent(event);
		assertThat(parent.getBean(NumberHolderListener.class).events).contains(event.getPayload());
		ac.close();
		parent.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testPayloadObjectWithPayloadType() {
		final NumberHolder<Integer> payload = new NumberHolder<>(42);

		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(NumberHolderListener.class) {
			@Override
			protected void finishRefresh() throws BeansException {
				super.finishRefresh();
				// This is not recommended: use publishEvent(new PayloadApplicationEvent(...)) instead
				publishEvent(payload, ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
			}
		};

		assertThat(ac.getBean(NumberHolderListener.class).events).contains(payload);
		ac.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testPayloadObjectWithPayloadTypeOnParentContext() {
		final NumberHolder<Integer> payload = new NumberHolder<>(42);

		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(NumberHolderListener.class);
		ConfigurableApplicationContext ac = new GenericApplicationContext(parent) {
			@Override
			protected void finishRefresh() throws BeansException {
				super.finishRefresh();
				// This is not recommended: use publishEvent(new PayloadApplicationEvent(...)) instead
				publishEvent(payload, ResolvableType.forClassWithGenerics(NumberHolder.class, Integer.class));
			}
		};
		ac.refresh();

		assertThat(parent.getBean(NumberHolderListener.class).events).contains(payload);
		ac.close();
		parent.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testEventClassWithInterface() {
		ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(AuditableListener.class);

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(ac.getBean(AuditableListener.class).events).contains(event);
		ac.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testEventClassWithInterfaceOnParentContext() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(AuditableListener.class);
		ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
		ac.refresh();

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(parent.getBean(AuditableListener.class).events).contains(event);
		ac.close();
		parent.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testProgrammaticEventListener() {
		List<Auditable> events = new ArrayList<>();
		ApplicationListener<AuditablePayloadEvent<String>> listener = events::add;
		ApplicationListener<AuditablePayloadEvent<Integer>> mismatch = (PayloadApplicationEvent::getPayload);

		ConfigurableApplicationContext ac = new GenericApplicationContext();
		ac.addApplicationListener(listener);
		ac.addApplicationListener(mismatch);
		ac.refresh();

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(events).contains(event);
		ac.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testProgrammaticEventListenerOnParentContext() {
		List<Auditable> events = new ArrayList<>();
		ApplicationListener<AuditablePayloadEvent<String>> listener = events::add;
		ApplicationListener<AuditablePayloadEvent<Integer>> mismatch = (PayloadApplicationEvent::getPayload);

		ConfigurableApplicationContext parent = new GenericApplicationContext();
		parent.addApplicationListener(listener);
		parent.addApplicationListener(mismatch);
		parent.refresh();
		ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
		ac.refresh();

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(events).contains(event);
		ac.close();
		parent.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testProgrammaticPayloadListener() {
		List<String> events = new ArrayList<>();
		ApplicationListener<PayloadApplicationEvent<String>> listener = ApplicationListener.forPayload(events::add);
		ApplicationListener<PayloadApplicationEvent<Integer>> mismatch = ApplicationListener.forPayload(Integer::intValue);

		ConfigurableApplicationContext ac = new GenericApplicationContext();
		ac.addApplicationListener(listener);
		ac.addApplicationListener(mismatch);
		ac.refresh();

		String payload = "xyz";
		ac.publishEvent(payload);
		assertThat(events).contains(payload);
		ac.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testProgrammaticPayloadListenerOnParentContext() {
		List<String> events = new ArrayList<>();
		ApplicationListener<PayloadApplicationEvent<String>> listener = ApplicationListener.forPayload(events::add);
		ApplicationListener<PayloadApplicationEvent<Integer>> mismatch = ApplicationListener.forPayload(Integer::intValue);

		ConfigurableApplicationContext parent = new GenericApplicationContext();
		parent.addApplicationListener(listener);
		parent.addApplicationListener(mismatch);
		parent.refresh();
		ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
		ac.refresh();

		String payload = "xyz";
		ac.publishEvent(payload);
		assertThat(events).contains(payload);
		ac.close();
		parent.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testPlainPayloadListener() {
		ConfigurableApplicationContext ac = new AnnotationConfigApplicationContext(PlainPayloadListener.class);

		String payload = "xyz";
		ac.publishEvent(payload);
		assertThat(ac.getBean(PlainPayloadListener.class).events).contains(payload);
		ac.close();
	}

	@Test
	@SuppressWarnings("resource")
	void testPlainPayloadListenerOnParentContext() {
		ConfigurableApplicationContext parent = new AnnotationConfigApplicationContext(PlainPayloadListener.class);
		ConfigurableApplicationContext ac = new GenericApplicationContext(parent);
		ac.refresh();

		String payload = "xyz";
		ac.publishEvent(payload);
		assertThat(parent.getBean(PlainPayloadListener.class).events).contains(payload);
		ac.close();
		parent.close();
	}


	static class NumberHolder<T extends Number> {

		public NumberHolder(T number) {
		}
	}


	@Component
	public static class NumberHolderListener {

		public final List<NumberHolder<Integer>> events = new ArrayList<>();

		@EventListener
		public void onEvent(NumberHolder<Integer> event) {
			events.add(event);
		}
	}


	public interface Auditable {
	}


	@SuppressWarnings("serial")
	public static class AuditablePayloadEvent<T> extends PayloadApplicationEvent<T> implements Auditable {

		public AuditablePayloadEvent(Object source, T payload) {
			super(source, payload);
		}
	}


	@Component
	public static class AuditableListener {

		public final List<Auditable> events = new ArrayList<>();

		@EventListener
		public void onEvent(Auditable event) {
			events.add(event);
		}
	}


	@Component
	public static class PlainPayloadListener {

		public final List<String> events = new ArrayList<>();

		@EventListener
		public void onEvent(String event) {
			events.add(event);
		}
	}

}
