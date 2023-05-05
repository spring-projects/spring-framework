/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.expression.spel.ast;

import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import org.springframework.asm.MethodVisitor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.lang.Nullable;

/**
 * Represents a variable reference &mdash; for example, {@code #someVar}. Note
 * that this is different than a <em>local</em> variable like {@code $someVar}.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0
 */
public class VariableReference extends SpelNodeImpl {

	// Well known variables:
	private static final String THIS = "this";  // currently active context object

	private static final String ROOT = "root";  // root context object


	private final String name;


	public VariableReference(String variableName, int startPos, int endPos) {
		super(startPos, endPos);
		this.name = variableName;
	}


	@Override
	public ValueRef getValueRef(ExpressionState state) throws SpelEvaluationException {
		if (this.name.equals(THIS)) {
			return new ValueRef.TypedValueHolderValueRef(state.getActiveContextObject(), this);
		}
		if (this.name.equals(ROOT)) {
			return new ValueRef.TypedValueHolderValueRef(state.getRootContextObject(), this);
		}
		TypedValue result = state.lookupVariable(this.name);
		// a null value will mean either the value was null or the variable was not found
		return new VariableRef(this.name, result, state.getEvaluationContext());
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws SpelEvaluationException {
		if (this.name.equals(THIS)) {
			return state.getActiveContextObject();
		}
		if (this.name.equals(ROOT)) {
			TypedValue result = state.getRootContextObject();
			this.exitTypeDescriptor = CodeFlow.toDescriptorFromObject(result.getValue());
			return result;
		}
		TypedValue result = state.lookupVariable(this.name);
		Object value = result.getValue();
		if (value == null || !Modifier.isPublic(value.getClass().getModifiers())) {
			// If the type is not public then when generateCode produces a checkcast to it
			// then an IllegalAccessError will occur.
			// If resorting to Object isn't sufficient, the hierarchy could be traversed for
			// the first public type.
			this.exitTypeDescriptor = "Ljava/lang/Object";
		}
		else {
			this.exitTypeDescriptor = CodeFlow.toDescriptorFromObject(value);
		}
		// a null value will mean either the value was null or the variable was not found
		return result;
	}

	@Override
	public TypedValue setValueInternal(ExpressionState state, Supplier<TypedValue> valueSupplier)
			throws EvaluationException {

		return state.assignVariable(this.name, valueSupplier);
	}

	@Override
	public String toStringAST() {
		return "#" + this.name;
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return !(this.name.equals(THIS) || this.name.equals(ROOT));
	}

	@Override
	public boolean isCompilable() {
		return (this.exitTypeDescriptor != null);
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		if (this.name.equals(ROOT)) {
			mv.visitVarInsn(ALOAD,1);
		}
		else {
			mv.visitVarInsn(ALOAD, 2);
			mv.visitLdcInsn(this.name);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/springframework/expression/EvaluationContext", "lookupVariable", "(Ljava/lang/String;)Ljava/lang/Object;",true);
		}
		CodeFlow.insertCheckCast(mv, this.exitTypeDescriptor);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}


	private static class VariableRef implements ValueRef {

		private final String name;

		private final TypedValue value;

		private final EvaluationContext evaluationContext;

		public VariableRef(String name, TypedValue value, EvaluationContext evaluationContext) {
			this.name = name;
			this.value = value;
			this.evaluationContext = evaluationContext;
		}

		@Override
		public TypedValue getValue() {
			return this.value;
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			this.evaluationContext.setVariable(this.name, newValue);
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

}
