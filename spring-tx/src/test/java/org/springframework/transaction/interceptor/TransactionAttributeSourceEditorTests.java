/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TransactionAttributeSourceEditor}.
 *
 * <p>Format is: {@code <fully-qualified class name>.<method-name>=tx attribute representation}
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 26.04.2003
 */
class TransactionAttributeSourceEditorTests {

	private final TransactionAttributeSourceEditor editor = new TransactionAttributeSourceEditor();


	@Test
	void nullValue() throws Exception {
		editor.setAsText(null);
		TransactionAttributeSource tas = (TransactionAttributeSource) editor.getValue();

		Method m = Object.class.getMethod("hashCode");
		assertThat(tas.getTransactionAttribute(m, null)).isNull();
	}

	@Test
	void invalidFormat() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("foo=bar"));
	}

	@Test
	void matchesSpecific() throws Exception {
		editor.setAsText("""
				java.lang.Object.hashCode=PROPAGATION_REQUIRED
				java.lang.Object.equals=PROPAGATION_MANDATORY
				java.lang.Object.*it=PROPAGATION_SUPPORTS
				java.lang.Object.notify=PROPAGATION_SUPPORTS
				java.lang.Object.not*=PROPAGATION_REQUIRED""");
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
	void matchesAll() throws Exception {
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
			assertThat(ta).isNotNull();
			assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
			assertThat(ta.getPropagationBehavior()).isEqualTo(propagationBehavior);
		}
		else {
			assertThat(ta).isNull();
		}
	}

}
