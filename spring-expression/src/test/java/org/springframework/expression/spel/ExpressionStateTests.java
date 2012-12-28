/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression.spel;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.testresources.Inventor;

/**
 * Tests for the expression state object - some features are not yet exploited in the language (eg nested scopes)
 *
 * @author Andy Clement
 */
public class ExpressionStateTests extends ExpressionTestCase {

	@Test
	public void testConstruction() {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		ExpressionState state = new ExpressionState(context);
		Assert.assertEquals(context,state.getEvaluationContext());
	}

	// Local variables are in variable scopes which come and go during evaluation.  Normal variables are
	// accessible through the evaluation context

	@Test
	public void testLocalVariables() {
		ExpressionState state = getState();

		Object value = state.lookupLocalVariable("foo");
		Assert.assertNull(value);

		state.setLocalVariable("foo",34);
		value = state.lookupLocalVariable("foo");
		Assert.assertEquals(34,value);

		state.setLocalVariable("foo",null);
		value = state.lookupLocalVariable("foo");
		Assert.assertEquals(null,value);
	}

	@Test
	public void testVariables() {
		ExpressionState state = getState();
		TypedValue typedValue = state.lookupVariable("foo");
		Assert.assertEquals(TypedValue.NULL,typedValue);

		state.setVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		Assert.assertEquals(34,typedValue.getValue());
		Assert.assertEquals(Integer.class,typedValue.getTypeDescriptor().getType());

		state.setVariable("foo","abc");
		typedValue = state.lookupVariable("foo");
		Assert.assertEquals("abc",typedValue.getValue());
		Assert.assertEquals(String.class,typedValue.getTypeDescriptor().getType());
	}

	@Test
	public void testNoVariableInteference() {
		ExpressionState state = getState();
		TypedValue typedValue = state.lookupVariable("foo");
		Assert.assertEquals(TypedValue.NULL,typedValue);

		state.setLocalVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		Assert.assertEquals(TypedValue.NULL,typedValue);

		state.setVariable("goo","hello");
		Assert.assertNull(state.lookupLocalVariable("goo"));
	}

	@Test
	public void testLocalVariableNestedScopes() {
		ExpressionState state = getState();
		Assert.assertEquals(null,state.lookupLocalVariable("foo"));

		state.setLocalVariable("foo",12);
		Assert.assertEquals(12,state.lookupLocalVariable("foo"));

		state.enterScope(null);
		Assert.assertEquals(12,state.lookupLocalVariable("foo")); // found in upper scope

		state.setLocalVariable("foo","abc");
		Assert.assertEquals("abc",state.lookupLocalVariable("foo")); // found in nested scope

		state.exitScope();
		Assert.assertEquals(12,state.lookupLocalVariable("foo")); // found in nested scope
	}

	@Test
	public void testRootContextObject() {
		ExpressionState state = getState();
		Assert.assertEquals(Inventor.class,state.getRootContextObject().getValue().getClass());

		// although the root object is being set on the evaluation context, the value in the 'state' remains what it was when constructed
		((StandardEvaluationContext) state.getEvaluationContext()).setRootObject(null);
		Assert.assertEquals(Inventor.class,state.getRootContextObject().getValue().getClass());
		// Assert.assertEquals(null, state.getRootContextObject().getValue());

		state = new ExpressionState(new StandardEvaluationContext());
		Assert.assertEquals(TypedValue.NULL,state.getRootContextObject());


		((StandardEvaluationContext)state.getEvaluationContext()).setRootObject(null);
		Assert.assertEquals(null,state.getRootContextObject().getValue());
	}

	@Test
	public void testActiveContextObject() {
		ExpressionState state = getState();
		Assert.assertEquals(state.getRootContextObject().getValue(),state.getActiveContextObject().getValue());

		try {
			state.popActiveContextObject();
			Assert.fail("stack should be empty...");
		} catch (EmptyStackException ese) {
			// success
		}

		state.pushActiveContextObject(new TypedValue(34));
		Assert.assertEquals(34,state.getActiveContextObject().getValue());

		state.pushActiveContextObject(new TypedValue("hello"));
		Assert.assertEquals("hello",state.getActiveContextObject().getValue());

		state.popActiveContextObject();
		Assert.assertEquals(34,state.getActiveContextObject().getValue());

		state.popActiveContextObject();
		Assert.assertEquals(state.getRootContextObject().getValue(),state.getActiveContextObject().getValue());

		state = new ExpressionState(new StandardEvaluationContext());
		Assert.assertEquals(TypedValue.NULL,state.getActiveContextObject());
	}

