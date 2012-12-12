/*
 * Copyright 2002-2012 the original author or authors.
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
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;

/**
 * An Indexer can index into some proceeding structure to access a particular
 * piece of it. Supported structures are: strings/collections
 * (lists/sets)/arrays
 *
 * @author Andy Clement
 * @since 3.0
 */
// TODO support multidimensional arrays
// TODO support correct syntax for multidimensional [][][] and not [,,,]
public class Indexer extends SpelNodeImpl {

	// These fields are used when the indexer is being used as a property read accessor.
	// If the name and target type match these cached values then the cachedReadAccessor
	// is used to read the property. If they do not match, the correct accessor is
	// discovered and then cached for later use.

	private String cachedReadName;

	private Class<?> cachedReadTargetType;

	private PropertyAccessor cachedReadAccessor;

	// These fields are used when the indexer is being used as a property write accessor.
	// If the name and target type match these cached values then the cachedWriteAccessor
	// is used to write the property. If they do not match, the correct accessor is
	// discovered and then cached for later use.

	private String cachedWriteName;

	private Class<?> cachedWriteTargetType;

	private PropertyAccessor cachedWriteAccessor;


	public Indexer(int pos, SpelNodeImpl expr) {
		super(pos, expr);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return getValueRef(state).getValue();
	}

	@Override
	public void setValue(ExpressionState state, Object newValue) throws EvaluationException {
		getValueRef(state).setValue(newValue);
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return true;
	}


	private class ArrayIndexingValueRef implements ValueRef {

		private final TypeConverter typeConverter;

		private final Object array;

		private final int idx;

		private final TypeDescriptor typeDescriptor;

		ArrayIndexingValueRef(TypeConverter typeConverter, Object array, int idx, TypeDescriptor typeDescriptor) {
			this.typeConverter = typeConverter;
			this.array = array;
			this.idx = idx;
			this.typeDescriptor = typeDescriptor;
		}

		public TypedValue getValue() {
			Object arrayElement = accessArrayElement(this.array, this.idx);
			return new TypedValue(arrayElement, this.typeDescriptor.elementTypeDescriptor(arrayElement));
		}

		public void setValue(Object newValue) {
			setArrayElement(this.typeConverter, this.array, this.idx, newValue,
					this.typeDescriptor.getElementTypeDescriptor().getType());
		}

		public boolean isWritable() {
			return true;
		}
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	private class MapIndexingValueRef implements ValueRef {

		private final TypeConverter typeConverter;

		private final Map map;

		private final Object key;

		private final TypeDescriptor mapEntryTypeDescriptor;

		MapIndexingValueRef(TypeConverter typeConverter, Map map, Object key, TypeDescriptor mapEntryTypeDescriptor) {
			this.typeConverter = typeConverter;
			this.map = map;
			this.key = key;
			this.mapEntryTypeDescriptor = mapEntryTypeDescriptor;
		}

		public TypedValue getValue() {
			Object value = this.map.get(this.key);
			return new TypedValue(value, this.mapEntryTypeDescriptor.getMapValueTypeDescriptor(value));
		}

		public void setValue(Object newValue) {
			if (this.mapEntryTypeDescriptor.getMapValueTypeDescriptor() != null) {
				newValue = this.typeConverter.convertValue(newValue, TypeDescriptor.forObject(newValue),
						this.mapEntryTypeDescriptor.getMapValueTypeDescriptor());
			}
			this.map.put(this.key, newValue);
		}

		public boolean isWritable() {
			return true;
		}
	}


	private class PropertyIndexingValueRef implements ValueRef {

		private final Object targetObject;

		private final String name;

		private final EvaluationContext eContext;

		private final TypeDescriptor td;

		public PropertyIndexingValueRef(Object targetObject, String value, EvaluationContext evaluationContext,
				TypeDescriptor targetObjectTypeDescriptor) {
			this.targetObject = targetObject;
			this.name = value;
			this.eContext = evaluationContext;
			this.td = targetObjectTypeDescriptor;
		}

		public TypedValue getValue() {
			Class<?> targetObjectRuntimeClass = getObjectClass(targetObject);
			try {
				if (cachedReadName != null && cachedReadName.equals(name) && cachedReadTargetType != null &&
						cachedReadTargetType.equals(targetObjectRuntimeClass)) {
					// it is OK to use the cached accessor
					return cachedReadAccessor.read(this.eContext, this.targetObject, this.name);
				}
				List<PropertyAccessor> accessorsToTry =
						AstUtils.getPropertyAccessorsToTry(targetObjectRuntimeClass, eContext.getPropertyAccessors());
				if (accessorsToTry != null) {
					for (PropertyAccessor accessor : accessorsToTry) {
						if (accessor.canRead(this.eContext, this.targetObject, this.name)) {
							if (accessor instanceof ReflectivePropertyAccessor) {
								accessor = ((ReflectivePropertyAccessor) accessor).createOptimalAccessor(
										this.eContext, this.targetObject, this.name);
							}
							cachedReadAccessor = accessor;
							cachedReadName = this.name;
							cachedReadTargetType = targetObjectRuntimeClass;
							return accessor.read(this.eContext, this.targetObject, this.name);
						}
					}
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
						this.td.toString());
			}
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
					this.td.toString());
		}

