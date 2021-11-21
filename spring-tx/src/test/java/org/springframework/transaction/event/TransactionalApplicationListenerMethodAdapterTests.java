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

package org.springframework.transaction.event;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationListenerMethodAdapter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 */
public class TransactionalApplicationListenerMethodAdapterTests {

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

	@Test
	public void invokesCompletionCallbackOnSuccess() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "defaultPhase", String.class);
		CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
		PayloadApplicationEvent<Object> event = new PayloadApplicationEvent<>(this, new Object());

		TransactionalApplicationListenerMethodAdapter adapter = createTestInstance(m);
		adapter.addCallback(callback);
		runInTransaction(() -> adapter.onApplicationEvent(event));

		assertThat(callback.preEvent).isEqualTo(event);
		assertThat(callback.postEvent).isEqualTo(event);
		assertThat(callback.ex).isNull();
		assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
		assertThat(adapter.getListenerId()).endsWith("SampleEvents.defaultPhase(java.lang.String)");
	}

	@Test
	public void invokesExceptionHandlerOnException() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "throwing", String.class);
		CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");

		TransactionalApplicationListenerMethodAdapter adapter = createTestInstance(m);
		adapter.addCallback(callback);

		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> runInTransaction(() -> adapter.onApplicationEvent(event)))
				.withMessage("event");

		assertThat(callback.preEvent).isEqualTo(event);
		assertThat(callback.postEvent).isEqualTo(event);
		assertThat(callback.ex).isInstanceOf(RuntimeException.class);
		assertThat(callback.ex.getMessage()).isEqualTo("event");
		assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
		assertThat(adapter.getListenerId()).isEqualTo(ClassUtils.getQualifiedMethodName(m) + "(java.lang.String)");
	}

	@Test
	public void usesAnnotatedIdentifier() {
		Method m = ReflectionUtils.findMethod(SampleEvents.class, "identified", String.class);
		CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
		PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");

		TransactionalApplicationListenerMethodAdapter adapter = createTestInstance(m);
		adapter.addCallback(callback);
		runInTransaction(() -> adapter.onApplicationEvent(event));

		assertThat(callback.preEvent).isEqualTo(event);
		assertThat(callback.postEvent).isEqualTo(event);
		assertThat(callback.ex).isNull();
		assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
		assertThat(adapter.getListenerId()).endsWith("identifier");
	}


	private static void assertPhase(Method method, TransactionPhase expected) {
		assertThat(method).as("Method must not be null").isNotNull();
		TransactionalEventListener annotation =
				AnnotatedElementUtils.findMergedAnnotation(method, TransactionalEventListener.class);
		assertThat(annotation.phase()).as("Wrong phase for '" + method + "'").isEqualTo(expected);
	}

	private static void supportsEventType(boolean match, Method method, ResolvableType eventType) {
		ApplicationListenerMethodAdapter adapter = createTestInstance(method);
		assertThat(adapter.supportsEventType(eventType)).as("Wrong match for event '" + eventType + "' on " + method).isEqualTo(match);
	}

	private static TransactionalApplicationListenerMethodAdapter createTestInstance(Method m) {
		return new TransactionalApplicationListenerMethodAdapter("test", SampleEvents.class, m) {
			@Override
			protected Object getTargetBean() {
				return new SampleEvents();
			}
		};
	}

	private static ResolvableType createGenericEventType(Class<?> payloadType) {
		return ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, payloadType);
	}

	private static void runInTransaction(Runnable runnable) {
		TransactionSynchronizationManager.setActualTransactionActive(true);
		TransactionSynchronizationManager.initSynchronization();
		try {
			runnable.run();
			TransactionSynchronizationManager.getSynchronizations().forEach(it -> {
				it.beforeCommit(false);
				it.afterCommit();
				it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
			});
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
			TransactionSynchronizationManager.setActualTransactionActive(false);
		}
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

		@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
		public void throwing(String data) {
			throw new RuntimeException(data);
		}

		@TransactionalEventListener(id = "identifier")
		public void identified(String data) {
		}
	}

}
