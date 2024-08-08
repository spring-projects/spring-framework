/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ExpressionState}.
 *
 * <p>Some features are not yet exploited in the language, such as nested scopes
 * or local variables scoped to the currently evaluated expression.
 *
 * <p>Local variables are in variable scopes which come and go during evaluation.
 * Normal/global variables are accessible through the {@link EvaluationContext}.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 */
class ExpressionStateTests extends AbstractExpressionTests {

	private ExpressionState state = new ExpressionState(TestScenarioCreator.getTestEvaluationContext());


	@Test
	void construction() {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		ExpressionState state = new ExpressionState(context);
		assertThat(state.getEvaluationContext()).isEqualTo(context);
	}

	@Test
	@SuppressWarnings("removal")
	void localVariables() {
		Object value = state.lookupLocalVariable("foo");
		assertThat(value).isNull();

		state.setLocalVariable("foo",34);
		value = state.lookupLocalVariable("foo");
		assertThat(value).isEqualTo(34);

		state.setLocalVariable("foo", null);
		value = state.lookupLocalVariable("foo");
		assertThat(value).isNull();
	}

	@Test
	void globalVariables() {
		TypedValue typedValue = state.lookupVariable("foo");
		assertThat(typedValue).isEqualTo(TypedValue.NULL);

		state.setVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		assertThat(typedValue.getValue()).isEqualTo(34);
		assertThat(typedValue.getTypeDescriptor().getType()).isEqualTo(Integer.class);

		state.setVariable("foo","abc");
		typedValue = state.lookupVariable("foo");
		assertThat(typedValue.getValue()).isEqualTo("abc");
		assertThat(typedValue.getTypeDescriptor().getType()).isEqualTo(String.class);
	}

	@Test
	@SuppressWarnings("removal")
	void noVariableInterference() {
		TypedValue typedValue = state.lookupVariable("foo");
		assertThat(typedValue).isEqualTo(TypedValue.NULL);

		state.setLocalVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		assertThat(typedValue).isEqualTo(TypedValue.NULL);

		state.setVariable("goo", "hello");
		assertThat(state.lookupLocalVariable("goo")).isNull();
	}

	@Test
	@SuppressWarnings("removal")
	void localVariableNestedScopes() {
		assertThat(state.lookupLocalVariable("foo")).isNull();

		state.setLocalVariable("foo",12);
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(12);

		state.enterScope(null);
		// found in upper scope
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(12);

		state.setLocalVariable("foo","abc");
		// found in nested scope
		assertThat(state.lookupLocalVariable("foo")).isEqualTo("abc");

		state.exitScope();
		// found in nested scope
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(12);
	}

	@Test
	void rootContextObject() {
		assertThat(state.getRootContextObject().getValue().getClass()).isEqualTo(Inventor.class);

		// Although the root object is being set on the evaluation context,
		// the value in the 'state' remains what it was when constructed.
		((StandardEvaluationContext) state.getEvaluationContext()).setRootObject(null);
		assertThat(state.getRootContextObject().getValue()).isInstanceOf(Inventor.class);

		state = new ExpressionState(new StandardEvaluationContext());
		assertThat(state.getRootContextObject()).isEqualTo(TypedValue.NULL);

		((StandardEvaluationContext) state.getEvaluationContext()).setRootObject(null);
		assertThat(state.getRootContextObject().getValue()).isNull();
	}

	@Test
	void activeContextObject() {
		assertThat(state.getActiveContextObject().getValue()).isEqualTo(state.getRootContextObject().getValue());

		assertThatIllegalStateException().isThrownBy(state::popActiveContextObject);

		state.pushActiveContextObject(new TypedValue(34));
		assertThat(state.getActiveContextObject().getValue()).isEqualTo(34);

		state.pushActiveContextObject(new TypedValue("hello"));
		assertThat(state.getActiveContextObject().getValue()).isEqualTo("hello");

		state.popActiveContextObject();
		assertThat(state.getActiveContextObject().getValue()).isEqualTo(34);

		state.popActiveContextObject();
		assertThat(state.getActiveContextObject().getValue()).isEqualTo(state.getRootContextObject().getValue());

		state = new ExpressionState(new StandardEvaluationContext());
		assertThat(state.getActiveContextObject()).isEqualTo(TypedValue.NULL);
	}

	@Test
	@SuppressWarnings("removal")
	void populatedNestedScopes() {
		assertThat(state.lookupLocalVariable("foo")).isNull();

		state.enterScope("foo",34);
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(34);

		state.enterScope(null);
		state.setLocalVariable("foo", 12);
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(12);

		state.exitScope();
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(34);

		state.exitScope();
		assertThat(state.lookupLocalVariable("goo")).isNull();
	}

	@Test
	void rootObjectConstructor() {
		EvaluationContext ctx = TestScenarioCreator.getTestEvaluationContext();
		// TypedValue root = ctx.getRootObject();
		// supplied should override root on context
		ExpressionState state = new ExpressionState(ctx, new TypedValue("i am a string"));
		TypedValue stateRoot = state.getRootContextObject();
		assertThat(stateRoot.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(stateRoot.getValue()).isEqualTo("i am a string");
	}

	@Test
	@SuppressWarnings("removal")
	void populatedNestedScopesMap() {
		assertThat(state.lookupLocalVariable("foo")).isNull();
		assertThat(state.lookupLocalVariable("goo")).isNull();

		state.enterScope(Map.of("foo", 34, "goo", "abc"));
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(34);
		assertThat(state.lookupLocalVariable("goo")).isEqualTo("abc");

		state.enterScope(null);
		state.setLocalVariable("foo",12);
		assertThat(state.lookupLocalVariable("foo")).isEqualTo(12);
		assertThat(state.lookupLocalVariable("goo")).isEqualTo("abc");

		state.exitScope();
		state.exitScope();
		assertThat(state.lookupLocalVariable("foo")).isNull();
		assertThat(state.lookupLocalVariable("goo")).isNull();
	}

	@Test
	void operators() {
		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> state.operate(Operation.ADD,1,2))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES));

		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> state.operate(Operation.ADD,null,null))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES));
	}

	@Test
	void comparator() {
		assertThat(state.getTypeComparator()).isEqualTo(state.getEvaluationContext().getTypeComparator());
	}

	@Test
	void typeLocator() {
		assertThat(state.getEvaluationContext().getTypeLocator()).isNotNull();
		assertThat(state.findType("java.lang.Integer")).isEqualTo(Integer.class);
		assertThatExceptionOfType(SpelEvaluationException.class)
			.isThrownBy(() -> state.findType("someMadeUpName"))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.TYPE_NOT_FOUND));
	}

	@Test
	void typeConversion() {
		String s = (String) state.convertValue(34, TypeDescriptor.valueOf(String.class));
		assertThat(s).isEqualTo("34");

		s = (String) state.convertValue(new TypedValue(34), TypeDescriptor.valueOf(String.class));
		assertThat(s).isEqualTo("34");
	}

	@Test
	void propertyAccessors() {
		assertThat(state.getPropertyAccessors()).isEqualTo(state.getEvaluationContext().getPropertyAccessors());
	}

}
