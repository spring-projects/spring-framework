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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;

/**
 * An Indexer can index into some proceeding structure to access a particular piece of it. Supported structures are:
 * strings/collections (lists/sets)/arrays
 * 
 * @author Andy Clement
 * @since 3.0
 */
// TODO support multidimensional arrays
// TODO support correct syntax for multidimensional [][][] and not [,,,]
public class Indexer extends SpelNodeImpl {

	public Indexer(int pos, SpelNodeImpl expr) {
		super(pos, expr);
	}

	@SuppressWarnings("unchecked")
	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		TypedValue context = state.getActiveContextObject();
		Object targetObject = context.getValue();
		TypeDescriptor targetObjectTypeDescriptor = context.getTypeDescriptor();
		TypedValue indexValue = null;
		Object index = null;
		
		// This first part of the if clause prevents a 'double dereference' of the property (SPR-5847)
		if (targetObject instanceof Map && (children[0] instanceof PropertyOrFieldReference)) {
			PropertyOrFieldReference reference = (PropertyOrFieldReference)children[0];
			index = reference.getName();
			indexValue = new TypedValue(index);
		}
		else {
			// In case the map key is unqualified, we want it evaluated against the root object so 
			// temporarily push that on whilst evaluating the key
			try {
				state.pushActiveContextObject(state.getRootContextObject());
				indexValue = children[0].getValueInternal(state);
				index = indexValue.getValue();
			}
			finally {
				state.popActiveContextObject();
			}
		}

		// Indexing into a Map
		if (targetObjectTypeDescriptor.isMap()) {
			if (targetObject == null) {
			    // Current decision: attempt to index into null map == exception and does not just return null
				throw new SpelEvaluationException(getStartPosition(),SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
			}
			Object possiblyConvertedKey = index;
			if (targetObjectTypeDescriptor.isMapEntryTypeKnown()) {
				possiblyConvertedKey = state.convertValue(index,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapKeyType()));
			}
			Object o = ((Map<?, ?>) targetObject).get(possiblyConvertedKey);
			if (targetObjectTypeDescriptor.isMapEntryTypeKnown()) {
				return new TypedValue(o, targetObjectTypeDescriptor.getMapValueTypeDescriptor());
			} else {
				return new TypedValue(o);
			}
		}

		int idx = (Integer)state.convertValue(index, TypeDescriptor.valueOf(Integer.class));

		if (targetObject == null) {
			throw new SpelEvaluationException(getStartPosition(),SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}
		
		if (targetObject.getClass().isArray()) {
			return new TypedValue(accessArrayElement(targetObject, idx),TypeDescriptor.valueOf(targetObjectTypeDescriptor.getElementType()));
		} else if (targetObject instanceof Collection) {
			Collection c = (Collection) targetObject;
			if (idx >= c.size()) {
				if (state.getConfiguration().isAutoGrowCollections()) {
					// Grow the collection
					Object newCollectionElement = null;
					try {
						int newElements = idx-c.size();
						Class elementClass = targetObjectTypeDescriptor.getElementType();
						if (elementClass == null) {
							throw new SpelEvaluationException(getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE);	
						}
						while (newElements>0) {
							c.add(elementClass.newInstance());
							newElements--;
						}
						newCollectionElement = targetObjectTypeDescriptor.getElementType().newInstance();
					}
					catch (Exception ex) {
						throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.UNABLE_TO_GROW_COLLECTION);
					}
					c.add(newCollectionElement);
					return new TypedValue(newCollectionElement,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getElementType()));
				}
				else {
					throw new SpelEvaluationException(getStartPosition(),SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
				}
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
				throw new SpelEvaluationException(getStartPosition(),SpelMessage.STRING_INDEX_OUT_OF_BOUNDS, ctxString.length(), idx);
			}
			return new TypedValue(String.valueOf(ctxString.charAt(idx)));
		}
		throw new SpelEvaluationException(getStartPosition(),SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetObjectTypeDescriptor.asString());
	}


	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setValue(ExpressionState state, Object newValue) throws EvaluationException {
		TypedValue contextObject = state.getActiveContextObject();
		Object targetObject = contextObject.getValue();
		TypeDescriptor targetObjectTypeDescriptor = contextObject.getTypeDescriptor();
		TypedValue index = children[0].getValueInternal(state);

		if (targetObject == null) {
			throw new SpelEvaluationException(SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}
		// Indexing into a Map
		if (targetObjectTypeDescriptor.isMap()) {
			Map map = (Map)targetObject;
			Object possiblyConvertedKey = index;
			Object possiblyConvertedValue = newValue;
			if (targetObjectTypeDescriptor.isMapEntryTypeKnown()) {
			  possiblyConvertedKey = state.convertValue(index.getValue(),TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapKeyType()));
			  possiblyConvertedValue = state.convertValue(newValue,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getMapValueType()));
			}
			map.put(possiblyConvertedKey,possiblyConvertedValue);
			return;
		}

		if (targetObjectTypeDescriptor.isArray()) {
			int idx = (Integer)state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
			setArrayElement(state, contextObject.getValue(), idx, newValue, targetObjectTypeDescriptor.getElementType());
		}
		else if (targetObjectTypeDescriptor.isCollection()) {
			int idx = (Integer)state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
			Collection c = (Collection) targetObject;
			if (idx >= c.size()) {
				throw new SpelEvaluationException(getStartPosition(),SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
			}
			if (targetObject instanceof List) {
				List list = (List)targetObject;
				Object possiblyConvertedValue = state.convertValue(newValue,TypeDescriptor.valueOf(targetObjectTypeDescriptor.getElementType()));
				list.set(idx,possiblyConvertedValue);
			}
			else {
				throw new SpelEvaluationException(getStartPosition(),SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, contextObject.getClass().getName());
			}
		} else {
			throw new SpelEvaluationException(getStartPosition(),SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, contextObject.getClass().getName());
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
			array[idx] = (Integer)state.convertValue(newValue, TypeDescriptor.valueOf(Integer.class));
		} else if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Boolean)state.convertValue(newValue, TypeDescriptor.valueOf(Boolean.class));
		} else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Character)state.convertValue(newValue, TypeDescriptor.valueOf(Character.class));
		} else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Long)state.convertValue(newValue, TypeDescriptor.valueOf(Long.class));
		} else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Short)state.convertValue(newValue, TypeDescriptor.valueOf(Short.class));
		} else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Double)state.convertValue(newValue, TypeDescriptor.valueOf(Double.class));
		} else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Float)state.convertValue(newValue, TypeDescriptor.valueOf(Float.class));
		} else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Byte)state.convertValue(newValue, TypeDescriptor.valueOf(Byte.class));
		} else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = state.convertValue(newValue, TypeDescriptor.valueOf(clazz));
		}		
	}
	
	private Object accessArrayElement(Object ctx, int idx) throws SpelEvaluationException {
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

	private void checkAccess(int arrayLength, int index) throws SpelEvaluationException {
		if (index > arrayLength) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.ARRAY_INDEX_OUT_OF_BOUNDS, arrayLength, index);
		}
	}

}
