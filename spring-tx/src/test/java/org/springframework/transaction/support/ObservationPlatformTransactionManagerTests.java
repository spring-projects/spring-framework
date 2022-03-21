/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.transaction.support;

import io.micrometer.core.tck.TestObservationRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.TransactionStatus;

import static io.micrometer.core.tck.TestObservationRegistryAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ObservationPlatformTransactionManagerTests {

	TestObservationRegistry observationRegistry = TestObservationRegistry.create();

	PlatformTransactionManager delegate = mock(PlatformTransactionManager.class);

	private static final TransactionManager transactionManager = new TransactionManager() {

	};

	@AfterEach
	void noLeakingObservationRemaining() {
		assertThat(this.observationRegistry).doesNotHaveAnyRemainingCurrentObservation();
	}

	@Test
	void getTransactionLeavesOpenScopeWhenPreviousObservationWasPresent() {
		try (Observation.Scope scope = Observation.start("parent", this.observationRegistry).openScope()) {
			ObservationPlatformTransactionManager manager = observationPlatformTransactionManager();

			manager.getTransaction(new DefaultTransactionDefinition());

			assertThat(this.observationRegistry)
					.hasRemainingCurrentObservation()
					.doesNotHaveRemainingCurrentObservationSameAs(scope.getCurrentObservation());
			then(this.delegate).should().getTransaction(any());
		}
	}

	@Test
	void getTransactionJustDelegatesWhenNoPreviousObservationPresent() {
		ObservationPlatformTransactionManager manager = observationPlatformTransactionManager();

		manager.getTransaction(new DefaultTransactionDefinition());

		assertThat(this.observationRegistry).doesNotHaveAnyRemainingCurrentObservation();
		then(this.delegate).should().getTransaction(any());
	}

	@Test
	void commitAfterGetTransaction() {
		try (Observation.Scope scope = Observation.start("parent", this.observationRegistry).openScope()) {
			ObservationPlatformTransactionManager manager = observationPlatformTransactionManager();

			// when
			TransactionStatus transaction = manager.getTransaction(new DefaultTransactionDefinition());

			// then
			assertThat(this.observationRegistry)
					.hasRemainingCurrentObservation()
					.doesNotHaveRemainingCurrentObservationSameAs(scope.getCurrentObservation());
			then(this.delegate).should().getTransaction(any());

			// when
			manager.commit(transaction);

			// then
			assertThatRegistryHasAStoppedTxObservation(scope);
			then(this.delegate).should().commit(any());
		}
	}

	@Test
	void rollbackAfterGetTransaction() {
		try (Observation.Scope scope = Observation.start("parent", this.observationRegistry).openScope()) {
			ObservationPlatformTransactionManager manager = observationPlatformTransactionManager();

			// when
			TransactionStatus transaction = manager.getTransaction(new DefaultTransactionDefinition());
			then(this.delegate).should().getTransaction(any());

			// then
			assertThat(this.observationRegistry).doesNotHaveRemainingCurrentObservationSameAs(scope.getCurrentObservation());

			// when
			manager.rollback(transaction);

			// then
			assertThatRegistryHasAStoppedTxObservation(scope);
			then(this.delegate).should().rollback(any());
		}
	}

	@Test
	void commitForNoOpRegistryDelegatesToManagerWithoutCallingObservation() {
		TransactionObservationContext context = mock(TransactionObservationContext.class);
		ObservationPlatformTransactionManager manager = noOpDelegateObservationPlatformTransactionManager(context);

		manager.commit(null);

		then(this.delegate).should().commit(null);
		verifyNoInteractions(context);
	}

	@Test
	void rollbackForNoOpRegistryDelegatesToManagerWithoutCallingObservation() {
		TransactionObservationContext context = mock(TransactionObservationContext.class);
		ObservationPlatformTransactionManager manager = noOpDelegateObservationPlatformTransactionManager(context);

		manager.rollback(null);

		then(this.delegate).should().rollback(null);
		verifyNoInteractions(context);
	}

	private ObservationPlatformTransactionManager observationPlatformTransactionManager() {
		TransactionObservationContext context = new TransactionObservationContext(transactionDefinition(), transactionManager);
		given(this.delegate.getTransaction(any())).willReturn(new SimpleTransactionStatus());
		return new ObservationPlatformTransactionManager(this.delegate, this.observationRegistry, context, new DefaultTransactionTagsProvider());
	}

	private ObservationPlatformTransactionManager noOpDelegateObservationPlatformTransactionManager(TransactionObservationContext context) {
		return new ObservationPlatformTransactionManager(this.delegate, ObservationRegistry.NOOP, context, new DefaultTransactionTagsProvider());
	}

	private void assertThatRegistryHasAStoppedTxObservation(Observation.Scope scope) {
		assertThat(this.observationRegistry)
				.hasRemainingCurrentObservationSameAs(scope.getCurrentObservation())
				.hasObservationWithNameEqualTo("spring.tx").that()
				.hasLowCardinalityTag("spring.tx.isolation-level", "-1")
				.hasLowCardinalityTag("spring.tx.propagation-level", "REQUIRED")
				.hasLowCardinalityTag("spring.tx.read-only", "false")
				.hasLowCardinalityTag("spring.tx.transaction-manager", "org.springframework.transaction.support.ObservationPlatformTransactionManagerTests$1")
				.hasHighCardinalityTag("spring.tx.name", "foo")
				.hasBeenStarted()
				.hasBeenStopped();
	}

	private DefaultTransactionDefinition transactionDefinition() {
		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setName("foo");
		return definition;
	}

}