		public void setValue(Object newValue) {
			Class<?> contextObjectClass = getObjectClass(targetObject);
			try {
				if (cachedWriteName != null && cachedWriteName.equals(name) && cachedWriteTargetType != null &&
						cachedWriteTargetType.equals(contextObjectClass)) {
					// it is OK to use the cached accessor
					cachedWriteAccessor.write(this.eContext, this.targetObject, this.name, newValue);
					return;
				}
				List<PropertyAccessor> accessorsToTry =
						AstUtils.getPropertyAccessorsToTry(contextObjectClass, this.eContext.getPropertyAccessors());
				if (accessorsToTry != null) {
					for (PropertyAccessor accessor : accessorsToTry) {
						if (accessor.canWrite(this.eContext, this.targetObject, this.name)) {
							cachedWriteName = this.name;
							cachedWriteTargetType = contextObjectClass;
							cachedWriteAccessor = accessor;
							accessor.write(this.eContext, this.targetObject, this.name, newValue);
							return;
						}
					}
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE,
						this.name, ex.getMessage());
			}
		}

		public boolean isWritable() {
			return true;
		}
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	private class CollectionIndexingValueRef implements ValueRef {

		private final Collection collection;

		private final int index;

		private final TypeDescriptor collectionEntryTypeDescriptor;

		private final TypeConverter typeConverter;

		private final boolean growCollection;

		CollectionIndexingValueRef(Collection collection, int index, TypeDescriptor collectionEntryTypeDescriptor,
				TypeConverter typeConverter, boolean growCollection) {
			this.collection = collection;
			this.index = index;
			this.collectionEntryTypeDescriptor = collectionEntryTypeDescriptor;
			this.typeConverter = typeConverter;
			this.growCollection = growCollection;
		}

		public TypedValue getValue() {
			if (this.index >= this.collection.size()) {
				if (this.growCollection) {
					growCollection(this.collectionEntryTypeDescriptor, this.index, this.collection);
				}
				else {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS,
							this.collection.size(), this.index);
				}
			}
			if (this.collection instanceof List) {
				Object o = ((List) this.collection).get(this.index);
				return new TypedValue(o, this.collectionEntryTypeDescriptor.elementTypeDescriptor(o));
			}
			int pos = 0;
			for (Object o : this.collection) {
				if (pos == this.index) {
					return new TypedValue(o, this.collectionEntryTypeDescriptor.elementTypeDescriptor(o));
				}
				pos++;
			}
			throw new IllegalStateException("Failed to find indexed element " + this.index + ": " + this.collection);
		}

