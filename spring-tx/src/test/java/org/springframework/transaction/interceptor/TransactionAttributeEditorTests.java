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

import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests to check conversion from String to TransactionAttribute using
 * a {@link TransactionAttributeEditor}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 26.04.2003
 */
class TransactionAttributeEditorTests {

	private final TransactionAttributeEditor pe = new TransactionAttributeEditor();


	@Test
	void nullText() {
		pe.setAsText(null);
		assertThat(pe.getValue()).isNull();
	}

	@Test
	void emptyString() {
		pe.setAsText("");
		assertThat(pe.getValue()).isNull();
	}

	@Test
	void validPropagationCodeOnly() {
		pe.setAsText("PROPAGATION_REQUIRED");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta).isNotNull();
		assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
		assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
		assertThat(ta.isReadOnly()).isFalse();
	}

	@Test
	void invalidPropagationCodeOnly() {
		// should have failed with bogus propagation code
		assertThatIllegalArgumentException().isThrownBy(() -> pe.setAsText("XXPROPAGATION_REQUIRED"));
	}

	@Test
	void validPropagationCodeAndIsolationCode() {
		pe.setAsText("PROPAGATION_REQUIRED, ISOLATION_READ_UNCOMMITTED");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta).isNotNull();
		assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
		assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
	}

	@Test
	void validPropagationAndIsolationCodesAndInvalidRollbackRule() {
		// should fail with bogus rollback rule
		assertThatIllegalArgumentException()
			.isThrownBy(() -> pe.setAsText("PROPAGATION_REQUIRED,ISOLATION_READ_UNCOMMITTED,XXX"));
	}

	@Test
	void validPropagationCodeAndIsolationCodeAndRollbackRules1() {
		pe.setAsText("PROPAGATION_MANDATORY,ISOLATION_REPEATABLE_READ,timeout_10,-IOException,+MyRuntimeException");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta).isNotNull();
		assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_MANDATORY);
		assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThat(ta.getTimeout()).isEqualTo(10);
		assertThat(ta.isReadOnly()).isFalse();
		assertThat(ta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(ta.rollbackOn(new Exception())).isFalse();
		// Check for our bizarre customized rollback rules
		assertThat(ta.rollbackOn(new IOException())).isTrue();
		assertThat(ta.rollbackOn(new MyRuntimeException())).isFalse();
	}

	@Test
	void validPropagationCodeAndIsolationCodeAndRollbackRules2() {
		pe.setAsText("+IOException,readOnly,ISOLATION_READ_COMMITTED,-MyRuntimeException,PROPAGATION_SUPPORTS");
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(ta).isNotNull();
		assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_READ_COMMITTED);
		assertThat(ta.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
		assertThat(ta.isReadOnly()).isTrue();
		assertThat(ta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(ta.rollbackOn(new Exception())).isFalse();
		// Check for our bizarre customized rollback rules
		assertThat(ta.rollbackOn(new IOException())).isFalse();
		assertThat(ta.rollbackOn(new MyRuntimeException())).isTrue();
	}

	@Test
	void defaultTransactionAttributeToString() {
		DefaultTransactionAttribute source = new DefaultTransactionAttribute();
		source.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		source.setTimeout(10);
		source.setReadOnly(true);

		pe.setAsText(source.toString());
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(source).isEqualTo(ta);
		assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThat(ta.getTimeout()).isEqualTo(10);
		assertThat(ta.isReadOnly()).isTrue();
		assertThat(ta.rollbackOn(new RuntimeException())).isTrue();
		assertThat(ta.rollbackOn(new Exception())).isFalse();

		source.setTimeout(9);
		assertThat(source).isNotSameAs(ta);
		source.setTimeout(10);
		assertThat(source).isEqualTo(ta);
	}

	@Test
	void ruleBasedTransactionAttributeToString() {
		RuleBasedTransactionAttribute source = new RuleBasedTransactionAttribute();
		source.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
		source.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		source.setTimeout(10);
		source.setReadOnly(true);
		source.getRollbackRules().add(new RollbackRuleAttribute("IllegalArgumentException"));
		source.getRollbackRules().add(new NoRollbackRuleAttribute("IllegalStateException"));

		pe.setAsText(source.toString());
		TransactionAttribute ta = (TransactionAttribute) pe.getValue();
		assertThat(source).isEqualTo(ta);
		assertThat(ta.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_SUPPORTS);
		assertThat(ta.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
		assertThat(ta.getTimeout()).isEqualTo(10);
		assertThat(ta.isReadOnly()).isTrue();
		assertThat(ta.rollbackOn(new IllegalArgumentException())).isTrue();
		assertThat(ta.rollbackOn(new IllegalStateException())).isFalse();

		source.getRollbackRules().clear();
		assertThat(source).isNotSameAs(ta);
		source.getRollbackRules().add(new RollbackRuleAttribute("IllegalArgumentException"));
		source.getRollbackRules().add(new NoRollbackRuleAttribute("IllegalStateException"));
		assertThat(source).isEqualTo(ta);
	}

}
