/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;

/**
 * An Indexer can index into some proceeding structure to access a particular piece of it.
 * Supported structures are: strings/collections (lists/sets)/arrays
 *
 * @author Andy Clement
 * @since 3.0
 */
// TODO support multidimensional arrays
// TODO support correct syntax for multidimensional [][][] and not [,,,]
public class Indexer extends SpelNodeImpl {

	// These fields are used when the indexer is being used as a property read accessor. If the name and 
	// target type match these cached values then the cachedReadAccessor is used to read the property.
	// If they do not match, the correct accessor is discovered and then cached for later use.
	private String cachedReadName;
	private Class<?> cachedReadTargetType;
	private PropertyAccessor cachedReadAccessor;

	// These fields are used when the indexer is being used as a property write accessor.  If the name and 
	// target type match these cached values then the cachedWriteAccessor is used to write the property.
	// If they do not match, the correct accessor is discovered and then cached for later use.
	private String cachedWriteName;
	private Class<?> cachedWriteTargetType;
	private PropertyAccessor cachedWriteAccessor;
	

	public Indexer(int pos, SpelNodeImpl expr) {
		super(pos, expr);
	}

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
		if (targetObject instanceof Map) {
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
		
		if (targetObject == null) {
			throw new SpelEvaluationException(getStartPosition(),SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}
		
		// if the object is something that looks indexable by an integer, attempt to treat the index value as a number
		if ((targetObject instanceof Collection ) || targetObject.getClass().isArray() || targetObject instanceof String) {
			int idx = (Integer)state.convertValue(index, TypeDescriptor.valueOf(Integer.class));		
			if (targetObject.getClass().isArray()) {
				return new TypedValue(accessArrayElement(targetObject, idx), targetObjectTypeDescriptor.getElementTypeDescriptor());
			} else if (targetObject instanceof Collection) {
				Collection c = (Collection) targetObject;
				if (idx >= c.size()) {
					if (!growCollection(state, targetObjectTypeDescriptor.getElementType(), idx, c)) {
						throw new SpelEvaluationException(getStartPosition(),SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
					}
				}
				int pos = 0;
				for (Object o : c) {
					if (pos == idx) {
						return new TypedValue(o, targetObjectTypeDescriptor.getElementTypeDescriptor());
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
		}
		
		// Try and treat the index value as a property of the context object
		// TODO could call the conversion service to convert the value to a String		
		if (indexValue.getTypeDescriptor().getType()==String.class) {
			Class<?> targetObjectRuntimeClass = getObjectClass(targetObject);
			String name = (String)indexValue.getValue();
			EvaluationContext eContext = state.getEvaluationContext();

			try {
				if (cachedReadName!=null && cachedReadName.equals(name) && cachedReadTargetType!=null && cachedReadTargetType.equals(targetObjectRuntimeClass)) {
					// it is OK to use the cached accessor
					return cachedReadAccessor.read(eContext, targetObject, name);
				}
				
				List<PropertyAccessor> accessorsToTry = AstUtils.getPropertyAccessorsToTry(targetObjectRuntimeClass, state);
		
				if (accessorsToTry != null) {			
					for (PropertyAccessor accessor : accessorsToTry) {
							if (accessor.canRead(eContext, targetObject, name)) {
								if (accessor instanceof ReflectivePropertyAccessor) {
									accessor = ((ReflectivePropertyAccessor)accessor).createOptimalAccessor(eContext, targetObject, name);
								}
								this.cachedReadAccessor = accessor;
								this.cachedReadName = name;
								this.cachedReadTargetType = targetObjectRuntimeClass;
								return accessor.read(eContext, targetObject, name);
							}
					}
				}
			} catch (AccessException e) {
				throw new SpelEvaluationException(getStartPosition(), e, SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetObjectTypeDescriptor.asString());
			}
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
			return;
		}
		else if (targetObjectTypeDescriptor.isCollection()) {
			int idx = (Integer)state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
			Collection c = (Collection) targetObject;
			if (idx >= c.size()) {
				if (!growCollection(state, targetObjectTypeDescriptor.getElementType(), idx, c)) {
					throw new SpelEvaluationException(getStartPosition(),SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS, c.size(), idx);
				}
			}
			if (targetObject instanceof List) {
				List list = (List)targetObject;
				Object possiblyConvertedValue = state.convertValue(newValue, targetObjectTypeDescriptor.getElementTypeDescriptor());
				list.set(idx,possiblyConvertedValue);
				return;
			}
			else {
				throw new SpelEvaluationException(getStartPosition(),SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetObjectTypeDescriptor.asString());
			}
		}
		
		// Try and treat the index value as a property of the context object		
		// TODO could call the conversion service to convert the value to a String		
		if (index.getTypeDescriptor().getType()==String.class) {
			Class<?> contextObjectClass = getObjectClass(contextObject.getValue());
			String name = (String)index.getValue();
			EvaluationContext eContext = state.getEvaluationContext();
			try {
				if (cachedWriteName!=null && cachedWriteName.equals(name) && cachedWriteTargetType!=null && cachedWriteTargetType.equals(contextObjectClass)) {
					// it is OK to use the cached accessor
					cachedWriteAccessor.write(eContext, targetObject, name,newValue);
					return;
				}
	
				List<PropertyAccessor> accessorsToTry = AstUtils.getPropertyAccessorsToTry(contextObjectClass, state);
				if (accessorsToTry != null) {
						for (PropertyAccessor accessor : accessorsToTry) {
							if (accessor.canWrite(eContext, contextObject.getValue(), name)) {
								this.cachedWriteName = name;
								this.cachedWriteTargetType = contextObjectClass;
								this.cachedWriteAccessor = accessor;
								accessor.write(eContext, contextObject.getValue(), name, newValue);
								return;
							}
						}
				}
			} catch (AccessException ae) {
				throw new SpelEvaluationException(getStartPosition(), ae, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
						name, ae.getMessage());
			}

		}
		
		throw new SpelEvaluationException(getStartPosition(),SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetObjectTypeDescriptor.asString());
	}
	
	/**
	 * Attempt to grow the specified collection so that the specified index is valid.
	 * 
	 * @param state the expression state
	 * @param elementType the type of the elements in the collection
	 * @param index the index into the collection that needs to be valid
	 * @param collection the collection to grow with elements
	 * @return true if collection growing succeeded, otherwise false
	 */
	@SuppressWarnings("unchecked")
	private boolean growCollection(ExpressionState state, Class<?> elementType, int index,
			Collection collection) {
		if (state.getConfiguration().isAutoGrowCollections()) {
			Object newCollectionElement = null;
			try {
				int newElements = index-collection.size();
				if (elementType == null || elementType == Object.class) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE);	
				}
				while (newElements>0) {
					collection.add(elementType.newInstance());
					newElements--;
				}
				newCollectionElement = elementType.newInstance();
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.UNABLE_TO_GROW_COLLECTION);
			}
			collection.add(newCollectionElement);
			return true;
		}
		return false;
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