		public void setValue(Object newValue) {
			if (this.index >= this.collection.size()) {
				if (this.growCollection) {
					growCollection(this.collectionEntryTypeDescriptor, this.index, this.collection);
				}
				else {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS,
							this.collection.size(), this.index);
				}
			}
			if (this.collection instanceof List) {
				List list = (List) this.collection;
				if (this.collectionEntryTypeDescriptor.getElementTypeDescriptor() != null) {
					newValue = this.typeConverter.convertValue(newValue, TypeDescriptor.forObject(newValue),
							this.collectionEntryTypeDescriptor.getElementTypeDescriptor());
				}
				list.set(this.index, newValue);
			}
			else {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
						this.collectionEntryTypeDescriptor.toString());
			}
		}

		public boolean isWritable() {
			return true;
		}
	}


	private class StringIndexingLValue implements ValueRef {

		private final String target;

		private final int index;

		private final TypeDescriptor td;

		public StringIndexingLValue(String target, int index, TypeDescriptor td) {
			this.target = target;
			this.index = index;
			this.td = td;
		}

		public TypedValue getValue() {
			if (this.index >= this.target.length()) {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.STRING_INDEX_OUT_OF_BOUNDS,
						this.target.length(), index);
			}
			return new TypedValue(String.valueOf(this.target.charAt(this.index)));
		}

		public void setValue(Object newValue) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
					this.td.toString());
		}

		public boolean isWritable() {
			return true;
		}
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		TypedValue context = state.getActiveContextObject();
		Object targetObject = context.getValue();
		TypeDescriptor targetObjectTypeDescriptor = context.getTypeDescriptor();
		TypedValue indexValue = null;
		Object index = null;

		// This first part of the if clause prevents a 'double dereference' of
		// the property (SPR-5847)
		if (targetObject instanceof Map && (this.children[0] instanceof PropertyOrFieldReference)) {
			PropertyOrFieldReference reference = (PropertyOrFieldReference) this.children[0];
			index = reference.getName();
			indexValue = new TypedValue(index);
		}
		else {
			// In case the map key is unqualified, we want it evaluated against
			// the root object so temporarily push that on whilst evaluating the key
			try {
				state.pushActiveContextObject(state.getRootContextObject());
				indexValue = this.children[0].getValueInternal(state);
				index = indexValue.getValue();
			}
			finally {
				state.popActiveContextObject();
			}
		}

		// Indexing into a Map
		if (targetObject instanceof Map) {
			Object key = index;
			if (targetObjectTypeDescriptor.getMapKeyTypeDescriptor() != null) {
				key = state.convertValue(key, targetObjectTypeDescriptor.getMapKeyTypeDescriptor());
			}
			return new MapIndexingValueRef(state.getTypeConverter(), (Map<?, ?>) targetObject, key,
					targetObjectTypeDescriptor);
		}

		if (targetObject == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}

		// if the object is something that looks indexable by an integer,
		// attempt to treat the index value as a number
		if (targetObject.getClass().isArray() || targetObject instanceof Collection || targetObject instanceof String) {
			int idx = (Integer) state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
			if (targetObject.getClass().isArray()) {
				return new ArrayIndexingValueRef(state.getTypeConverter(), targetObject, idx, targetObjectTypeDescriptor);
			}
			else if (targetObject instanceof Collection) {
				return new CollectionIndexingValueRef((Collection<?>) targetObject, idx, targetObjectTypeDescriptor,
						state.getTypeConverter(), state.getConfiguration().isAutoGrowCollections());
			}
			else if (targetObject instanceof String) {
				return new StringIndexingLValue((String) targetObject, idx, targetObjectTypeDescriptor);
			}
		}

		// Try and treat the index value as a property of the context object
		// TODO could call the conversion service to convert the value to a String
		if (indexValue.getTypeDescriptor().getType() == String.class) {
			return new PropertyIndexingValueRef(targetObject, (String) indexValue.getValue(),
					state.getEvaluationContext(), targetObjectTypeDescriptor);
		}

		throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
				targetObjectTypeDescriptor.toString());
	}

	/**
	 * Attempt to grow the specified collection so that the specified index is valid.
	 * @param targetType the type of the elements in the collection
	 * @param index the index into the collection that needs to be valid
	 * @param collection the collection to grow with elements
	 */
	private void growCollection(TypeDescriptor targetType, int index, Collection<Object> collection) {
		if (targetType.getElementTypeDescriptor() == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE);
		}
		TypeDescriptor elementType = targetType.getElementTypeDescriptor();
		Object newCollectionElement;
		try {
			int newElements = index - collection.size();
			while (newElements > 0) {
				collection.add(elementType.getType().newInstance());
				newElements--;
			}
			newCollectionElement = elementType.getType().newInstance();
		}
		catch (Exception ex) {
			throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.UNABLE_TO_GROW_COLLECTION);
		}
		collection.add(newCollectionElement);
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < getChildCount(); i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(getChild(i).toStringAST());
		}
		sb.append("]");
		return sb.toString();
	}

	private void setArrayElement(TypeConverter converter, Object ctx, int idx, Object newValue, Class<?> clazz)
			throws EvaluationException {
		Class<?> arrayComponentType = clazz;
		if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Integer) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Integer.class));
		}
		else if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Boolean) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Boolean.class));
		}
		else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Character) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Character.class));
		}
		else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Long) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Long.class));
		}
		else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Short) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Short.class));
		}
		else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Double) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Double.class));
		}
		else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Float) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Float.class));
		}
		else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = (Byte) converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(Byte.class));
		}
		else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = converter.convertValue(newValue, TypeDescriptor.forObject(newValue),
					TypeDescriptor.valueOf(clazz));
		}
	}

	private Object accessArrayElement(Object ctx, int idx) throws SpelEvaluationException {
		Class<?> arrayComponentType = ctx.getClass().getComponentType();
		if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
		else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			return array[idx];
		}
	}

	private void checkAccess(int arrayLength, int index) throws SpelEvaluationException {
		if (index > arrayLength) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.ARRAY_INDEX_OUT_OF_BOUNDS,
					arrayLength, index);
		}
	}

}
