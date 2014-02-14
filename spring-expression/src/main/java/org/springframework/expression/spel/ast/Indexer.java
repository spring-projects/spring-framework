/*
 * Copyright 2002-2014 the original author or authors.
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
 * An Indexer can index into some proceeding structure to access a particular piece of it.
 * Supported structures are: strings / collections (lists/sets) / arrays.
 *
 * @author Andy Clement
 * @author Phillip Webb
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
						state.getTypeConverter(), state.getConfiguration().isAutoGrowCollections(),
						state.getConfiguration().getMaximumAutoGrowSize());
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

	private void setArrayElement(TypeConverter converter, Object ctx, int idx, Object newValue,
			Class<?> arrayComponentType) throws EvaluationException {

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
					TypeDescriptor.valueOf(arrayComponentType));
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


	private class ArrayIndexingValueRef implements ValueRef {

		private final TypeConverter typeConverter;

		private final Object array;

		private final int index;

		private final TypeDescriptor typeDescriptor;


		ArrayIndexingValueRef(TypeConverter typeConverter, Object array, int index, TypeDescriptor typeDescriptor) {
			this.typeConverter = typeConverter;
			this.array = array;
			this.index = index;
			this.typeDescriptor = typeDescriptor;
		}


		@Override
		public TypedValue getValue() {
			Object arrayElement = accessArrayElement(this.array, this.index);
			return new TypedValue(arrayElement, this.typeDescriptor.elementTypeDescriptor(arrayElement));
		}

		@Override
		public void setValue(Object newValue) {
			setArrayElement(this.typeConverter, this.array, this.index, newValue,
					this.typeDescriptor.getElementTypeDescriptor().getType());
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	private static class MapIndexingValueRef implements ValueRef {

		private final TypeConverter typeConverter;

		private final Map map;

		private final Object key;

		private final TypeDescriptor mapEntryTypeDescriptor;

		public MapIndexingValueRef(TypeConverter typeConverter, Map map, Object key, TypeDescriptor mapEntryTypeDescriptor) {
			this.typeConverter = typeConverter;
			this.map = map;
			this.key = key;
			this.mapEntryTypeDescriptor = mapEntryTypeDescriptor;
		}

		@Override
		public TypedValue getValue() {
			Object value = this.map.get(this.key);
			return new TypedValue(value,
					this.mapEntryTypeDescriptor.getMapValueTypeDescriptor(value));
		}

		@Override
		public void setValue(Object newValue) {
			if (this.mapEntryTypeDescriptor.getMapValueTypeDescriptor() != null) {
				newValue = this.typeConverter.convertValue(newValue, TypeDescriptor.forObject(newValue),
						this.mapEntryTypeDescriptor.getMapValueTypeDescriptor());
			}
			this.map.put(this.key, newValue);
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}


	private class PropertyIndexingValueRef implements ValueRef {

		private final Object targetObject;

		private final String name;

		private final EvaluationContext evaluationContext;

		private final TypeDescriptor targetObjectTypeDescriptor;

		public PropertyIndexingValueRef(Object targetObject, String value, EvaluationContext evaluationContext,
				TypeDescriptor targetObjectTypeDescriptor) {
			this.targetObject = targetObject;
			this.name = value;
			this.evaluationContext = evaluationContext;
			this.targetObjectTypeDescriptor = targetObjectTypeDescriptor;
		}


		@Override
		public TypedValue getValue() {
			Class<?> targetObjectRuntimeClass = getObjectClass(this.targetObject);
			try {
				if (Indexer.this.cachedReadName != null && Indexer.this.cachedReadName.equals(this.name) &&
						Indexer.this.cachedReadTargetType != null &&
						Indexer.this.cachedReadTargetType.equals(targetObjectRuntimeClass)) {
					// It is OK to use the cached accessor
					return Indexer.this.cachedReadAccessor.read(this.evaluationContext, this.targetObject, this.name);
				}
				List<PropertyAccessor> accessorsToTry = AstUtils.getPropertyAccessorsToTry(
						targetObjectRuntimeClass, this.evaluationContext.getPropertyAccessors());
				if (accessorsToTry != null) {
					for (PropertyAccessor accessor : accessorsToTry) {
						if (accessor.canRead(this.evaluationContext, this.targetObject, this.name)) {
							if (accessor instanceof ReflectivePropertyAccessor) {
								accessor = ((ReflectivePropertyAccessor) accessor).createOptimalAccessor(
										this.evaluationContext, this.targetObject, this.name);
							}
							Indexer.this.cachedReadAccessor = accessor;
							Indexer.this.cachedReadName = this.name;
							Indexer.this.cachedReadTargetType = targetObjectRuntimeClass;
							return accessor.read(this.evaluationContext, this.targetObject, this.name);
						}
					}
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
						this.targetObjectTypeDescriptor.toString());
			}
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
					this.targetObjectTypeDescriptor.toString());
		}

		@Override
		public void setValue(Object newValue) {
			Class<?> contextObjectClass = getObjectClass(this.targetObject);
			try {
				if (Indexer.this.cachedWriteName != null && Indexer.this.cachedWriteName.equals(this.name) &&
						Indexer.this.cachedWriteTargetType != null &&
						Indexer.this.cachedWriteTargetType.equals(contextObjectClass)) {
					// It is OK to use the cached accessor
					Indexer.this.cachedWriteAccessor.write(this.evaluationContext, this.targetObject, this.name, newValue);
					return;
				}
				List<PropertyAccessor> accessorsToTry =
						AstUtils.getPropertyAccessorsToTry(contextObjectClass, this.evaluationContext.getPropertyAccessors());
				if (accessorsToTry != null) {
					for (PropertyAccessor accessor : accessorsToTry) {
						if (accessor.canWrite(this.evaluationContext, this.targetObject, this.name)) {
							Indexer.this.cachedWriteName = this.name;
							Indexer.this.cachedWriteTargetType = contextObjectClass;
							Indexer.this.cachedWriteAccessor = accessor;
							accessor.write(this.evaluationContext, this.targetObject, this.name, newValue);
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

		@Override
		public boolean isWritable() {
			return true;
		}
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	private class CollectionIndexingValueRef implements ValueRef {

		private final Collection collection;

		private final int index;

		private final TypeDescriptor collectionEntryDescriptor;

		private final TypeConverter typeConverter;

		private final boolean growCollection;

		private final int maximumSize;

		public CollectionIndexingValueRef(Collection collection, int index, TypeDescriptor collectionEntryTypeDescriptor,
				TypeConverter typeConverter, boolean growCollection, int maximumSize) {
			this.collection = collection;
			this.index = index;
			this.collectionEntryDescriptor = collectionEntryTypeDescriptor;
			this.typeConverter = typeConverter;
			this.growCollection = growCollection;
			this.maximumSize = maximumSize;
		}


		@Override
		public TypedValue getValue() {
			growCollectionIfNecessary();
			if (this.collection instanceof List) {
				Object o = ((List) this.collection).get(this.index);
				return new TypedValue(o, this.collectionEntryDescriptor.elementTypeDescriptor(o));
			}
			int pos = 0;
			for (Object o : this.collection) {
				if (pos == this.index) {
					return new TypedValue(o, this.collectionEntryDescriptor.elementTypeDescriptor(o));
				}
				pos++;
			}
			throw new IllegalStateException("Failed to find indexed element " + this.index + ": " + this.collection);
		}

		@Override
		public void setValue(Object newValue) {
			growCollectionIfNecessary();
			if (this.collection instanceof List) {
				List list = (List) this.collection;
				if (this.collectionEntryDescriptor.getElementTypeDescriptor() != null) {
					newValue = this.typeConverter.convertValue(newValue, TypeDescriptor.forObject(newValue),
							this.collectionEntryDescriptor.getElementTypeDescriptor());
				}
				list.set(this.index, newValue);
			}
			else {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
						this.collectionEntryDescriptor.toString());
			}
		}

		private void growCollectionIfNecessary() {
			if (this.index >= this.collection.size()) {
				if (!this.growCollection) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS,
							this.collection.size(), this.index);
				}
				if(this.index >= this.maximumSize) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION);
				}
				if (this.collectionEntryDescriptor.getElementTypeDescriptor() == null) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE);
				}
				TypeDescriptor elementType = this.collectionEntryDescriptor.getElementTypeDescriptor();
				try {
					int newElements = this.index - this.collection.size();
					while (newElements >= 0) {
						(this.collection).add(elementType.getType().newInstance());
						newElements--;
					}
				}
				catch (Exception ex) {
					throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.UNABLE_TO_GROW_COLLECTION);
				}
			}
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}


	private class StringIndexingLValue implements ValueRef {

		private final String target;

		private final int index;

		private final TypeDescriptor typeDescriptor;

		public StringIndexingLValue(String target, int index, TypeDescriptor typeDescriptor) {
			this.target = target;
			this.index = index;
			this.typeDescriptor = typeDescriptor;
		}

		@Override
		public TypedValue getValue() {
			if (this.index >= this.target.length()) {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.STRING_INDEX_OUT_OF_BOUNDS,
						this.target.length(), this.index);
			}
			return new TypedValue(String.valueOf(this.target.charAt(this.index)));
		}

		@Override
		public void setValue(Object newValue) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
					this.typeDescriptor.toString());
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

}
