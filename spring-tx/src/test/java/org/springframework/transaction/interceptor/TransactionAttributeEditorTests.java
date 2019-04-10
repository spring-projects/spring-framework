/*
 * Copyright 2002-2012 the original author or authors.
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

import org.junit.Test;

import org.springframework.transaction.TransactionDefinition;

import static org.junit.Assert.*;

/**
 * Tests to check conversion from String to TransactionAttribute.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 26.04.2003
 */
public class TransactionAttributeEditorTests {

	@Test
	public void testNull() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText(null);
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertTrue(ta == null);
	}

	@Test
	public void testEmptyString() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertTrue(ta == null);
	}

	@Test
	public void testValidPropagationCodeOnly() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("PROPAGATION_REQUIRED");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertTrue(ta != null);
		assertTrue(ta.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED);
		assertTrue(ta.getIsolationLevel() == TransactionDefinition.ISOLATION_DEFAULT);
		assertTrue(!ta.isReadOnly());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidPropagationCodeOnly() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		// should have failed with bogus propagation code
		pe.setAsText("XXPROPAGATION_REQUIRED");
	}

	@Test
	public void testValidPropagationCodeAndIsolationCode() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("PROPAGATION_REQUIRED, ISOLATION_READ_UNCOMMITTED");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertTrue(ta != null);
		assertTrue(ta.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED);
		assertTrue(ta.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidPropagationAndIsolationCodesAndInvalidRollbackRule() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		// should fail with bogus rollback rule
		pe.setAsText("PROPAGATION_REQUIRED,ISOLATION_READ_UNCOMMITTED,XXX");
	}

	@Test
	public void testValidPropagationCodeAndIsolationCodeAndRollbackRules1() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("PROPAGATION_MANDATORY,ISOLATION_REPEATABLE_READ,timeout_10,-IOException,+MyRuntimeException");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertNotNull(ta);
		assertEquals(TransactionDefinition.PROPAGATION_MANDATORY, ta.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ, ta.getIsolationLevel());
		assertEquals(10, ta.getTimeout());
		assertFalse(ta.isReadOnly());
		assertTrue(ta.rollbackOn(new RuntimeException()));
		assertFalse(ta.rollbackOn(new Exception()));
		// Check for our bizarre customized rollback rules
		assertTrue(ta.rollbackOn(new IOException()));
		assertTrue(!ta.rollbackOn(new MyRuntimeException("")));
	}

	@Test
	public void testValidPropagationCodeAndIsolationCodeAndRollbackRules2() {
		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText("+IOException,readOnly,ISOLATION_READ_COMMITTED,-MyRuntimeException,PROPAGATION_SUPPORTS");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertNotNull(ta);
		assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, ta.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_READ_COMMITTED, ta.getIsolationLevel());
		assertEquals(TransactionDefinition.TIMEOUT_DEFAULT, ta.getTimeout());
		assertTrue(ta.isReadOnly());
		assertTrue(ta.rollbackOn(new RuntimeException()));
		assertFalse(ta.rollbackOn(new Exception()));
		// Check for our bizarre customized rollback rules
		assertFalse(ta.rollbackOn(new IOException()));
		assertTrue(ta.rollbackOn(new MyRuntimeException("")));
	}

	@Test
	public void testDefaultTransactionAttributeToString() {
		DefaultTransactionAttribute source = new DefaultTransactionAttribute();
		source.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		source.setTimeout(10);
		source.setReadOnly(true);

		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText(source.toString());
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertEquals(ta, source);
		assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, ta.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ, ta.getIsolationLevel());
		assertEquals(10, ta.getTimeout());
		assertTrue(ta.isReadOnly());
		assertTrue(ta.rollbackOn(new RuntimeException()));
		assertFalse(ta.rollbackOn(new Exception()));

		source.setTimeout(9);
		assertNotSame(ta, source);
		source.setTimeout(10);
		assertEquals(ta, source);
	}

	@Test
	public void testRuleBasedTransactionAttributeToString() {
		RuleBasedTransactionAttribute source = new RuleBasedTransactionAttribute();
		source.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		source.setTimeout(10);
		source.setReadOnly(true);
		source.getRollbackRules().add(new RollbackRuleAttribute("IllegalArgumentException"));
		source.getRollbackRules().add(new NoRollbackRuleAttribute("IllegalStateException"));

		TransactionAttributeEditor pe = new TransactionAttributeEditor();
		pe.setAsText(source.toString());
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertEquals(ta, source);
		assertEquals(TransactionDefinition.PROPAGATION_SUPPORTS, ta.getPropagationBehavior());
		assertEquals(TransactionDefinition.ISOLATION_REPEATABLE_READ, ta.getIsolationLevel());
		assertEquals(10, ta.getTimeout());
		assertTrue(ta.isReadOnly());
		assertTrue(ta.rollbackOn(new IllegalArgumentException()));
		assertFalse(ta.rollbackOn(new IllegalStateException()));

		source.getRollbackRules().clear();
		assertNotSame(ta, source);
		source.getRollbackRules().add(new RollbackRuleAttribute("IllegalArgumentException"));
		source.getRollbackRules().add(new NoRollbackRuleAttribute("IllegalStateException"));
		assertEquals(ta, source);
	}

}
