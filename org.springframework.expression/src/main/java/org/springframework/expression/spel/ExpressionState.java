/*
 * Copyright 2004-2008 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Operation;
import org.springframework.expression.OperatorOverloader;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeComparator;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeUtils;
import org.springframework.expression.spel.internal.VariableScope;

/**
 * An ExpressionState is for maintaining per-expression-evaluation state, any changes to it are not seen by other
 * expressions but it gives a place to hold local variables and for component expressions in a compound expression to
 * communicate state. This is in contrast to the EvaluationContext, which is shared amongst expression evaluations, and
 * any changes to it will be seen by other expressions or any code that chooses to ask questions of the context.
 * 
 * It also acts as a place for to define common utility routines that the various Ast nodes might need.
 * 
 * @author Andy Clement
 */
public class ExpressionState {

	private EvaluationContext relatedContext;

	private final Stack<VariableScope> variableScopes = new Stack<VariableScope>();

	private final Stack<Object> contextObjects = new Stack<Object>();

	public ExpressionState(EvaluationContext context) {
		relatedContext = context;
		createVariableScope();
	}

	public ExpressionState() {
		createVariableScope();
	}

	private void createVariableScope() {
		variableScopes.add(new VariableScope()); // create an empty top level VariableScope
	}

	/**
	 * The active context object is what unqualified references to properties/etc are resolved against.
	 */
	public Object getActiveContextObject() {
		if (contextObjects.isEmpty()) {
			return relatedContext.getRootContextObject();
		}
		return contextObjects.peek();
	}

	public void pushActiveContextObject(Object obj) {
		contextObjects.push(obj);
	}

	public void popActiveContextObject() {
		contextObjects.pop();
	}

	public Object getRootContextObject() {
		return relatedContext.getRootContextObject();
	}

	public Object lookupReference(Object contextName, Object objectName) throws EvaluationException {
		return relatedContext.lookupReference(contextName, objectName);
	}

	public TypeUtils getTypeUtilities() {
		return relatedContext.getTypeUtils();
	}

	public TypeComparator getTypeComparator() {
		return relatedContext.getTypeUtils().getTypeComparator();
	}

	public Class<?> findType(String type) throws EvaluationException {
		return getTypeUtilities().getTypeLocator().findType(type);
	}

	// TODO all these methods that grab the type converter will fail badly if there isn't one...
	public boolean toBoolean(Object value) throws EvaluationException {
		return ((Boolean) getTypeConverter().convertValue(value, Boolean.TYPE)).booleanValue();
	}

	public char toCharacter(Object value) throws EvaluationException {
		return ((Character) getTypeConverter().convertValue(value, Character.TYPE)).charValue();
	}

	public short toShort(Object value) throws EvaluationException {
		return ((Short) getTypeConverter().convertValue(value, Short.TYPE)).shortValue();
	}

	public int toInteger(Object value) throws EvaluationException {
		return ((Integer) getTypeConverter().convertValue(value, Integer.TYPE)).intValue();
	}

	public double toDouble(Object value) throws EvaluationException {
		return ((Double) getTypeConverter().convertValue(value, Double.TYPE)).doubleValue();
	}

	public float toFloat(Object value) throws EvaluationException {
		return ((Float) getTypeConverter().convertValue(value, Float.TYPE)).floatValue();
	}

	public long toLong(Object value) throws EvaluationException {
		return ((Long) getTypeConverter().convertValue(value, Long.TYPE)).longValue();
	}

	public byte toByte(Object value) throws EvaluationException {
		return ((Byte) getTypeConverter().convertValue(value, Byte.TYPE)).byteValue();
	}

	public TypeConverter getTypeConverter() {
		// TODO cache TypeConverter when it is set/changed?
		return getTypeUtilities().getTypeConverter();
	}

	public void setVariable(String name, Object value) {
		relatedContext.setVariable(name, value);
	}

	public Object lookupVariable(String name) {
		return relatedContext.lookupVariable(name);
	}

	/**
	 * A new scope is entered when a function is invoked
	 */
	public void enterScope(Map<String, Object> argMap) {
		variableScopes.push(new VariableScope(argMap));
	}

	public void enterScope(String name, Object value) {
		variableScopes.push(new VariableScope(name, value));
	}

	public void exitScope() {
		variableScopes.pop();
	}

	public void setLocalVariable(String name, Object value) {
		variableScopes.peek().setVariable(name, value);
	}

	public Object lookupLocalVariable(String name) {
		int scopeNumber = variableScopes.size() - 1;
		for (int i = scopeNumber; i >= 0; i--) {
			if (variableScopes.get(i).definesVariable(name)) {
				return variableScopes.get(i).lookupVariable(name);
			}
		}
		return null;
	}

	public Object operate(Operation op, Object left, Object right) throws SpelException {
		OperatorOverloader overloader = relatedContext.getTypeUtils().getOperatorOverloader();
		try {
			if (overloader != null && overloader.overridesOperation(op, left, right)) {
				return overloader.operate(op, left, right);
			} else {
				throw new SpelException(SpelMessages.OPERATOR_NOT_SUPPORTED_BETWEEN_TYPES, op, left, right);
			}
		} catch (EvaluationException e) {
			if (e instanceof SpelException) {
				throw (SpelException) e;
			} else {
				throw new SpelException(e, SpelMessages.UNEXPECTED_PROBLEM_INVOKING_OPERATOR, op, left, right, e
						.getMessage());
			}
		}
	}

	public List<PropertyAccessor> getPropertyAccessors() {
		return relatedContext.getPropertyAccessors();
	}

	public EvaluationContext getEvaluationContext() {
		return relatedContext;
	}

}
