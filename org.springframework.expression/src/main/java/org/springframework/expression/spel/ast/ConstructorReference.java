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

package org.springframework.expression.spel.ast;

import java.lang.reflect.Array;
import java.util.List;

import org.antlr.runtime.Token;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Represents the invocation of a constructor. Either a constructor on a regular type or construction of an array. When
 * an array is constructed, an initializer can be specified.
 * <p>
 * Examples:<br>
 * new String('hello world')<br>
 * new int[]{1,2,3,4}<br>
 * new int[3] new int[3]{1,2,3}
 * 
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ConstructorReference extends SpelNodeImpl {

	/**
	 * If true then this is an array constructor, for example, 'new String[]', rather than a simple constructor 'new
	 * String()'
	 */
	private final boolean isArrayConstructor;

	/**
	 * The cached executor that may be reused on subsequent evaluations.
	 */
	private ConstructorExecutor cachedExecutor;


	public ConstructorReference(Token payload, boolean isArrayConstructor) {
		super(payload);
		this.isArrayConstructor = isArrayConstructor;
	}


	/**
	 * Implements getValue() - delegating to the code for building an array or a simple type.
	 */
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		if (this.isArrayConstructor) {
			return createArray(state);
		}
		else {
			return createNewInstance(state);
		}
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("new ");

		int index = 0;
		sb.append(getChild(index++).toStringAST());

		if (!this.isArrayConstructor) {
			sb.append("(");
			for (int i = index; i < getChildCount(); i++) {
				if (i > index)
					sb.append(",");
				sb.append(getChild(i).toStringAST());
			}
			sb.append(")");
		}
		else {
			// Next child is EXPRESSIONLIST token with children that are the
			// expressions giving array size
			sb.append("[");
			SpelNodeImpl arrayRank = getChild(index++);
			for (int i = 0; i < arrayRank.getChildCount(); i++) {
				if (i > 0)
					sb.append(",");
				sb.append(arrayRank.getChild(i).toStringAST());
			}
			sb.append("]");
			if (index < getChildCount())
				sb.append(" ").append(getChild(index).toStringAST());
		}
		return sb.toString();
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return false;
	}


	/**
	 * Create an array and return it. The children of this node indicate the type of array, the array ranks and any
	 * optional initializer that might have been supplied.
	 * @param state the expression state within which this expression is being evaluated
	 * @return the new array
	 * @throws EvaluationException if there is a problem creating the array
	 */
	private TypedValue createArray(ExpressionState state) throws EvaluationException {
		TypedValue intendedArrayType = getChild(0).getValueInternal(state);
		if (!(intendedArrayType.getValue() instanceof String)) {
			throw new SpelException(getChild(0).getCharPositionInLine(),
					SpelMessages.TYPE_NAME_EXPECTED_FOR_ARRAY_CONSTRUCTION,
					FormatHelper.formatClassNameForMessage(intendedArrayType.getClass()));
		}
		String type = (String) intendedArrayType.getValue();
		Class<?> componentType = null;
		TypeCode arrayTypeCode = TypeCode.forName(type);
		if (arrayTypeCode == TypeCode.OBJECT) {
			componentType = state.findType(type);
		} else {
			componentType = arrayTypeCode.getType();
		}

		Object newArray = null;
		TypeDescriptor newArrayTypeDescriptor = null;

		if (getChild(1).getChildCount() == 0) { // are the array ranks defined?
			if (getChildCount() < 3) {
				throw new SpelException(getCharPositionInLine(),
						SpelMessages.NO_SIZE_OR_INITIALIZER_FOR_ARRAY_CONSTRUCTION);
			}
			// no array ranks so use the size of the initializer to determine array size
			int arraySize = getChild(2).getChildCount();
			newArray = Array.newInstance(componentType, arraySize);
			newArrayTypeDescriptor = TypeDescriptor.valueOf(newArray.getClass());
		}
		else {
			// Array ranks are specified but is it a single or multiple dimension array?
			int dimensions = getChild(1).getChildCount();
			if (dimensions == 1) {
				Object o = getChild(1).getValueInternal(state);
				int arraySize = state.convertValue(o, INTEGER_TYPE_DESCRIPTOR);
				if (getChildCount() == 3) {
					// Check initializer length matches array size length
					int initializerLength = getChild(2).getChildCount();
					if (initializerLength != arraySize) {
						throw new SpelException(getChild(2).getCharPositionInLine(),
								SpelMessages.INITIALIZER_LENGTH_INCORRECT, initializerLength, arraySize);
					}
				}
				newArray = Array.newInstance(componentType, arraySize);
				newArrayTypeDescriptor = TypeDescriptor.valueOf(newArray.getClass());
			}
			else {
				// Multi-dimensional - hold onto your hat !
				int[] dims = new int[dimensions];
				for (int d = 0; d < dimensions; d++) {
					dims[d] = state.convertValue(getChild(1).getChild(d).getValueInternal(state), INTEGER_TYPE_DESCRIPTOR);
				}
				newArray = Array.newInstance(componentType, dims);
				newArrayTypeDescriptor = TypeDescriptor.valueOf(newArray.getClass());
				// TODO check any specified initializer for the multidim array matches
			}
		}

		// Populate the array using the initializer if one is specified
		if (getChildCount() == 3) {
			SpelNodeImpl initializer = getChild(2);
			if (arrayTypeCode == TypeCode.OBJECT) {
				Object[] newObjectArray = (Object[]) newArray;
				for (int i = 0; i < newObjectArray.length; i++) {
					SpelNodeImpl elementNode = initializer.getChild(i);
					Object arrayEntry = elementNode.getValueInternal(state);
					if (!componentType.isAssignableFrom(arrayEntry.getClass())) {
						throw new SpelException(elementNode.getCharPositionInLine(),
								SpelMessages.INCORRECT_ELEMENT_TYPE_FOR_ARRAY, componentType.getName(), arrayEntry
										.getClass().getName());
					}
					newObjectArray[i] = arrayEntry;
				}
			} else if (arrayTypeCode == TypeCode.INT) {
				int[] newIntArray = (int[]) newArray;
				for (int i = 0; i < newIntArray.length; i++) {
					newIntArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), INTEGER_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.BOOLEAN) {
				boolean[] newBooleanArray = (boolean[]) newArray;
				for (int i = 0; i < newBooleanArray.length; i++) {
					newBooleanArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), BOOLEAN_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.CHAR) {
				char[] newCharArray = (char[]) newArray;
				for (int i = 0; i < newCharArray.length; i++) {
					newCharArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), CHARACTER_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.SHORT) {
				short[] newShortArray = (short[]) newArray;
				for (int i = 0; i < newShortArray.length; i++) {
					newShortArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), SHORT_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.LONG) {
				long[] newLongArray = (long[]) newArray;
				for (int i = 0; i < newLongArray.length; i++) {
					newLongArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), LONG_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.FLOAT) {
				float[] newFloatArray = (float[]) newArray;
				for (int i = 0; i < newFloatArray.length; i++) {
					newFloatArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), FLOAT_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.DOUBLE) {
				double[] newDoubleArray = (double[]) newArray;
				for (int i = 0; i < newDoubleArray.length; i++) {
					newDoubleArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), DOUBLE_TYPE_DESCRIPTOR);
				}
			} else if (arrayTypeCode == TypeCode.BYTE) {
				byte[] newByteArray = (byte[]) newArray;
				for (int i = 0; i < newByteArray.length; i++) {
					newByteArray[i] = state.convertValue(initializer.getChild(i).getValueInternal(state), BYTE_TYPE_DESCRIPTOR);
				}
			}
		}

		return new TypedValue(newArray, newArrayTypeDescriptor);
	}

	/**
	 * Create a new ordinary object and return it.
	 * @param state the expression state within which this expression is being evaluated
	 * @return the new object
	 * @throws EvaluationException if there is a problem creating the object
	 */
	private TypedValue createNewInstance(ExpressionState state) throws EvaluationException {
		Object[] arguments = new Object[getChildCount() - 1];
		Class<?>[] argumentTypes = new Class[getChildCount() - 1];
		for (int i = 0; i < arguments.length; i++) {
			TypedValue childValue = getChild(i + 1).getValueInternal(state);
			arguments[i] = childValue.getValue();
			argumentTypes[i] = childValue.getValue().getClass();
		}

		ConstructorExecutor executorToUse = this.cachedExecutor;
		if (executorToUse != null) {
			try {
				return executorToUse.execute(state.getEvaluationContext(), arguments);
			}
			catch (AccessException ae) {
				// this is OK - it may have gone stale due to a class change,
				// let's try to get a new one and call it before giving up
				this.cachedExecutor = null;
			}
		}

		// either there was no accessor or it no longer exists
		String typename = (String) getChild(0).getValueInternal(state).getValue();
		executorToUse = findExecutorForConstructor(typename, argumentTypes, state);
		try {
			return executorToUse.execute(state.getEvaluationContext(), arguments);
		}
		catch (AccessException ae) {
			throw new SpelException(ae, SpelMessages.EXCEPTION_DURING_CONSTRUCTOR_INVOCATION, typename, ae.getMessage());
		}
	}

	/**
	 * Go through the list of registered constructor resolvers and see if any can find a constructor that takes the
	 * specified set of arguments.
	 * @param typename the type trying to be constructed
	 * @param argumentTypes the types of the arguments supplied that the constructor must take
	 * @param state the current state of the expression
	 * @return a reusable ConstructorExecutor that can be invoked to run the constructor or null
	 * @throws SpelException if there is a problem locating the constructor
	 */
	protected ConstructorExecutor findExecutorForConstructor(
			String typename, Class<?>[] argumentTypes, ExpressionState state) throws SpelException {

		EvaluationContext eContext = state.getEvaluationContext();
		List<ConstructorResolver> cResolvers = eContext.getConstructorResolvers();
		if (cResolvers != null) {
			for (ConstructorResolver ctorResolver : cResolvers) {
				try {
					ConstructorExecutor cEx = ctorResolver.resolve(state.getEvaluationContext(), typename,
							argumentTypes);
					if (cEx != null) {
						return cEx;
					}
				}
				catch (AccessException ex) {
					throw new SpelException(ex, SpelMessages.PROBLEM_LOCATING_CONSTRUCTOR, typename,
							FormatHelper.formatMethodForMessage("", argumentTypes));
				}
			}
		}
		throw new SpelException(SpelMessages.CONSTRUCTOR_NOT_FOUND, typename, FormatHelper.formatMethodForMessage("",
				argumentTypes));
	}

}
