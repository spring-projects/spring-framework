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

package org.springframework.transaction.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.testfixture.CallCountingTransactionManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;
import static org.springframework.transaction.event.TransactionPhase.AFTER_COMPLETION;
import static org.springframework.transaction.event.TransactionPhase.AFTER_ROLLBACK;
import static org.springframework.transaction.event.TransactionPhase.BEFORE_COMMIT;

/**
 * Integration tests for {@link TransactionalEventListener} support
 * with thread-bound transactions.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 */
class TransactionalEventListenerTests {

	private ConfigurableApplicationContext context;

	private EventCollector eventCollector;

	private TransactionTemplate transactionTemplate;


	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}


	@Test
	void immediately() {
		load(ImmediateTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
			getEventCollector().assertTotalEventsCount(1);
			return null;
		});
		getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	void immediatelyImpactsCurrentTransaction() {
		load(ImmediateTestListener.class, BeforeCommitTestListener.class);
		assertThatIllegalStateException().isThrownBy(() ->
				this.transactionTemplate.execute(status -> {
					getContext().publishEvent("FAIL");
					throw new AssertionError("Should have thrown an exception at this point");
				}))
			.withMessageContaining("Test exception")
			.withMessageContaining(EventCollector.IMMEDIATELY);

		getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "FAIL");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	void afterCompletionCommit() {
		load(AfterCompletionTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
	}

	@Test
	void afterCompletionRollback() {
		load(AfterCompletionTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			status.setRollbackOnly();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
	}

	@Test
	void afterCommit() {
		load(AfterCompletionExplicitTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
	}

	@Test
	void afterCommitWithTransactionalComponentListenerProxiedViaDynamicProxy() {
		load(TransactionalComponentTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("SKIP");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertNoEventReceived();
	}

	@Test
	void afterCommitWithTransactionalComponentListenerWithInterfaceProxy() {
		load(TransactionalComponentTestListenerWithInterface.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("SKIP");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertNoEventReceived();
	}

	@Test
	void afterRollback() {
		load(AfterCompletionExplicitTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			status.setRollbackOnly();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_ROLLBACK, "test");
		getEventCollector().assertTotalEventsCount(1); // After commit not invoked
	}

	@Test
	void afterRollbackWithCustomExecutor() {
		load(AfterCompletionExplicitTestListener.class, MulticasterWithCustomExecutor.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			status.setRollbackOnly();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_ROLLBACK, "test");
		getEventCollector().assertTotalEventsCount(1); // After commit not invoked
	}

	@Test
	void beforeCommit() {
		load(BeforeCommitTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(10) {
				@Override
				public void beforeCommit(boolean readOnly) {
					getEventCollector().assertNoEventReceived(); // Not seen yet
				}
			});
			TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(20) {
				@Override
				public void beforeCommit(boolean readOnly) {
					getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
					getEventCollector().assertTotalEventsCount(1);
				}
			});
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			return null;

		});
		getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	void beforeCommitWithException() { // Validates the custom synchronization is invoked
		load(BeforeCommitTestListener.class);
		assertThatIllegalStateException().isThrownBy(() ->
				this.transactionTemplate.execute(status -> {
					TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(10) {
						@Override
						public void beforeCommit(boolean readOnly) {
							throw new IllegalStateException("test");
						}
					});
					getContext().publishEvent("test");
					getEventCollector().assertNoEventReceived();
					return null;

				}));
		getEventCollector().assertNoEventReceived(); // Before commit not invoked
	}

	@Test
	void regularTransaction() {
		load(ImmediateTestListener.class, BeforeCommitTestListener.class, AfterCompletionExplicitTestListener.class);
		this.transactionTemplate.execute(status -> {
			TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(10) {
				@Override
				public void beforeCommit(boolean readOnly) {
					getEventCollector().assertTotalEventsCount(1); // Immediate event
					getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
				}
			});
			TransactionSynchronizationManager.registerSynchronization(new EventTransactionSynchronization(20) {
				@Override
				public void beforeCommit(boolean readOnly) {
					getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
					getEventCollector().assertTotalEventsCount(2);
				}
			});
			getContext().publishEvent("test");
			getEventCollector().assertTotalEventsCount(1);
			return null;

		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(3); // Immediate, before commit, after commit
	}

	@Test
	void noTransaction() {
		load(BeforeCommitTestListener.class, AfterCompletionTestListener.class, AfterCompletionExplicitTestListener.class);
		this.context.publishEvent("test");
		getEventCollector().assertTotalEventsCount(0);
	}

	@Test
	void transactionDemarcationWithNotSupportedPropagation() {
		load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
		getContext().getBean(TestBean.class).notSupported();
		getEventCollector().assertTotalEventsCount(0);
	}

	@Test
	void transactionDemarcationWithSupportsPropagationAndNoTransaction() {
		load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
		getContext().getBean(TestBean.class).supports();
		getEventCollector().assertTotalEventsCount(0);
	}

	@Test
	void transactionDemarcationWithSupportsPropagationAndExistingTransaction() {
		load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().getBean(TestBean.class).supports();
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertTotalEventsCount(2);
	}

	@Test
	void transactionDemarcationWithRequiredPropagation() {
		load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
		getContext().getBean(TestBean.class).required();
		getEventCollector().assertTotalEventsCount(2);
	}

	@Test
	void noTransactionWithFallbackExecution() {
		load(FallbackExecutionTestListener.class);
		this.context.publishEvent("test");
		this.eventCollector.assertEvents(EventCollector.BEFORE_COMMIT, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_COMMIT, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_ROLLBACK, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(4);
	}

	@Test
	void noTransactionManagementWithFallbackExecution() {
		doLoad(PlainConfiguration.class, FallbackExecutionTestListener.class);
		this.context.publishEvent("test");
		this.eventCollector.assertEvents(EventCollector.BEFORE_COMMIT, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_COMMIT, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_ROLLBACK, "test");
		this.eventCollector.assertEvents(EventCollector.AFTER_COMPLETION, "test");
		getEventCollector().assertTotalEventsCount(4);
	}

	@Test
	void noTransactionManagementWithoutFallbackExecution() {
		doLoad(PlainConfiguration.class, BeforeCommitTestListener.class, AfterCommitMetaAnnotationTestListener.class);
		this.context.publishEvent("test");
		this.eventCollector.assertNoEventReceived();
	}

	@Test
	void conditionFoundOnTransactionalEventListener() {
		load(ImmediateTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("SKIP");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertNoEventReceived();
	}

	@Test
	void afterCommitMetaAnnotation() {
		load(AfterCommitMetaAnnotationTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("test");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
		getEventCollector().assertTotalEventsCount(1);
	}

	@Test
	void conditionFoundOnMetaAnnotation() {
		load(AfterCommitMetaAnnotationTestListener.class);
		this.transactionTemplate.execute(status -> {
			getContext().publishEvent("SKIP");
			getEventCollector().assertNoEventReceived();
			return null;
		});
		getEventCollector().assertNoEventReceived();
	}


	protected EventCollector getEventCollector() {
		return this.eventCollector;
	}

	protected ConfigurableApplicationContext getContext() {
		return this.context;
	}

	private void load(Class<?>... classes) {
		List<Class<?>> allClasses = new ArrayList<>();
		allClasses.add(BasicConfiguration.class);
		allClasses.addAll(Arrays.asList(classes));
		doLoad(allClasses.toArray(new Class<?>[0]));
	}

	private void doLoad(Class<?>... classes) {
		this.context = new AnnotationConfigApplicationContext(classes);
		this.eventCollector = this.context.getBean(EventCollector.class);
		this.transactionTemplate = this.context.getBean(TransactionTemplate.class);
	}


	@Configuration
	@EnableTransactionManagement
	static class BasicConfiguration {

		@Bean
		public EventCollector eventCollector() {
			return new EventCollector();
		}

		@Bean
		public TestBean testBean(ApplicationEventPublisher eventPublisher) {
			return new TestBean(eventPublisher);
		}

		@Bean
		public CallCountingTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public TransactionTemplate transactionTemplate() {
			return new TransactionTemplate(transactionManager());
		}
	}


	@Configuration
	static class PlainConfiguration {

		@Bean
		public EventCollector eventCollector() {
			return new EventCollector();
		}

		@Bean
		public TestBean testBean(ApplicationEventPublisher eventPublisher) {
			return new TestBean(eventPublisher);
		}

		@Bean
		public CallCountingTransactionManager transactionManager() {
			return new CallCountingTransactionManager();
		}

		@Bean
		public TransactionTemplate transactionTemplate() {
			return new TransactionTemplate(transactionManager());
		}
	}


	@Configuration
	static class MulticasterWithCustomExecutor {

		@Bean
		public SimpleApplicationEventMulticaster applicationEventMulticaster() {
			SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
			multicaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
			return multicaster;
		}
	}


	static class EventCollector {

		public static final String IMMEDIATELY = "IMMEDIATELY";

		public static final String BEFORE_COMMIT = "BEFORE_COMMIT";

		public static final String AFTER_COMPLETION = "AFTER_COMPLETION";

		public static final String AFTER_COMMIT = "AFTER_COMMIT";

		public static final String AFTER_ROLLBACK = "AFTER_ROLLBACK";

		public static final String[] ALL_PHASES = {IMMEDIATELY, BEFORE_COMMIT, AFTER_COMMIT, AFTER_ROLLBACK};

		private final MultiValueMap<String, Object> events = new LinkedMultiValueMap<>();

		public void addEvent(String phase, Object event) {
			this.events.add(phase, event);
		}

		public List<Object> getEvents(String phase) {
			return this.events.getOrDefault(phase, Collections.emptyList());
		}

		public void assertNoEventReceived(String... phases) {
			if (phases.length == 0) { // All values if none set
				phases = ALL_PHASES;
			}
			for (String phase : phases) {
				List<Object> eventsForPhase = getEvents(phase);
				assertThat(eventsForPhase.size()).as("Expected no events for phase '" + phase + "' " +
								"but got " + eventsForPhase + ":").isEqualTo(0);
			}
		}

		public void assertEvents(String phase, Object... expected) {
			List<Object> actual = getEvents(phase);
			assertThat(actual.size()).as("wrong number of events for phase '" + phase + "'").isEqualTo(expected.length);
			for (int i = 0; i < expected.length; i++) {
				assertThat(actual.get(i)).as("Wrong event for phase '" + phase + "' at index " + i).isEqualTo(expected[i]);
			}
		}

		public void assertTotalEventsCount(int number) {
			int size = 0;
			for (Map.Entry<String, List<Object>> entry : this.events.entrySet()) {
				size += entry.getValue().size();
			}
			assertThat(size).as("Wrong number of total events (" + this.events.size() + ") " +
						"registered phase(s)").isEqualTo(number);
		}
	}


	static class TestBean {

		private final ApplicationEventPublisher eventPublisher;

		TestBean(ApplicationEventPublisher eventPublisher) {
			this.eventPublisher = eventPublisher;
		}

		@Transactional(propagation = Propagation.NOT_SUPPORTED)
		public void notSupported() {
			this.eventPublisher.publishEvent("test");
		}

		@Transactional(propagation = Propagation.SUPPORTS)
		public void supports() {
			this.eventPublisher.publishEvent("test");
		}

		@Transactional(propagation = Propagation.REQUIRED)
		public void required() {
			this.eventPublisher.publishEvent("test");
		}
	}


	abstract static class BaseTransactionalTestListener {

		static final String FAIL_MSG = "FAIL";

		@Autowired
		private EventCollector eventCollector;

		public void handleEvent(String phase, String data) {
			this.eventCollector.addEvent(phase, data);
			if (FAIL_MSG.equals(data)) {
				throw new IllegalStateException("Test exception on phase '" + phase + "'");
			}
		}
	}


	@Component
	static class ImmediateTestListener extends BaseTransactionalTestListener {

		@EventListener(condition = "!'SKIP'.equals(#data)")
		public void handleImmediately(String data) {
			handleEvent(EventCollector.IMMEDIATELY, data);
		}
	}


	@Component
	static class AfterCompletionTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = AFTER_COMPLETION)
		public void handleAfterCompletion(String data) {
			handleEvent(EventCollector.AFTER_COMPLETION, data);
		}
	}


	@Component
	static class AfterCompletionExplicitTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = AFTER_COMMIT)
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_ROLLBACK)
		public void handleAfterRollback(String data) {
			handleEvent(EventCollector.AFTER_ROLLBACK, data);
		}
	}


	@Transactional
	@Component
	interface TransactionalComponentTestListenerInterface {

		// Cannot use #data in condition due to dynamic proxy.
		@TransactionalEventListener(condition = "!'SKIP'.equals(#p0)")
		void handleAfterCommit(String data);
	}


	static class TransactionalComponentTestListener extends BaseTransactionalTestListener implements
			TransactionalComponentTestListenerInterface {

		@Override
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}


	interface TransactionalComponentTestInterface {

		void handleAfterCommit(String data);
	}


	@Transactional
	@Component
	static class TransactionalComponentTestListenerWithInterface extends BaseTransactionalTestListener implements
			TransactionalComponentTestInterface {

		@TransactionalEventListener(condition = "!'SKIP'.equals(#data)")
		@Override
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}


	@Component
	static class BeforeCommitTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = BEFORE_COMMIT)
		@Order(15)
		public void handleBeforeCommit(String data) {
			handleEvent(EventCollector.BEFORE_COMMIT, data);
		}
	}


	@Component
	static class FallbackExecutionTestListener extends BaseTransactionalTestListener {

		@TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)
		public void handleBeforeCommit(String data) {
			handleEvent(EventCollector.BEFORE_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}

		@TransactionalEventListener(phase = AFTER_ROLLBACK, fallbackExecution = true)
		public void handleAfterRollback(String data) {
			handleEvent(EventCollector.AFTER_ROLLBACK, data);
		}

		@TransactionalEventListener(phase = AFTER_COMPLETION, fallbackExecution = true)
		public void handleAfterCompletion(String data) {
			handleEvent(EventCollector.AFTER_COMPLETION, data);
		}
	}


	@TransactionalEventListener(phase = AFTER_COMMIT, condition = "!'SKIP'.equals(#p0)")
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@interface AfterCommitEventListener {
	}


	@Component
	static class AfterCommitMetaAnnotationTestListener extends BaseTransactionalTestListener {

		@AfterCommitEventListener
		public void handleAfterCommit(String data) {
			handleEvent(EventCollector.AFTER_COMMIT, data);
		}
	}


	static class EventTransactionSynchronization implements TransactionSynchronization {

		private final int order;

		EventTransactionSynchronization(int order) {
			this.order = order;
		}

		@Override
		public int getOrder() {
			return order;
		}
	}

}
