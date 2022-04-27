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

package org.springframework.transaction.interceptor;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 09.04.2003
 */
class RuleBasedTransactionAttributeTests {

	@Test
	void defaultRule() {
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute();
		assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(rta.rollbackOn(new MyRuntimeException())).isTrue();
		assertThat(rta.rollbackOn(new Exception())).isFalse();
		assertThat(rta.rollbackOn(new IOException())).isFalse();
	}

	/**
	 * Test one checked exception that should roll back.
	 */
	@Test
	void ruleForRollbackOnChecked() {
		List<RollbackRuleAttribute> list = new ArrayList<>();
		list.add(new RollbackRuleAttribute(IOException.class.getName()));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(rta.rollbackOn(new MyRuntimeException())).isTrue();
		assertThat(rta.rollbackOn(new Exception())).isFalse();
		// Check that default behaviour is overridden
		assertThat(rta.rollbackOn(new IOException())).isTrue();
	}

	@Test
	void ruleForCommitOnUnchecked() {
		List<RollbackRuleAttribute> list = new ArrayList<>();
		list.add(new NoRollbackRuleAttribute(MyRuntimeException.class.getName()));
		list.add(new RollbackRuleAttribute(IOException.class.getName()));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
		// Check default behaviour is overridden
		assertThat(rta.rollbackOn(new MyRuntimeException())).isFalse();
		assertThat(rta.rollbackOn(new Exception())).isFalse();
		// Check that default behaviour is overridden
		assertThat(rta.rollbackOn(new IOException())).isTrue();
	}

	@Test
	void ruleForSelectiveRollbackOnCheckedWithString() {
		List<RollbackRuleAttribute> l = new ArrayList<>();
		l.add(new RollbackRuleAttribute(java.rmi.RemoteException.class.getName()));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
		doTestRuleForSelectiveRollbackOnChecked(rta);
	}

	@Test
	void ruleForSelectiveRollbackOnCheckedWithClass() {
		List<RollbackRuleAttribute> l = Collections.singletonList(new RollbackRuleAttribute(RemoteException.class));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
		doTestRuleForSelectiveRollbackOnChecked(rta);
	}

	private void doTestRuleForSelectiveRollbackOnChecked(RuleBasedTransactionAttribute rta) {
		assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
		// Check default behaviour is overridden
		assertThat(rta.rollbackOn(new Exception())).isFalse();
		// Check that default behaviour is overridden
		assertThat(rta.rollbackOn(new RemoteException())).isTrue();
	}

	/**
	 * Check that a rule can cause commit on a IOException
	 * when Exception prompts a rollback.
	 */
	@Test
	void ruleForCommitOnSubclassOfChecked() {
		List<RollbackRuleAttribute> list = new ArrayList<>();
		// Note that it's important to ensure that we have this as
		// a FQN: otherwise it will match everything!
		list.add(new RollbackRuleAttribute("java.lang.Exception"));
		list.add(new NoRollbackRuleAttribute("IOException"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertThat(rta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(rta.rollbackOn(new Exception())).isTrue();
		// Check that default behaviour is overridden
		assertThat(rta.rollbackOn(new IOException())).isFalse();
	}

	@Test
	void rollbackNever() {
		List<RollbackRuleAttribute> list = new ArrayList<>();
		list.add(new NoRollbackRuleAttribute("Throwable"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertThat(rta.rollbackOn(new Throwable())).isFalse();
		assertThat(rta.rollbackOn(new RuntimeException())).isFalse();
		assertThat(rta.rollbackOn(new MyRuntimeException())).isFalse();
		assertThat(rta.rollbackOn(new Exception())).isFalse();
		assertThat(rta.rollbackOn(new IOException())).isFalse();
	}

	@Test
	void toStringMatchesEditor() {
		List<RollbackRuleAttribute> list = new ArrayList<>();
		list.add(new NoRollbackRuleAttribute("Throwable"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		tae.setAsText(rta.toString());
		rta = (RuleBasedTransactionAttribute) tae.getValue();

		assertThat(rta.rollbackOn(new Throwable())).isFalse();
		assertThat(rta.rollbackOn(new RuntimeException())).isFalse();
		assertThat(rta.rollbackOn(new MyRuntimeException())).isFalse();
		assertThat(rta.rollbackOn(new Exception())).isFalse();
		assertThat(rta.rollbackOn(new IOException())).isFalse();
	}

	/**
	 * See <a href="https://forum.springframework.org/showthread.php?t=41350">this forum post</a>.
	 */
	@Test
	void conflictingRulesToDetermineExactContract() {
		List<RollbackRuleAttribute> list = new ArrayList<>();
		list.add(new NoRollbackRuleAttribute(MyBusinessWarningException.class));
		list.add(new RollbackRuleAttribute(MyBusinessException.class));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertThat(rta.rollbackOn(new MyBusinessException())).isTrue();
		assertThat(rta.rollbackOn(new MyBusinessWarningException())).isFalse();
	}


	@SuppressWarnings("serial")
	private static class MyBusinessException extends Exception {}


	@SuppressWarnings("serial")
	private static final class MyBusinessWarningException extends MyBusinessException {}

}
