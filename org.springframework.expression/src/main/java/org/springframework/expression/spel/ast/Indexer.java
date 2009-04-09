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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.Token;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

// TODO support multidimensional arrays
// TODO support correct syntax for multidimensional [][][] and not [,,,]
/**
 * An Indexer can index into some proceeding structure to access a particular piece of it. Supported structures are:
 * strings/collections (lists/sets)/arrays
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class Indexer extends SpelNodeImpl {

	public Indexer(Token payload) {
		super(payload);
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue context = state.getActiveContextObject();
		Object targetObject = context.getValue();
		TypeDescriptor targetObjectTypeDescriptor = context.getTypeDescriptor();
		TypedValue indexValue =  getChild(0).getValueInternal(state);
		Object index = indexValue.getValue();

		// Indexing into a Map
		if (targetObject instanceof Map) {
			Object possiblyConvertedKey = state.convertValue(indexValue,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapKeyType()));
			Object o = ((Map<?, ?>) targetObject).get(possiblyConvertedKey);
			return new TypedValue(o,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapValueType()));
		}

		int idx = (Integer)state.convertValue(index, INTEGER_TYPE_DESCRIPTOR);

		if (targetObject == null) {
			throw new SpelException(SpelMessages.CANNOT_INDEX_INTO_NULL_VALUE);
		}
		
		if (targetObject.getClass().isArray()) {
			return new TypedValue(accessArrayElement(targetObject, idx),TypeDescriptor.valueOf(targetObjectTypeDescriptor.getElementType()));
		} else if (targetObject instanceof Collection) {
			Collection<?> c = (Collection<?>) targetObject;
			if (idx >= c.size()) {
				throw new SpelException(SpelMessages.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
			}
			int pos = 0;
			for (Object o : c) {
				if (pos == idx) {
					return new TypedValue(o,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getElementType()));
				}
				pos++;
			}
		} else if (targetObject instanceof String) {
			String ctxString = (String) targetObject;
			if (idx >= ctxString.length()) {
				throw new SpelException(SpelMessages.STRING_INDEX_OUT_OF_BOUNDS, ctxString.length(), idx);
			}
			return new TypedValue(String.valueOf(ctxString.charAt(idx)),STRING_TYPE_DESCRIPTOR);
		}
		throw new SpelException(SpelMessages.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetObjectTypeDescriptor.asString());
	}


	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelException {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(ExpressionState state, Object newValue) throws EvaluationException {
		TypedValue contextObject = state.getActiveContextObject();
		Object targetObject = contextObject.getValue();
		TypeDescriptor targetObjectTypeDescriptor = contextObject.getTypeDescriptor();
		TypedValue index = getChild(0).getValueInternal(state);

		if (targetObject == null) {
			throw new SpelException(SpelMessages.CANNOT_INDEX_INTO_NULL_VALUE);
		}
		// Indexing into a Map
		if (targetObjectTypeDescriptor.isMap()) {
			Map map = (Map)targetObject;
			Object possiblyConvertedKey = state.convertValue(index.getValue(),TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapKeyType()));
			Object possiblyConvertedValue = state.convertValue(newValue,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapValueType()));
			map.put(possiblyConvertedKey,possiblyConvertedValue);
			return;
		}

		if (targetObjectTypeDescriptor.isArray()) {
			int idx = (Integer)state.convertValue(index, INTEGER_TYPE_DESCRIPTOR);
			setArrayElement(state, contextObject.getValue(), idx, newValue, targetObjectTypeDescriptor.getElementType());
		} else if (targetObjectTypeDescriptor.isCollection()) {
			int idx = (Integer)state.convertValue(index, INTEGER_TYPE_DESCRIPTOR);
			Collection c = (Collection) targetObject;
			if (idx >= c.size()) {
				throw new SpelException(SpelMessages.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
			}
			if (targetObject instanceof List) {
				List list = (List)targetObject;
				Object possiblyConvertedValue = state.convertValue(newValue,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getElementType()));
				list.set(idx,possiblyConvertedValue);
			} else {
				throw new SpelException(SpelMessages.INDEXING_NOT_SUPPORTED_FOR_TYPE, contextObject.getClass().getName());
			}
		} else {
			throw new SpelException(SpelMessages.INDEXING_NOT_SUPPORTED_FOR_TYPE, contextObject.getClass().getName());
		}
	}
	
	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0)
				sb.append(",");
			sb.append(getChild(i).toStringAST());
		}
		sb.append("]");
		return sb.toString();
	}

	private void setArrayElement(ExpressionState state, Object ctx, int idx, Object newValue, Class clazz) throws EvaluationException {
		Class<?> arrayComponentType = clazz;
		if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Integer)state.convertValue(newValue, INTEGER_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Boolean)state.convertValue(newValue, BOOLEAN_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Character)state.convertValue(newValue, CHARACTER_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Long)state.convertValue(newValue, LONG_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Short)state.convertValue(newValue, SHORT_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Double)state.convertValue(newValue, DOUBLE_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Float)state.convertValue(newValue, FLOAT_TYPE_DESCRIPTOR);
		} else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Byte)state.convertValue(newValue, BYTE_TYPE_DESCRIPTOR);
		} else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = state.convertValue(newValue, TypeDescriptor.valueOf(clazz));
		}		
	}
	
	
	private Object accessArrayElement(Object ctx, int idx) throws SpelException {
		Class<?> arrayComponentType = ctx.getClass().getComponentType();
		if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		} else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
	}


	private void checkAccess(int arrayLength, int index) throws SpelException {
		if (index > arrayLength) {
			throw new SpelException(getCharPositionInLine(), SpelMessages.ARRAY_INDEX_OUT_OF_BOUNDS, arrayLength, index);
		}
	}

}
