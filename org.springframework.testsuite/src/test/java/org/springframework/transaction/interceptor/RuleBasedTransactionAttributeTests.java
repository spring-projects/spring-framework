/*
 * Copyright 2002-2007 the original author or authors.
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

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import junit.framework.TestCase;

import org.springframework.mail.MailSendException;
import org.springframework.transaction.TransactionDefinition;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 09.04.2003
 */
public class RuleBasedTransactionAttributeTests extends TestCase {

	public void testDefaultRule() {
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute();
		assertTrue(rta.rollbackOn(new RuntimeException()));
		assertTrue(rta.rollbackOn(new MailSendException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		assertFalse(rta.rollbackOn(new ServletException()));
	}
	
	/**
	 * Test one checked exception that should roll back.
	 */
	public void testRuleForRollbackOnChecked() {
		List list = new LinkedList();
		list.add(new RollbackRuleAttribute("javax.servlet.ServletException"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);
		
		assertTrue(rta.rollbackOn(new RuntimeException()));
		assertTrue(rta.rollbackOn(new MailSendException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertTrue(rta.rollbackOn(new ServletException()));
	}
	
	public void testRuleForCommitOnUnchecked() {
		List list = new LinkedList();
		list.add(new NoRollbackRuleAttribute("org.springframework.mail.MailSendException"));
		list.add(new RollbackRuleAttribute("javax.servlet.ServletException"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);
		
		assertTrue(rta.rollbackOn(new RuntimeException()));
		// Check default behaviour is overridden
		assertFalse(rta.rollbackOn(new MailSendException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertTrue(rta.rollbackOn(new ServletException()));
	}
	
	public void testRuleForSelectiveRollbackOnCheckedWithString() {
		List l = new LinkedList();
		l.add(new RollbackRuleAttribute("java.rmi.RemoteException"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, l);
		doTestRuleForSelectiveRollbackOnChecked(rta);
	}
	
	public void testRuleForSelectiveRollbackOnCheckedWithClass() {
		List l = Collections.singletonList(new RollbackRuleAttribute(RemoteException.class));
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
	 * Check that a rule can cause commit on a ServletException
	 * when Exception prompts a rollback.
	 */
	public void testRuleForCommitOnSubclassOfChecked() {
		List list = new LinkedList();
		// Note that it's important to ensure that we have this as
		// a FQN: otherwise it will match everything!
		list.add(new RollbackRuleAttribute("java.lang.Exception"));
		list.add(new NoRollbackRuleAttribute("ServletException"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertTrue(rta.rollbackOn(new RuntimeException()));
		assertTrue(rta.rollbackOn(new Exception()));
		// Check that default behaviour is overridden
		assertFalse(rta.rollbackOn(new ServletException()));
	}

	public void testRollbackNever() {
		List list = new LinkedList();
		list.add(new NoRollbackRuleAttribute("Throwable"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);
	
		assertFalse(rta.rollbackOn(new Throwable()));
		assertFalse(rta.rollbackOn(new RuntimeException()));
		assertFalse(rta.rollbackOn(new MailSendException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		assertFalse(rta.rollbackOn(new ServletException()));
	}

	public void testToStringMatchesEditor() {
		List list = new LinkedList();
		list.add(new NoRollbackRuleAttribute("Throwable"));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		TransactionAttributeEditor tae = new TransactionAttributeEditor();
		tae.setAsText(rta.toString());
		rta = (RuleBasedTransactionAttribute) tae.getValue();

		assertFalse(rta.rollbackOn(new Throwable()));
		assertFalse(rta.rollbackOn(new RuntimeException()));
		assertFalse(rta.rollbackOn(new MailSendException("")));
		assertFalse(rta.rollbackOn(new Exception()));
		assertFalse(rta.rollbackOn(new ServletException()));
	}

	/**
	 * See <a href="http://forum.springframework.org/showthread.php?t=41350">this forum post</a>.
	 */
	public void testConflictingRulesToDetermineExactContract() {
		List list = new LinkedList();
		list.add(new NoRollbackRuleAttribute(MyBusinessWarningException.class));
		list.add(new RollbackRuleAttribute(MyBusinessException.class));
		RuleBasedTransactionAttribute rta = new RuleBasedTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRED, list);

		assertTrue(rta.rollbackOn(new MyBusinessException()));
		assertFalse(rta.rollbackOn(new MyBusinessWarningException()));
	}


	public static class MyBusinessException extends Exception {}


	public static final class MyBusinessWarningException extends MyBusinessException {}

}
