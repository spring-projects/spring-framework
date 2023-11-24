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

package org.springframework.transaction.support;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.MockCallbackPreferringTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TestTransactionExecutionListener;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;
import static org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_MANDATORY;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_SUPPORTS;
import static org.springframework.transaction.support.AbstractPlatformTransactionManager.SYNCHRONIZATION_ON_ACTUAL_TRANSACTION;
import static org.springframework.transaction.support.DefaultTransactionDefinition.PREFIX_ISOLATION;
import static org.springframework.transaction.support.DefaultTransactionDefinition.PREFIX_PROPAGATION;

/**
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 29.04.2003
 */
class TransactionSupportTests {

	@AfterEach
	void postConditions() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
	}

	@Test
	void noExistingTransaction() {
		PlatformTransactionManager tm = new TestTransactionManager(false, true);

		DefaultTransactionStatus status1 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_SUPPORTS));
		assertThat(status1.hasTransaction()).as("Must not have transaction").isFalse();

		DefaultTransactionStatus status2 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRED));
		assertThat(status2.hasTransaction()).as("Must have transaction").isTrue();
		assertThat(status2.isNewTransaction()).as("Must be new transaction").isTrue();

		assertThatExceptionOfType(IllegalTransactionStateException.class)
				.isThrownBy(() -> tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_MANDATORY)));
	}

	@Test
	void existingTransaction() {
		PlatformTransactionManager tm = new TestTransactionManager(true, true);

		DefaultTransactionStatus status1 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_SUPPORTS));
		assertThat(status1.getTransaction()).as("Must have transaction").isNotNull();
		assertThat(status1.isNewTransaction()).as("Must not be new transaction").isFalse();

		DefaultTransactionStatus status2 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_REQUIRED));
		assertThat(status2.getTransaction()).as("Must have transaction").isNotNull();
		assertThat(status2.isNewTransaction()).as("Must not be new transaction").isFalse();

		DefaultTransactionStatus status3 = (DefaultTransactionStatus)
				tm.getTransaction(new DefaultTransactionDefinition(PROPAGATION_MANDATORY));
		assertThat(status3.getTransaction()).as("Must have transaction").isNotNull();
		assertThat(status3.isNewTransaction()).as("Must not be new transaction").isFalse();
	}

	@Test
	void commitWithoutExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		TransactionStatus status = tm.getTransaction(null);
		tm.commit(status);

		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("triggered commit").isTrue();
		assertThat(tm.rollback).as("no rollback").isFalse();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isTrue();
		assertThat(tl.afterCommitCalled).isTrue();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void rollbackWithoutExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		TransactionStatus status = tm.getTransaction(null);
		tm.rollback(status);

		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("triggered rollback").isTrue();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isTrue();
		assertThat(tl.afterRollbackCalled).isTrue();
	}

	@Test
	void rollbackOnlyWithoutExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		TransactionStatus status = tm.getTransaction(null);
		status.setRollbackOnly();
		tm.commit(status);

		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("triggered rollback").isTrue();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

		assertThat(tl.beforeBeginCalled).isTrue();
		assertThat(tl.afterBeginCalled).isTrue();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isTrue();
		assertThat(tl.afterRollbackCalled).isTrue();
	}

	@Test
	void commitWithExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(true, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		TransactionStatus status = tm.getTransaction(null);
		tm.commit(status);

		assertThat(tm.begin).as("no begin").isFalse();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("no rollback").isFalse();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();

		assertThat(tl.beforeBeginCalled).isFalse();
		assertThat(tl.afterBeginCalled).isFalse();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void rollbackWithExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(true, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		TransactionStatus status = tm.getTransaction(null);
		tm.rollback(status);

		assertThat(tm.begin).as("no begin").isFalse();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("no rollback").isFalse();
		assertThat(tm.rollbackOnly).as("triggered rollbackOnly").isTrue();

		assertThat(tl.beforeBeginCalled).isFalse();
		assertThat(tl.afterBeginCalled).isFalse();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void rollbackOnlyWithExistingTransaction() {
		TestTransactionManager tm = new TestTransactionManager(true, true);
		TestTransactionExecutionListener tl = new TestTransactionExecutionListener();
		tm.addListener(tl);

		TransactionStatus status = tm.getTransaction(null);
		status.setRollbackOnly();
		tm.commit(status);

		assertThat(tm.begin).as("no begin").isFalse();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("no rollback").isFalse();
		assertThat(tm.rollbackOnly).as("triggered rollbackOnly").isTrue();

		assertThat(tl.beforeBeginCalled).isFalse();
		assertThat(tl.afterBeginCalled).isFalse();
		assertThat(tl.beforeCommitCalled).isFalse();
		assertThat(tl.afterCommitCalled).isFalse();
		assertThat(tl.beforeRollbackCalled).isFalse();
		assertThat(tl.afterRollbackCalled).isFalse();
	}

	@Test
	void transactionTemplate() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate(tm);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			}
		});

		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("triggered commit").isTrue();
		assertThat(tm.rollback).as("no rollback").isFalse();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
	}

	@Test
	void transactionTemplateWithCallbackPreference() {
		MockCallbackPreferringTransactionManager ptm = new MockCallbackPreferringTransactionManager();
		TransactionTemplate template = new TransactionTemplate(ptm);
		template.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
			}
		});

		assertThat(ptm.getDefinition()).isSameAs(template);
		assertThat(ptm.getStatus().isRollbackOnly()).isFalse();
	}

	@Test
	void transactionTemplateWithException() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate(tm);
		RuntimeException ex = new RuntimeException("Some application exception");
		assertThatRuntimeException()
				.isThrownBy(() -> template.execute(new TransactionCallbackWithoutResult() {
					@Override
					protected void doInTransactionWithoutResult(TransactionStatus status) {
						throw ex;
					}
				}))
				.isSameAs(ex);
		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("triggered rollback").isTrue();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
	}

	@SuppressWarnings("serial")
	@Test
	void transactionTemplateWithRollbackException() {
		final TransactionSystemException tex = new TransactionSystemException("system exception");
		TestTransactionManager tm = new TestTransactionManager(false, true) {
			@Override
			protected void doRollback(DefaultTransactionStatus status) {
				super.doRollback(status);
				throw tex;
			}
		};
		TransactionTemplate template = new TransactionTemplate(tm);
		RuntimeException ex = new RuntimeException("Some application exception");
		assertThatRuntimeException()
				.isThrownBy(() -> template.executeWithoutResult(status -> { throw ex; }))
				.isSameAs(tex);
		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("triggered rollback").isTrue();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
	}

	@Test
	void transactionTemplateWithError() {
		TestTransactionManager tm = new TestTransactionManager(false, true);
		TransactionTemplate template = new TransactionTemplate(tm);
		assertThatExceptionOfType(Error.class)
				.isThrownBy(() -> template.executeWithoutResult(status -> { throw new Error("Some application error"); }));
		assertThat(tm.begin).as("triggered begin").isTrue();
		assertThat(tm.commit).as("no commit").isFalse();
		assertThat(tm.rollback).as("triggered rollback").isTrue();
		assertThat(tm.rollbackOnly).as("no rollbackOnly").isFalse();
	}

	@Test
	void transactionTemplateEquality() {
		TestTransactionManager tm1 = new TestTransactionManager(false, true);
		TestTransactionManager tm2 = new TestTransactionManager(false, true);
		TransactionTemplate template1 = new TransactionTemplate(tm1);
		TransactionTemplate template2 = new TransactionTemplate(tm2);
		TransactionTemplate template3 = new TransactionTemplate(tm2);

		assertThat(template2).isNotEqualTo(template1);
		assertThat(template3).isNotEqualTo(template1);
		assertThat(template3).isEqualTo(template2);
	}


	@Nested
	class AbstractPlatformTransactionManagerConfigurationTests {

		private final AbstractPlatformTransactionManager tm = new TestTransactionManager(false, true);

		@Test
		void setTransactionSynchronizationNameToUnsupportedValues() {
			assertThatIllegalArgumentException().isThrownBy(() -> tm.setTransactionSynchronizationName(null));
			assertThatIllegalArgumentException().isThrownBy(() -> tm.setTransactionSynchronizationName("   "));
			assertThatIllegalArgumentException().isThrownBy(() -> tm.setTransactionSynchronizationName("bogus"));
		}

		/**
		 * Verify that the internal 'constants' map is properly configured for all
		 * SYNCHRONIZATION_ constants defined in {@link AbstractPlatformTransactionManager}.
		 */
		@Test
		void setTransactionSynchronizationNameToAllSupportedValues() {
			Set<Integer> uniqueValues = new HashSet<>();
			streamSynchronizationConstants()
					.forEach(name -> {
						tm.setTransactionSynchronizationName(name);
						int transactionSynchronization = tm.getTransactionSynchronization();
						int expected = AbstractPlatformTransactionManager.constants.get(name);
						assertThat(transactionSynchronization).isEqualTo(expected);
						uniqueValues.add(transactionSynchronization);
					});
			assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(AbstractPlatformTransactionManager.constants.values());
		}

		@Test
		void setTransactionSynchronization() {
			tm.setTransactionSynchronization(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
			assertThat(tm.getTransactionSynchronization()).isEqualTo(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
		}

		private static Stream<String> streamSynchronizationConstants() {
			return Arrays.stream(AbstractPlatformTransactionManager.class.getFields())
					.filter(ReflectionUtils::isPublicStaticFinal)
					.map(Field::getName)
					.filter(name -> name.startsWith("SYNCHRONIZATION_"));
		}
	}


	@Nested
	class TransactionTemplateConfigurationTests {

		private final TransactionTemplate template = new TransactionTemplate();

		@Test
		void setTransactionManager() {
			TestTransactionManager tm = new TestTransactionManager(false, true);
			template.setTransactionManager(tm);
			assertThat(template.getTransactionManager()).as("correct transaction manager set").isSameAs(tm);
		}

		@Test
		void setPropagationBehaviorNameToUnsupportedValues() {
			assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName(null));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName("   "));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName("bogus"));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehaviorName("ISOLATION_SERIALIZABLE"));
		}

		/**
		 * Verify that the internal 'propagationConstants' map is properly configured
		 * for all PROPAGATION_ constants defined in {@link TransactionDefinition}.
		 */
		@Test
		void setPropagationBehaviorNameToAllSupportedValues() {
			Set<Integer> uniqueValues = new HashSet<>();
			streamPropagationConstants()
					.forEach(name -> {
						template.setPropagationBehaviorName(name);
						int propagationBehavior = template.getPropagationBehavior();
						int expected = DefaultTransactionDefinition.propagationConstants.get(name);
						assertThat(propagationBehavior).isEqualTo(expected);
						uniqueValues.add(propagationBehavior);
					});
			assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(DefaultTransactionDefinition.propagationConstants.values());
		}

		@Test
		void setPropagationBehavior() {
			assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehavior(999));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setPropagationBehavior(ISOLATION_SERIALIZABLE));

			template.setPropagationBehavior(PROPAGATION_MANDATORY);
			assertThat(template.getPropagationBehavior()).isEqualTo(PROPAGATION_MANDATORY);
		}

		@Test
		void setIsolationLevelNameToUnsupportedValues() {
			assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName(null));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName("   "));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName("bogus"));
			assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevelName("PROPAGATION_MANDATORY"));
		}

		/**
		 * Verify that the internal 'isolationConstants' map is properly configured
		 * for all ISOLATION_ constants defined in {@link TransactionDefinition}.
		 */
		@Test
		void setIsolationLevelNameToAllSupportedValues() {
			Set<Integer> uniqueValues = new HashSet<>();
			streamIsolationConstants()
					.forEach(name -> {
						template.setIsolationLevelName(name);
						int isolationLevel = template.getIsolationLevel();
						int expected = DefaultTransactionDefinition.isolationConstants.get(name);
						assertThat(isolationLevel).isEqualTo(expected);
						uniqueValues.add(isolationLevel);
					});
			assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(DefaultTransactionDefinition.isolationConstants.values());
		}

		@Test
		void setIsolationLevel() {
			assertThatIllegalArgumentException().isThrownBy(() -> template.setIsolationLevel(999));

			template.setIsolationLevel(ISOLATION_REPEATABLE_READ);
			assertThat(template.getIsolationLevel()).isEqualTo(ISOLATION_REPEATABLE_READ);
		}

		@Test
		void getDefinitionDescription() {
			assertThat(template.getDefinitionDescription()).asString()
					.isEqualTo("PROPAGATION_REQUIRED,ISOLATION_DEFAULT");

			template.setPropagationBehavior(PROPAGATION_MANDATORY);
			template.setIsolationLevel(ISOLATION_REPEATABLE_READ);
			template.setReadOnly(true);
			template.setTimeout(42);
			assertThat(template.getDefinitionDescription()).asString()
					.isEqualTo("PROPAGATION_MANDATORY,ISOLATION_REPEATABLE_READ,timeout_42,readOnly");
		}

		private static Stream<String> streamPropagationConstants() {
			return streamTransactionDefinitionConstants()
					.filter(name -> name.startsWith(PREFIX_PROPAGATION));
		}

		private static Stream<String> streamIsolationConstants() {
			return streamTransactionDefinitionConstants()
					.filter(name -> name.startsWith(PREFIX_ISOLATION));
		}

		private static Stream<String> streamTransactionDefinitionConstants() {
			return Arrays.stream(TransactionDefinition.class.getFields())
					.filter(ReflectionUtils::isPublicStaticFinal)
					.map(Field::getName);
		}
	}

}