	@Test
	public void testPopulatedNestedScopes() {
		ExpressionState state = getState();
		Assert.assertNull(state.lookupLocalVariable("foo"));

		state.enterScope("foo",34);
		Assert.assertEquals(34,state.lookupLocalVariable("foo"));

		state.enterScope(null);
		state.setLocalVariable("foo",12);
		Assert.assertEquals(12,state.lookupLocalVariable("foo"));

		state.exitScope();
		Assert.assertEquals(34,state.lookupLocalVariable("foo"));

		state.exitScope();
		Assert.assertNull(state.lookupLocalVariable("goo"));
	}

	@Test
	public void testRootObjectConstructor() {
		EvaluationContext ctx = getContext();
		// TypedValue root = ctx.getRootObject();
		// supplied should override root on context
		ExpressionState state = new ExpressionState(ctx,new TypedValue("i am a string"));
		TypedValue stateRoot = state.getRootContextObject();
		Assert.assertEquals(String.class,stateRoot.getTypeDescriptor().getType());
		Assert.assertEquals("i am a string",stateRoot.getValue());
	}

	@Test
	public void testPopulatedNestedScopesMap() {
		ExpressionState state = getState();
		Assert.assertNull(state.lookupLocalVariable("foo"));
		Assert.assertNull(state.lookupLocalVariable("goo"));

		Map<String,Object> m = new HashMap<String,Object>();
		m.put("foo",34);
		m.put("goo","abc");

		state.enterScope(m);
		Assert.assertEquals(34,state.lookupLocalVariable("foo"));
		Assert.assertEquals("abc",state.lookupLocalVariable("goo"));

		state.enterScope(null);
		state.setLocalVariable("foo",12);
		Assert.assertEquals(12,state.lookupLocalVariable("foo"));
		Assert.assertEquals("abc",state.lookupLocalVariable("goo"));

		state.exitScope();
		state.exitScope();
		Assert.assertNull(state.lookupLocalVariable("foo"));
		Assert.assertNull(state.lookupLocalVariable("goo"));
	}

	@Test
	public void testOperators() throws Exception {
		ExpressionState state = getState();
		try {
			state.operate(Operation.ADD,1,2);
			Assert.fail("should have failed");
		} catch (EvaluationException ee) {
			SpelEvaluationException sEx = (SpelEvaluationException)ee;
			Assert.assertEquals(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES,sEx.getMessageCode());
		}

		try {
			state.operate(Operation.ADD,null,null);
			Assert.fail("should have failed");
		} catch (EvaluationException ee) {
			SpelEvaluationException sEx = (SpelEvaluationException)ee;
			Assert.assertEquals(SpelMessage.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES,sEx.getMessageCode());
		}
	}

	@Test
	public void testComparator() {
		ExpressionState state = getState();
		Assert.assertEquals(state.getEvaluationContext().getTypeComparator(),state.getTypeComparator());
	}

	@Test
	public void testTypeLocator() throws EvaluationException {
		ExpressionState state = getState();
		Assert.assertNotNull(state.getEvaluationContext().getTypeLocator());
		Assert.assertEquals(Integer.class,state.findType("java.lang.Integer"));
		try {
			state.findType("someMadeUpName");
			Assert.fail("Should have failed to find it");
		} catch (EvaluationException ee) {
			SpelEvaluationException sEx = (SpelEvaluationException)ee;
			Assert.assertEquals(SpelMessage.TYPE_NOT_FOUND,sEx.getMessageCode());
		}
	}

	@Test
	public void testTypeConversion() throws EvaluationException {
		ExpressionState state = getState();
		String s = (String)state.convertValue(34, TypeDescriptor.valueOf(String.class));
		Assert.assertEquals("34",s);

		s = (String)state.convertValue(new TypedValue(34), TypeDescriptor.valueOf(String.class));
		Assert.assertEquals("34",s);
	}

	@Test
	public void testPropertyAccessors() {
		ExpressionState state = getState();
		Assert.assertEquals(state.getEvaluationContext().getPropertyAccessors(),state.getPropertyAccessors());
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
