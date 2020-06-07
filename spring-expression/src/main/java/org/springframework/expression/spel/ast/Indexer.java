/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * An Indexer can index into some proceeding structure to access a particular piece of it.
 * Supported structures are: strings / collections (lists/sets) / arrays.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 3.0
 */
// TODO support multidimensional arrays
// TODO support correct syntax for multidimensional [][][] and not [,,,]
public class Indexer extends SpelNodeImpl {

	private enum IndexedType {ARRAY, LIST, MAP, STRING, OBJECT}


	// These fields are used when the indexer is being used as a property read accessor.
	// If the name and target type match these cached values then the cachedReadAccessor
	// is used to read the property. If they do not match, the correct accessor is
	// discovered and then cached for later use.

	@Nullable
	private String cachedReadName;

	@Nullable
	private Class<?> cachedReadTargetType;

	@Nullable
	private PropertyAccessor cachedReadAccessor;

	// These fields are used when the indexer is being used as a property write accessor.
	// If the name and target type match these cached values then the cachedWriteAccessor
	// is used to write the property. If they do not match, the correct accessor is
	// discovered and then cached for later use.

	@Nullable
	private String cachedWriteName;

	@Nullable
	private Class<?> cachedWriteTargetType;

	@Nullable
	private PropertyAccessor cachedWriteAccessor;

	@Nullable
	private IndexedType indexedType;


