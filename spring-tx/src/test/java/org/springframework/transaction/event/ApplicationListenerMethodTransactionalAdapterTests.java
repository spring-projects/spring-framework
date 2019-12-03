/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.event;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Stephane Nicoll
 */
public class ApplicationListenerMethodTransactionalAdapterTests {

	@Test
	public void defaultPhase() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "defaultPhase", String.class);
		assertPhase(m, TransactionPhase.AFTER_COMMIT);
	}

	@Test
	public void phaseSet() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "phaseSet", String.class);
		assertPhase(m, TransactionPhase.AFTER_ROLLBACK);
	}

	@Test
	public void phaseAndClassesSet() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "phaseAndClassesSet");
		assertPhase(m, TransactionPhase.AFTER_COMPLETION);
		supportsEventType(true, m, createGenericEventType(String.class));
		supportsEventType(true, m, createGenericEventType(Integer.class));
		supportsEventType(false, m, createGenericEventType(Double.class));
	}

	@Test
	public void valueSet() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "valueSet");
		assertPhase(m, TransactionPhase.AFTER_COMMIT);
		supportsEventType(true, m, createGenericEventType(String.class));
		supportsEventType(false, m, createGenericEventType(Double.class));
	}

	private void assertPhase(Method method, TransactionPhase expected) {
		assertThat(method).as("Method must not be null").isNotNull();
		TransactionalEventListener annotation =
				AnnotatedElementUtils.findMergedAnnotation(method, TransactionalEventListener.class);
		assertThat(annotation.phase()).as("Wrong phase for '" + method + "'").isEqualTo(expected);
	}

	private void supportsEventType(boolean match, Method method, ResolvableType eventType) {
		ApplicationListenerMethodAdapter adapter = createTestInstance(method);
		assertThat(adapter.supportsEventType(eventType)).as("Wrong match for event '" + eventType + "' on " + method).isEqualTo(match);
	}

	private ApplicationListenerMethodTransactionalAdapter createTestInstance(Method m) {
		return new ApplicationListenerMethodTransactionalAdapter("test", SampleEvents.class, m);
	}

	private ResolvableType createGenericEventType(Class<?> payloadType) {
		return ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, payloadType);
	}


	static class SampleEvents {

		@TransactionalEventListener
		public void defaultPhase(String data) {
		}

		@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
		public void phaseSet(String data) {
		}

		@TransactionalEventListener(classes = {String.class, Integer.class},
				phase = TransactionPhase.AFTER_COMPLETION)
		public void phaseAndClassesSet() {
		}

		@TransactionalEventListener(String.class)
		public void valueSet() {
		}
	}

}
