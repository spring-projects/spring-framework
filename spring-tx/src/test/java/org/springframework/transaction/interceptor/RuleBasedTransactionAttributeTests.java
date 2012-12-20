/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.transaction.interceptor;

import static org.junit.Assert.*;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Chris Beams
 * @since 09.04.2003
 */
public class RuleBasedTransactionAttributeTests {

	@Test
	public void testDefaultRule() {
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute();
		assertTrue(rta.rollbackOn(new RuntimeException()));
		assertTrue(rta.rollbackOn(new MyRuntimeException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		assertFalse(rta.rollbackOn(new IOException()));
	}

	/**
	 * Test one checked exception that should roll back.
	 */
	@Test
	public void testRuleForRollbackOnChecked() {
		List<RollbackRuleAttribute> list = new LinkedList<RollbackRuleAttribute>();
		list.add(new RollbackRuleAttribute(IOException.class.getName()));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertTrue(rta.rollbackOn(new RuntimeException()));
		assertTrue(rta.rollbackOn(new MyRuntimeException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertTrue(rta.rollbackOn(new IOException()));
	}

	@Test
	public void testRuleForCommitOnUnchecked() {
		List<RollbackRuleAttribute> list = new LinkedList<RollbackRuleAttribute>();
		list.add(new NoRollbackRuleAttribute(MyRuntimeException.class.getName()));
		list.add(new RollbackRuleAttribute(IOException.class.getName()));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertTrue(rta.rollbackOn(new RuntimeException()));
		// Check default behaviour is overridden
		assertFalse(rta.rollbackOn(new MyRuntimeException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertTrue(rta.rollbackOn(new IOException()));
	}

	@Test
	public void testRuleForSelectiveRollbackOnCheckedWithString() {
		List<RollbackRuleAttribute> l = new LinkedList<RollbackRuleAttribute>();
		l.add(new RollbackRuleAttribute(java.rmi.RemoteException.class.getName()));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
		doTestRuleForSelectiveRollbackOnChecked(rta);
	}

	@Test
	public void testRuleForSelectiveRollbackOnCheckedWithClass() {
		List<RollbackRuleAttribute> l = Collections.singletonList(new RollbackRuleAttribute(RemoteException.class));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
		doTestRuleForSelectiveRollbackOnChecked(rta);
	}

	private void doTestRuleForSelectiveRollbackOnChecked(RuleBasedTransactionAttribute rta) {
		assertTrue(rta.rollbackOn(new RuntimeException()));
		// Check default behaviour is overridden
		assertFalse(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertTrue(rta.rollbackOn(new RemoteException()));
	}

	/**
	 * Check that a rule can cause commit on a IOException
	 * when Exception prompts a rollback.
	 */
	@Test
	public void testRuleForCommitOnSubclassOfChecked() {
		List<RollbackRuleAttribute> list = new LinkedList<RollbackRuleAttribute>();
		// Note that it's important to ensure that we have this as
		// a FQN: otherwise it will match everything!
		list.add(new RollbackRuleAttribute("java.lang.Exception"));
		list.add(new NoRollbackRuleAttribute("IOException"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertTrue(rta.rollbackOn(new RuntimeException()));
		assertTrue(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertFalse(rta.rollbackOn(new IOException()));
	}

	@Test
	public void testRollbackNever() {
		List<RollbackRuleAttribute> list = new LinkedList<RollbackRuleAttribute>();
		list.add(new NoRollbackRuleAttribute("Throwable"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertFalse(rta.rollbackOn(new Throwable()));
		assertFalse(rta.rollbackOn(new RuntimeException()));
		assertFalse(rta.rollbackOn(new MyRuntimeException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		assertFalse(rta.rollbackOn(new IOException()));
	}

	@Test
	public void testToStringMatchesEditor() {
		List<RollbackRuleAttribute> list = new LinkedList<RollbackRuleAttribute>();
		list.add(new NoRollbackRuleAttribute("Throwable"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		tae.setAsText(rta.toString());
		rta = (RuleBasedTransactionAttribute) tae.getValue();

		assertFalse(rta.rollbackOn(new Throwable()));
		assertFalse(rta.rollbackOn(new RuntimeException()));
		assertFalse(rta.rollbackOn(new MyRuntimeException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		assertFalse(rta.rollbackOn(new IOException()));
	}

	/**
	 * See <a href="http://forum.springframework.org/showthread.php?t=41350">this forum post</a>.
	 */
	@Test
	public void testConflictingRulesToDetermineExactContract() {
		List<RollbackRuleAttribute> list = new LinkedList<RollbackRuleAttribute>();
		list.add(new NoRollbackRuleAttribute(MyBusinessWarningException.class));
		list.add(new RollbackRuleAttribute(MyBusinessException.class));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertTrue(rta.rollbackOn(new MyBusinessException()));
		assertFalse(rta.rollbackOn(new MyBusinessWarningException()));
	}


	@SuppressWarnings("serial")
	private static class MyBusinessException extends Exception {}


	@SuppressWarnings("serial")
	private static final class MyBusinessWarningException extends MyBusinessException {}

}
