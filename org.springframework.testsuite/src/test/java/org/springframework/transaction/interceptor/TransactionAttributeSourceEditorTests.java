/*
 * Copyright 2002-2005 the original author or authors.
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

import java.lang.reflect.Method;

import junit.framework.TestCase;

import org.springframework.transaction.TransactionDefinition;

/**
 * Format is 
 * <code>FQN.Method=tx attribute representation</code>
 *
 * @author Rod Johnson
 * @since 26.04.2003
 */
public class TransactionAttributeSourceEditorTests extends TestCase {

	public void testNull() throws Exception {
		TransactionAttributeSourceEditor pe = new TransactionAttributeSourceEditor();
		pe.setAsText(null);
		TransactionAttributeSource tas = (TransactionAttributeSource) pe.getValue();

		Method m = Object.class.getMethod("hashCode", (Class[]) null);
		assertTrue(tas.getTransactionAttribute(m, null) == null);
	}

	public void testInvalid() throws Exception {
		TransactionAttributeSourceEditor pe = new TransactionAttributeSourceEditor();
		try {
			pe.setAsText("foo=bar");
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}
	
	public void testMatchesSpecific() throws Exception {
		TransactionAttributeSourceEditor pe = new TransactionAttributeSourceEditor();
		// TODO need FQN?
		pe.setAsText(
		    "java.lang.Object.hashCode=PROPAGATION_REQUIRED\n" +
		    "java.lang.Object.equals=PROPAGATION_MANDATORY\n" +
		    "java.lang.Object.*it=PROPAGATION_SUPPORTS\n" +
		    "java.lang.Object.notify=PROPAGATION_SUPPORTS\n" +
		    "java.lang.Object.not*=PROPAGATION_REQUIRED");
		TransactionAttributeSource tas = (TransactionAttributeSource) pe.getValue();

		checkTransactionProperties(tas, Object.class.getMethod("hashCode", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("equals", new Class[] { Object.class }),
		    TransactionDefinition.PROPAGATION_MANDATORY);
		checkTransactionProperties(tas, Object.class.getMethod("wait", (Class[]) null),
		    TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("wait", new Class[] { long.class }),
		    TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("wait", new Class[] { long.class, int.class }),
		    TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("notify", (Class[]) null),
		    TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("notifyAll", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("toString", (Class[]) null), -1);
	}

	public void testMatchesAll() throws Exception {
		TransactionAttributeSourceEditor pe = new TransactionAttributeSourceEditor();
		pe.setAsText("java.lang.Object.*=PROPAGATION_REQUIRED");
		TransactionAttributeSource tas = (TransactionAttributeSource) pe.getValue();

		checkTransactionProperties(tas, Object.class.getMethod("hashCode", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("equals", new Class[] { Object.class }),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("wait", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("wait", new Class[] { long.class }),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("wait", new Class[] { long.class, int.class }),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("notify", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("notifyAll", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("toString", (Class[]) null),
		    TransactionDefinition.PROPAGATION_REQUIRED);
	}

	private void checkTransactionProperties(TransactionAttributeSource tas, Method method, int propagationBehavior) {
		TransactionAttribute ta = tas.getTransactionAttribute(method, null);
		if (propagationBehavior >= 0) {
			assertTrue(ta != null);
			assertTrue(ta.getIsolationLevel() == TransactionDefinition.ISOLATION_DEFAULT);
			assertTrue(ta.getPropagationBehavior() == propagationBehavior);
		}
		else {
			assertTrue(ta == null);
		}
	}

}
