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

package org.springframework.context.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
public class PayloadApplicationEventTests {

	@Test
	public void testEventClassWithInterface() {
		ApplicationContext ac = new AnnotationConfigApplicationContext(AuditableListener.class);

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(ac.getBean(AuditableListener.class).events.contains(event)).isTrue();
	}

	@Test
	public void testProgrammaticEventListener() {
		List<Auditable> events = new ArrayList<>();
		ApplicationListener<AuditablePayloadEvent<String>> listener = events::add;

		ConfigurableApplicationContext ac = new GenericApplicationContext();
		ac.addApplicationListener(listener);
		ac.refresh();

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(events.contains(event)).isTrue();
	}

	@Test
	public void testProgrammaticPayloadListener() {
		List<String> events = new ArrayList<>();
		ApplicationListener<PayloadApplicationEvent<String>> listener = ApplicationListener.forPayload(events::add);

		ConfigurableApplicationContext ac = new GenericApplicationContext();
		ac.addApplicationListener(listener);
		ac.refresh();

		AuditablePayloadEvent<String> event = new AuditablePayloadEvent<>(this, "xyz");
		ac.publishEvent(event);
		assertThat(events.contains(event.getPayload())).isTrue();
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

}
