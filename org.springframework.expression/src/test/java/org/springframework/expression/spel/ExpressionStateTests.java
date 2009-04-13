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

import java.util.HashMap;
import java.util.Map;

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

	public void testConstruction() {		
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		ExpressionState state = new ExpressionState(context);
		assertEquals(context,state.getEvaluationContext());
	}

	// Local variables are in variable scopes which come and go during evaluation.  Normal variables are
	// accessible through the evaluation context
	
	
	public void testLocalVariables() {
		ExpressionState state = getState();
		
		Object value = state.lookupLocalVariable("foo");
		assertNull(value);
		
		state.setLocalVariable("foo",34);
		value = state.lookupLocalVariable("foo");
		assertEquals(34,value);

		state.setLocalVariable("foo",null);
		value = state.lookupLocalVariable("foo");
		assertEquals(null,value);
		
	}

	public void testVariables() {
		ExpressionState state = getState();
		TypedValue typedValue = state.lookupVariable("foo");
		assertEquals(TypedValue.NULL_TYPED_VALUE,typedValue);

		state.setVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		assertEquals(34,typedValue.getValue());
		assertEquals(Integer.class,typedValue.getTypeDescriptor().getType());

		state.setVariable("foo","abc");
		typedValue = state.lookupVariable("foo");
		assertEquals("abc",typedValue.getValue());
		assertEquals(String.class,typedValue.getTypeDescriptor().getType());
	}
	
	public void testNoVariableInteference() {
		ExpressionState state = getState();
		TypedValue typedValue = state.lookupVariable("foo");
		assertEquals(TypedValue.NULL_TYPED_VALUE,typedValue);
		
		state.setLocalVariable("foo",34);
		typedValue = state.lookupVariable("foo");
		assertEquals(TypedValue.NULL_TYPED_VALUE,typedValue);

		state.setVariable("goo","hello");
		assertNull(state.lookupLocalVariable("goo"));
	}
	
	public void testLocalVariableNestedScopes() {
		ExpressionState state = getState();
		assertEquals(null,state.lookupLocalVariable("foo"));
		
		state.setLocalVariable("foo",12);
		assertEquals(12,state.lookupLocalVariable("foo"));
		
		state.enterScope(null);
		assertEquals(12,state.lookupLocalVariable("foo")); // found in upper scope

		state.setLocalVariable("foo","abc");
		assertEquals("abc",state.lookupLocalVariable("foo")); // found in nested scope
		
		state.exitScope();
		assertEquals(12,state.lookupLocalVariable("foo")); // found in nested scope
	}
	
	public void testRootContextObject() {
		ExpressionState state = getState();
		assertEquals(Inventor.class,state.getRootContextObject().getValue().getClass());

		state.getEvaluationContext().setRootObject(null);
		assertEquals(null,state.getRootContextObject().getValue());
		
		state = new ExpressionState(new StandardEvaluationContext());
		assertEquals(TypedValue.NULL_TYPED_VALUE,state.getRootContextObject());
		

		((StandardEvaluationContext)state.getEvaluationContext()).setRootObject(null,TypeDescriptor.NULL_TYPE_DESCRIPTOR);
		assertEquals(null,state.getRootContextObject().getValue());
	}
	
	public void testActiveContextObject() {
		ExpressionState state = getState();
		assertEquals(state.getRootContextObject().getValue(),state.getActiveContextObject().getValue());
		
		state.pushActiveContextObject(new TypedValue(34));
		assertEquals(34,state.getActiveContextObject().getValue());
		
		state.pushActiveContextObject(new TypedValue("hello"));
		assertEquals("hello",state.getActiveContextObject().getValue());
		
		state.popActiveContextObject();
		assertEquals(34,state.getActiveContextObject().getValue());
		
		state.popActiveContextObject();
		assertEquals(state.getRootContextObject().getValue(),state.getActiveContextObject().getValue());
		
		state = new ExpressionState(new StandardEvaluationContext());
		assertEquals(TypedValue.NULL_TYPED_VALUE,state.getActiveContextObject());
	}
	
	public void testPopulatedNestedScopes() {
		ExpressionState state = getState();
		assertNull(state.lookupLocalVariable("foo"));
		
		state.enterScope("foo",34);
		assertEquals(34,state.lookupLocalVariable("foo"));
		
		state.enterScope(null);
		state.setLocalVariable("foo",12);
		assertEquals(12,state.lookupLocalVariable("foo"));

		state.exitScope();
		assertEquals(34,state.lookupLocalVariable("foo"));
		
		state.exitScope();
		assertNull(state.lookupLocalVariable("goo"));
	}
	
	public void testPopulatedNestedScopesMap() {
		ExpressionState state = getState();
		assertNull(state.lookupLocalVariable("foo"));
		assertNull(state.lookupLocalVariable("goo"));
		
		Map<String,Object> m = new HashMap<String,Object>();
		m.put("foo",34);
		m.put("goo","abc");
		
		state.enterScope(m);
		assertEquals(34,state.lookupLocalVariable("foo"));
		assertEquals("abc",state.lookupLocalVariable("goo"));
		
		state.enterScope(null);
		state.setLocalVariable("foo",12);
		assertEquals(12,state.lookupLocalVariable("foo"));
		assertEquals("abc",state.lookupLocalVariable("goo"));

		state.exitScope();
		state.exitScope();
		assertNull(state.lookupLocalVariable("foo"));
		assertNull(state.lookupLocalVariable("goo"));
	}
	
	public void testOperators() throws Exception {
		ExpressionState state = getState();
		try {
			state.operate(Operation.ADD,1,2);
			fail("should have failed");
		} catch (EvaluationException ee) {
			SpelException sEx = (SpelException)ee;
			assertEquals(SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES,sEx.getMessageUnformatted());
		}

		try {
			state.operate(Operation.ADD,null,null);
			fail("should have failed");
		} catch (EvaluationException ee) {
			SpelException sEx = (SpelException)ee;
			assertEquals(SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES,sEx.getMessageUnformatted());
		}
	}
	
	public void testComparator() {
		ExpressionState state = getState();
		assertEquals(state.getEvaluationContext().getTypeComparator(),state.getTypeComparator());
	}
	
	public void testTypeLocator() throws EvaluationException {
		ExpressionState state = getState();
		assertNotNull(state.getEvaluationContext().getTypeLocator());
		assertEquals(Integer.class,state.findType("java.lang.Integer"));
		try {
			state.findType("someMadeUpName");
			fail("Should have failed to find it");
		} catch (EvaluationException ee) {
			SpelException sEx = (SpelException)ee;
			assertEquals(SpelMessages.TYPE_NOT_FOUND,sEx.getMessageUnformatted());
		}
	}
	
	public void testTypeConversion() throws EvaluationException {
		ExpressionState state = getState();
		String s = (String)state.convertValue(34,TypeDescriptor.valueOf(String.class));
		assertEquals("34",s);

		s = (String)state.convertValue(new TypedValue(34),TypeDescriptor.valueOf(String.class));
		assertEquals("34",s);
	}

	public void testPropertyAccessors() {
		ExpressionState state = getState();
		assertEquals(state.getEvaluationContext().getPropertyAccessors(),state.getPropertyAccessors());
	}
	
	/**
	 * @return a new ExpressionState
	 */
	private ExpressionState getState() {
		EvaluationContext context = TestScenarioCreator.getTestEvaluationContext();
		ExpressionState state = new ExpressionState(context);
		return state;
	}
}
