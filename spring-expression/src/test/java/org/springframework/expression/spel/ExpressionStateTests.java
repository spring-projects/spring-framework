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

package org.springframework.expression.spel;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for the expression state object - some features are not yet exploited in the language (eg nested scopes)
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 */
public class ExpressionStateTests extends AbstractExpressionTests {

	@Test
	public void testConstruction() {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		ExpressionState state = new ExpressionState(context);
		assertThat(state.getEvaluationContext()).isEqualTo(context);
	}

	// Local variables are in variable scopes which come and go during evaluation.  Normal variables are
	// accessible through the evaluation context

	@Test
	public void testLocalVariables() {
		ExpressionState state = getState();

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
	public void testVariables() {
		ExpressionState state = getState();
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
	public void testNoVariableInterference() {
		ExpressionState state = getState();
		TypedValue typedValue = state.lookupVariable("foo");
		assertThat(typedValue).isEqualTo(TypedValue.NULL);

		state.setLocalVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		assertThat(typedValue).isEqualTo(TypedValue.NULL);

		state.setVariable("goo", "hello");
		assertThat(state.lookupLocalVariable("goo")).isNull();
	}

	@Test
	public void testLocalVariableNestedScopes() {
		ExpressionState state = getState();
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
	public void testRootContextObject() {
		ExpressionState state = getState();
		assertThat(state.getRootContextObject().getValue().getClass()).isEqualTo(Inventor.class);

		// although the root object is being set on the evaluation context, the value in the 'state' remains what it was when constructed
		((StandardEvaluationContext) state.getEvaluationContext()).setRootObject(null);
		assertThat(state.getRootContextObject().getValue().getClass()).isEqualTo(Inventor.class);
		// assertEquals(null, state.getRootContextObject().getValue());

		state = new ExpressionState(new StandardEvaluationContext());
		assertThat(state.getRootContextObject()).isEqualTo(TypedValue.NULL);


		((StandardEvaluationContext) state.getEvaluationContext()).setRootObject(null);
		assertThat(state.getRootContextObject().getValue()).isNull();
	}

	@Test
	public void testActiveContextObject() {
		ExpressionState state = getState();
		assertThat(state.getActiveContextObject().getValue()).isEqualTo(state.getRootContextObject().getValue());

		assertThatIllegalStateException().isThrownBy(
				state::popActiveContextObject);

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
	public void testPopulatedNestedScopes() {
		ExpressionState state = getState();
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
	public void testRootObjectConstructor() {
		EvaluationContext ctx = getContext();
		// TypedValue root = ctx.getRootObject();
		// supplied should override root on context
		ExpressionState state = new ExpressionState(ctx,new TypedValue("i am a string"));
		TypedValue stateRoot = state.getRootContextObject();
		assertThat(stateRoot.getTypeDescriptor().getType()).isEqualTo(String.class);
		assertThat(stateRoot.getValue()).isEqualTo("i am a string");
	}

	@Test
	public void testPopulatedNestedScopesMap() {
		ExpressionState state = getState();
		assertThat(state.lookupLocalVariable("foo")).isNull();
		assertThat(state.lookupLocalVariable("goo")).isNull();

		Map<String,Object> m = new HashMap<>();
		m.put("foo", 34);
		m.put("goo", "abc");

		state.enterScope(m);
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
	public void testOperators() {
		ExpressionState state = getState();
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
				state.operate(Operation.ADD,1,2))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES));

		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
				state.operate(Operation.ADD,null,null))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES));
	}

	@Test
	public void testComparator() {
		ExpressionState state = getState();
		assertThat(state.getTypeComparator()).isEqualTo(state.getEvaluationContext().getTypeComparator());
	}

	@Test
	public void testTypeLocator() throws EvaluationException {
		ExpressionState state = getState();
		assertThat(state.getEvaluationContext().getTypeLocator()).isNotNull();
		assertThat(state.findType("java.lang.Integer")).isEqualTo(Integer.class);
		assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
				state.findType("someMadeUpName"))
			.satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.TYPE_NOT_FOUND));

	}

	@Test
	public void testTypeConversion() throws EvaluationException {
		ExpressionState state = getState();
		String s = (String) state.convertValue(34, TypeDescriptor.valueOf(String.class));
		assertThat(s).isEqualTo("34");

		s = (String)state.convertValue(new TypedValue(34), TypeDescriptor.valueOf(String.class));
		assertThat(s).isEqualTo("34");
	}

	@Test
	public void testPropertyAccessors() {
		ExpressionState state = getState();
		assertThat(state.getPropertyAccessors()).isEqualTo(state.getEvaluationContext().getPropertyAccessors());
	}

	/**
	 * @return a new ExpressionState
	 */
	private ExpressionState getState() {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		ExpressionState state = new ExpressionState(context);
		return state;
	}

	private EvaluationContext getContext() {
		return TestScenarioCreator.getTestEvaluationContext();
	}

}
