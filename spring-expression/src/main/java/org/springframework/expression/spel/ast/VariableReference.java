/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.ast;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;

/**
 * Represents a variable reference, eg. #someVar. Note this is different to a *local*
 * variable like $someVar
 *
 * @author Andy Clement
 * @since 3.0
 */
public class VariableReference extends SpelNodeImpl {

	// Well known variables:
	private static final String THIS = "this";  // currently active context object

	private static final String ROOT = "root";  // root context object


	private final String name;


	public VariableReference(String variableName, int pos) {
		super(pos);
		this.name = variableName;
	}


	@Override
	public ValueRef getValueRef(ExpressionState state) throws SpelEvaluationException {
		if (this.name.equals(THIS)) {
			return new ValueRef.TypedValueHolderValueRef(state.getActiveContextObject(),this);
		}
		if (this.name.equals(ROOT)) {
			return new ValueRef.TypedValueHolderValueRef(state.getRootContextObject(),this);
		}
		TypedValue result = state.lookupVariable(this.name);
		// a null value will mean either the value was null or the variable was not found
		return new VariableRef(this.name,result,state.getEvaluationContext());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
		if (this.name.equals(THIS)) {
			return state.getActiveContextObject();
		}
		if (this.name.equals(ROOT)) {
			return state.getRootContextObject();
		}
		TypedValue result = state.lookupVariable(this.name);
		// a null value will mean either the value was null or the variable was not found
		return result;
	}

	@Override
	public void setValue(ExpressionState state, Object value) throws SpelEvaluationException {
		state.setVariable(this.name, value);
	}

	@Override
	public String toStringAST() {
		return "#" + this.name;
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return !(this.name.equals(THIS) || this.name.equals(ROOT));
	}


	class VariableRef implements ValueRef {

		private final String name;

		private final TypedValue value;

		private final EvaluationContext evaluationContext;


		public VariableRef(String name, TypedValue value,
				EvaluationContext evaluationContext) {
			this.name = name;
			this.value = value;
			this.evaluationContext = evaluationContext;
		}


		@Override
		public TypedValue getValue() {
			return this.value;
		}

		@Override
		public void setValue(Object newValue) {
			this.evaluationContext.setVariable(this.name, newValue);
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}


}