	public Indexer(int startPos, int endPos, SpelNodeImpl expr) {
		super(startPos, endPos, expr);
	}


	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return getValueRef(state).getValue();
	}

	@Override
	public void setValue(ExpressionState state, @Nullable Object newValue) throws EvaluationException {
		getValueRef(state).setValue(newValue);
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return true;
	}


	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		TypedValue context = state.getActiveContextObject();
		Object target = context.getValue();
		TypeDescriptor targetDescriptor = context.getTypeDescriptor();
		TypedValue indexValue;
		Object index;

		// This first part of the if clause prevents a 'double dereference' of the property (SPR-5847)
		if (target instanceof Map && (this.children[0] instanceof PropertyOrFieldReference)) {
			PropertyOrFieldReference reference = (PropertyOrFieldReference) this.children[0];
			index = reference.getName();
			indexValue = new TypedValue(index);
		}
		else {
			// In case the map key is unqualified, we want it evaluated against the root object
			// so temporarily push that on whilst evaluating the key
			try {
				state.pushActiveContextObject(state.getRootContextObject());
				indexValue = this.children[0].getValueInternal(state);
				index = indexValue.getValue();
				Assert.state(index != null, "No index");
			}
			finally {
				state.popActiveContextObject();
			}
		}

		// Raise a proper exception in case of a null target
		if (target == null) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}
		// At this point, we need a TypeDescriptor for a non-null target object
		Assert.state(targetDescriptor != null, "No type descriptor");

		// Indexing into a Map
		if (target instanceof Map) {
			Object key = index;
			if (targetDescriptor.getMapKeyTypeDescriptor() != null) {
				key = state.convertValue(key, targetDescriptor.getMapKeyTypeDescriptor());
			}
			this.indexedType = IndexedType.MAP;
			return new MapIndexingValueRef(state.getTypeConverter(), (Map<?, ?>) target, key, targetDescriptor);
		}

		// If the object is something that looks indexable by an integer,
		// attempt to treat the index value as a number
		if (target.getClass().isArray() || target instanceof Collection || target instanceof String) {
			int idx = (Integer) state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
			if (target.getClass().isArray()) {
				this.indexedType = IndexedType.ARRAY;
				return new ArrayIndexingValueRef(state.getTypeConverter(), target, idx, targetDescriptor);
			}
			else if (target instanceof Collection) {
				if (target instanceof List) {
					this.indexedType = IndexedType.LIST;
				}
				return new CollectionIndexingValueRef((Collection<?>) target, idx, targetDescriptor,
						state.getTypeConverter(), state.getConfiguration().isAutoGrowCollections(),
						state.getConfiguration().getMaximumAutoGrowSize());
			}
			else {
				this.indexedType = IndexedType.STRING;
				return new StringIndexingLValue((String) target, idx, targetDescriptor);
			}
		}

		// Try and treat the index value as a property of the context object
		// TODO: could call the conversion service to convert the value to a String
		TypeDescriptor valueType = indexValue.getTypeDescriptor();
		if (valueType != null && String.class == valueType.getType()) {
			this.indexedType = IndexedType.OBJECT;
			return new PropertyIndexingValueRef(
					target, (String) index, state.getEvaluationContext(), targetDescriptor);
		}

		throw new SpelEvaluationException(
				getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetDescriptor);
	}

	@Override
	public boolean isCompilable() {
		if (this.indexedType == IndexedType.ARRAY) {
			return (this.exitTypeDescriptor != null);
		}
		else if (this.indexedType == IndexedType.LIST) {
			return this.children[0].isCompilable();
		}
		else if (this.indexedType == IndexedType.MAP) {
			return (this.children[0] instanceof PropertyOrFieldReference || this.children[0].isCompilable());
		}
		else if (this.indexedType == IndexedType.OBJECT) {
			// If the string name is changing the accessor is clearly going to change (so no compilation possible)
			return (this.cachedReadAccessor != null &&
					this.cachedReadAccessor instanceof ReflectivePropertyAccessor.OptimalPropertyAccessor &&
					getChild(0) instanceof StringLiteral);
		}
		return false;
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		String descriptor = cf.lastDescriptor();
		if (descriptor == null) {
			// Stack is empty, should use context object
			cf.loadTarget(mv);
		}

		if (this.indexedType == IndexedType.ARRAY) {
			int insn;
			if ("D".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[D");
				insn = DALOAD;
			}
			else if ("F".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[F");
				insn = FALOAD;
			}
			else if ("J".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[J");
				insn = LALOAD;
			}
			else if ("I".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[I");
				insn = IALOAD;
			}
			else if ("S".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[S");
				insn = SALOAD;
			}
			else if ("B".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[B");
				insn = BALOAD;
			}
			else if ("C".equals(this.exitTypeDescriptor)) {
				mv.visitTypeInsn(CHECKCAST, "[C");
				insn = CALOAD;
			}
			else {
				mv.visitTypeInsn(CHECKCAST, "["+ this.exitTypeDescriptor +
						(CodeFlow.isPrimitiveArray(this.exitTypeDescriptor) ? "" : ";"));
						//depthPlusOne(exitTypeDescriptor)+"Ljava/lang/Object;");
				insn = AALOAD;
			}
			SpelNodeImpl index = this.children[0];
			cf.enterCompilationScope();
			index.generateCode(mv, cf);
			cf.exitCompilationScope();
			mv.visitInsn(insn);
		}

		else if (this.indexedType == IndexedType.LIST) {
			mv.visitTypeInsn(CHECKCAST, "java/util/List");
			cf.enterCompilationScope();
			this.children[0].generateCode(mv, cf);
			cf.exitCompilationScope();
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
		}

		else if (this.indexedType == IndexedType.MAP) {
			mv.visitTypeInsn(CHECKCAST, "java/util/Map");
			// Special case when the key is an unquoted string literal that will be parsed as
			// a property/field reference
			if ((this.children[0] instanceof PropertyOrFieldReference)) {
				PropertyOrFieldReference reference = (PropertyOrFieldReference) this.children[0];
				String mapKeyName = reference.getName();
				mv.visitLdcInsn(mapKeyName);
			}
			else {
				cf.enterCompilationScope();
				this.children[0].generateCode(mv, cf);
				cf.exitCompilationScope();
			}
			mv.visitMethodInsn(
					INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		}

		else if (this.indexedType == IndexedType.OBJECT) {
			ReflectivePropertyAccessor.OptimalPropertyAccessor accessor =
					(ReflectivePropertyAccessor.OptimalPropertyAccessor) this.cachedReadAccessor;
			Assert.state(accessor != null, "No cached read accessor");
			Member member = accessor.member;
			boolean isStatic = Modifier.isStatic(member.getModifiers());
			String classDesc = member.getDeclaringClass().getName().replace('.', '/');

			if (!isStatic) {
				if (descriptor == null) {
					cf.loadTarget(mv);
				}
				if (descriptor == null || !classDesc.equals(descriptor.substring(1))) {
					mv.visitTypeInsn(CHECKCAST, classDesc);
				}
			}

			if (member instanceof Method) {
				mv.visitMethodInsn((isStatic? INVOKESTATIC : INVOKEVIRTUAL), classDesc, member.getName(),
						CodeFlow.createSignatureDescriptor((Method) member), false);
			}
			else {
				mv.visitFieldInsn((isStatic ? GETSTATIC : GETFIELD), classDesc, member.getName(),
						CodeFlow.toJvmDescriptor(((Field) member).getType()));
			}
		}

		cf.pushDescriptor(this.exitTypeDescriptor);
	}

	@Override
	public String toStringAST() {
		StringJoiner sj = new StringJoiner(",", "[", "]");
		for (int i = 0; i < getChildCount(); i++) {
			sj.add(getChild(i).toStringAST());
		}
		return sj.toString();
	}


	private void setArrayElement(TypeConverter converter, Object ctx, int idx, @Nullable Object newValue,
			Class<?> arrayComponentType) throws EvaluationException {

		if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Boolean.class);
		}
		else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Byte.class);
		}
		else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Character.class);
		}
		else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Double.class);
		}
		else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Float.class);
		}
		else if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Integer.class);
		}
		else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Long.class);
		}
		else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, Short.class);
		}
		else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			array[idx] = convertValue(converter, newValue, arrayComponentType);
		}
	}

	private Object accessArrayElement(Object ctx, int idx) throws SpelEvaluationException {
		Class<?> arrayComponentType = ctx.getClass().getComponentType();
		if (arrayComponentType == Boolean.TYPE) {
			boolean[] array = (boolean[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "Z";
			return array[idx];
		}
		else if (arrayComponentType == Byte.TYPE) {
			byte[] array = (byte[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "B";
			return array[idx];
		}
		else if (arrayComponentType == Character.TYPE) {
			char[] array = (char[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "C";
			return array[idx];
		}
		else if (arrayComponentType == Double.TYPE) {
			double[] array = (double[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "D";
			return array[idx];
		}
		else if (arrayComponentType == Float.TYPE) {
			float[] array = (float[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "F";
			return array[idx];
		}
		else if (arrayComponentType == Integer.TYPE) {
			int[] array = (int[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "I";
			return array[idx];
		}
		else if (arrayComponentType == Long.TYPE) {
			long[] array = (long[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "J";
			return array[idx];
		}
		else if (arrayComponentType == Short.TYPE) {
			short[] array = (short[]) ctx;
			checkAccess(array.length, idx);
			this.exitTypeDescriptor = "S";
			return array[idx];
		}
		else {
			Object[] array = (Object[]) ctx;
			checkAccess(array.length, idx);
			Object retValue = array[idx];
			this.exitTypeDescriptor = CodeFlow.toDescriptor(arrayComponentType);
			return retValue;
		}
	}

	private void checkAccess(int arrayLength, int index) throws SpelEvaluationException {
		if (index >= arrayLength) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.ARRAY_INDEX_OUT_OF_BOUNDS,
					arrayLength, index);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T convertValue(TypeConverter converter, @Nullable Object value, Class<T> targetType) {
		T result = (T) converter.convertValue(
				value, TypeDescriptor.forObject(value), TypeDescriptor.valueOf(targetType));
		if (result == null) {
			throw new IllegalStateException("Null conversion result for index [" + value + "]");
		}
		return result;
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
		public void setValue(@Nullable Object newValue) {
			TypeDescriptor elementType = this.typeDescriptor.getElementTypeDescriptor();
			Assert.state(elementType != null, "No element type");
			setArrayElement(this.typeConverter, this.array, this.index, newValue, elementType.getType());
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	private class MapIndexingValueRef implements ValueRef {

		private final TypeConverter typeConverter;

		private final Map map;

		@Nullable
		private final Object key;

		private final TypeDescriptor mapEntryDescriptor;

		public MapIndexingValueRef(
				TypeConverter typeConverter, Map map, @Nullable Object key, TypeDescriptor mapEntryDescriptor) {

			this.typeConverter = typeConverter;
			this.map = map;
			this.key = key;
			this.mapEntryDescriptor = mapEntryDescriptor;
		}

		@Override
		public TypedValue getValue() {
			Object value = this.map.get(this.key);
			exitTypeDescriptor = CodeFlow.toDescriptor(Object.class);
			return new TypedValue(value, this.mapEntryDescriptor.getMapValueTypeDescriptor(value));
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			if (this.mapEntryDescriptor.getMapValueTypeDescriptor() != null) {
				newValue = this.typeConverter.convertValue(newValue, TypeDescriptor.forObject(newValue),
						this.mapEntryDescriptor.getMapValueTypeDescriptor());
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

		public PropertyIndexingValueRef(Object targetObject, String value,
				EvaluationContext evaluationContext, TypeDescriptor targetObjectTypeDescriptor) {

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
					PropertyAccessor accessor = Indexer.this.cachedReadAccessor;
					Assert.state(accessor != null, "No cached read accessor");
					return accessor.read(this.evaluationContext, this.targetObject, this.name);
				}
				List<PropertyAccessor> accessorsToTry = AstUtils.getPropertyAccessorsToTry(
						targetObjectRuntimeClass, this.evaluationContext.getPropertyAccessors());
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canRead(this.evaluationContext, this.targetObject, this.name)) {
						if (accessor instanceof ReflectivePropertyAccessor) {
							accessor = ((ReflectivePropertyAccessor) accessor).createOptimalAccessor(
									this.evaluationContext, this.targetObject, this.name);
						}
						Indexer.this.cachedReadAccessor = accessor;
						Indexer.this.cachedReadName = this.name;
						Indexer.this.cachedReadTargetType = targetObjectRuntimeClass;
						if (accessor instanceof ReflectivePropertyAccessor.OptimalPropertyAccessor) {
							ReflectivePropertyAccessor.OptimalPropertyAccessor optimalAccessor =
									(ReflectivePropertyAccessor.OptimalPropertyAccessor) accessor;
							Member member = optimalAccessor.member;
							Indexer.this.exitTypeDescriptor = CodeFlow.toDescriptor(member instanceof Method ?
									((Method) member).getReturnType() : ((Field) member).getType());
						}
						return accessor.read(this.evaluationContext, this.targetObject, this.name);
					}
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex,
						SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, this.targetObjectTypeDescriptor.toString());
			}
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, this.targetObjectTypeDescriptor.toString());
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			Class<?> contextObjectClass = getObjectClass(this.targetObject);
			try {
				if (Indexer.this.cachedWriteName != null && Indexer.this.cachedWriteName.equals(this.name) &&
						Indexer.this.cachedWriteTargetType != null &&
						Indexer.this.cachedWriteTargetType.equals(contextObjectClass)) {
					// It is OK to use the cached accessor
					PropertyAccessor accessor = Indexer.this.cachedWriteAccessor;
					Assert.state(accessor != null, "No cached write accessor");
					accessor.write(this.evaluationContext, this.targetObject, this.name, newValue);
					return;
				}
				List<PropertyAccessor> accessorsToTry = AstUtils.getPropertyAccessorsToTry(
						contextObjectClass, this.evaluationContext.getPropertyAccessors());
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
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex,
						SpelMessage.EXCEPTION_DURING_PROPERTY_WRITE, this.name, ex.getMessage());
			}
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}


	@SuppressWarnings({"rawtypes", "unchecked"})
	private class CollectionIndexingValueRef implements ValueRef {

		private final Collection collection;

		private final int index;

		private final TypeDescriptor collectionEntryDescriptor;

		private final TypeConverter typeConverter;

		private final boolean growCollection;

		private final int maximumSize;

		public CollectionIndexingValueRef(Collection collection, int index, TypeDescriptor collectionEntryDescriptor,
				TypeConverter typeConverter, boolean growCollection, int maximumSize) {

			this.collection = collection;
			this.index = index;
			this.collectionEntryDescriptor = collectionEntryDescriptor;
			this.typeConverter = typeConverter;
			this.growCollection = growCollection;
			this.maximumSize = maximumSize;
		}

		@Override
		public TypedValue getValue() {
			growCollectionIfNecessary();
			if (this.collection instanceof List) {
				Object o = ((List) this.collection).get(this.index);
				exitTypeDescriptor = CodeFlow.toDescriptor(Object.class);
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
		public void setValue(@Nullable Object newValue) {
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
				if (this.index >= this.maximumSize) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION);
				}
				if (this.collectionEntryDescriptor.getElementTypeDescriptor() == null) {
					throw new SpelEvaluationException(
							getStartPosition(), SpelMessage.UNABLE_TO_GROW_COLLECTION_UNKNOWN_ELEMENT_TYPE);
				}
				TypeDescriptor elementType = this.collectionEntryDescriptor.getElementTypeDescriptor();
				try {
					Constructor<?> ctor = ReflectionUtils.accessibleConstructor(elementType.getType());
					int newElements = this.index - this.collection.size();
					while (newElements >= 0) {
						this.collection.add(ctor.newInstance());
						newElements--;
					}
				}
				catch (Throwable ex) {
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
		public void setValue(@Nullable Object newValue) {
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
					this.typeDescriptor.toString());
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

}
