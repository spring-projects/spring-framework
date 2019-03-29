/*
 * Copyright 2002-2018 the original author or authors.
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

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.transaction.TransactionDefinition;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TransactionAttributeSourceEditor}.
 *
 * <p>Format is: {@code FQN.Method=tx attribute representation}
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 26.04.2003
 */
public class TransactionAttributeSourceEditorTests {

	private final TransactionAttributeSourceEditor editor = new TransactionAttributeSourceEditor();


	@Test
	public void nullValue() throws Exception {
		editor.setAsText(null);
		TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

		Method m = Object.class.getMethod("hashCode");
		assertNull(tas.getTransactionAttribute(m, null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidFormat() throws Exception {
		editor.setAsText("foo=bar");
	}

	@Test
	public void matchesSpecific() throws Exception {
		editor.setAsText(
			"java.lang.Object.hashCode=PROPAGATION_REQUIRED\n" +
			"java.lang.Object.equals=PROPAGATION_MANDATORY\n" +
			"java.lang.Object.*it=PROPAGATION_SUPPORTS\n" +
			"java.lang.Object.notify=PROPAGATION_SUPPORTS\n" +
			"java.lang.Object.not*=PROPAGATION_REQUIRED");
		TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

		checkTransactionProperties(tas, Object.class.getMethod("hashCode"),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("equals", Object.class),
				TransactionDefinition.PROPAGATION_MANDATORY);
		checkTransactionProperties(tas, Object.class.getMethod("wait"),
				TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("wait", long.class),
				TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("wait", long.class, int.class),
				TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("notify"),
				TransactionDefinition.PROPAGATION_SUPPORTS);
		checkTransactionProperties(tas, Object.class.getMethod("notifyAll"),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("toString"), -1);
	}

	@Test
	public void matchesAll() throws Exception {
		editor.setAsText("java.lang.Object.*=PROPAGATION_REQUIRED");
		TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

		checkTransactionProperties(tas, Object.class.getMethod("hashCode"),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("equals", Object.class),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("wait"),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("wait", long.class),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("wait", long.class, int.class),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("notify"),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("notifyAll"),
				TransactionDefinition.PROPAGATION_REQUIRED);
		checkTransactionProperties(tas, Object.class.getMethod("toString"),
				TransactionDefinition.PROPAGATION_REQUIRED);
	}

	private void checkTransactionProperties(TransactionAttributeSource tas, Method method, int propagationBehavior) {
		TransactionAttribute ta = tas.getTransactionAttribute(method, null);
		if (propagationBehavior >= 0) {
			assertNotNull(ta);
			assertEquals(TransactionDefinition.ISOLATION_DEFAULT, ta.getIsolationLevel());
			assertEquals(propagationBehavior, ta.getPropagationBehavior());
		}
		else {
			assertNull(ta);
		}
	}

}
