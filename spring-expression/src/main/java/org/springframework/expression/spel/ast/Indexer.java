/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.asm.Label;
import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectivePropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * An {@code Indexer} can index into some proceeding structure to access a
 * particular element of the structure.
 *
 * <p>Numerical index values are zero-based, such as when accessing the
 * n<sup>th</sup> element of an array in Java.
 *
 * <h3>Supported Structures</h3>
 *
 * <ul>
 * <li>Arrays: the n<sup>th</sup> element</li>
 * <li>Collections (lists and sets): the n<sup>th</sup> element</li>
 * <li>Strings: the n<sup>th</sup> character as a {@link String}</li>
 * <li>Maps: the value for the specified key</li>
 * <li>Objects: the property with the specified name</li>
 * </ul>
 *
 * <h3>Null-safe Indexing</h3>
 *
 * <p>As of Spring Framework 6.2, null-safe indexing is supported via the {@code '?.'}
 * operator. For example, {@code 'colors?.[0]'} will evaluate to {@code null} if
 * {@code colors} is {@code null} and will otherwise evaluate to the 0<sup>th</sup>
 * color.
 *
 * @author Andy Clement
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 3.0
 */
public class Indexer extends SpelNodeImpl {

	private enum IndexedType {ARRAY, LIST, MAP, STRING, OBJECT}


	private final boolean nullSafe;

	@Nullable
	private IndexedType indexedType;

	@Nullable
	private volatile String originalPrimitiveExitTypeDescriptor;

	@Nullable
	private volatile String arrayTypeDescriptor;

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


	/**
	 * Create an {@code Indexer} with the given start position, end position, and
	 * index expression.
	 * @see #Indexer(boolean, int, int, SpelNodeImpl)
	 * @deprecated as of Spring Framework 6.2, in favor of {@link #Indexer(boolean, int, int, SpelNodeImpl)}
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	public Indexer(int startPos, int endPos, SpelNodeImpl indexExpression) {
		this(false, startPos, endPos, indexExpression);
	}

	/**
	 * Create an {@code Indexer} with the given null-safe flag, start position,
	 * end position, and index expression.
	 * @since 6.2
	 */
	public Indexer(boolean nullSafe, int startPos, int endPos, SpelNodeImpl indexExpression) {
		super(startPos, endPos, indexExpression);
		this.nullSafe = nullSafe;
	}


	/**
	 * Does this node represent a null-safe index operation?
	 * @since 6.2
	 */
	@Override
	public final boolean isNullSafe() {
		return this.nullSafe;
	}

	@Override
	public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
		return getValueRef(state).getValue();
	}

	@Override
	public TypedValue setValueInternal(ExpressionState state, Supplier<TypedValue> valueSupplier)
			throws EvaluationException {

		TypedValue typedValue = valueSupplier.get();
		// TODO Query IndexAccessor's canWrite() method before invoking its write() method.
		getValueRef(state).setValue(typedValue.getValue());
		return typedValue;
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return getValueRef(expressionState).isWritable();
	}


	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		TypedValue context = state.getActiveContextObject();
		Object target = context.getValue();

		if (target == null) {
			if (this.nullSafe) {
				return ValueRef.NullValueRef.INSTANCE;
			}
			// Raise a proper exception in case of a null target
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.CANNOT_INDEX_INTO_NULL_VALUE);
		}

		TypeDescriptor targetDescriptor = context.getTypeDescriptor();
		TypedValue indexValue;
		Object index;

		// This first part of the if clause prevents a 'double dereference' of the property (SPR-5847)
		if (target instanceof Map && (this.children[0] instanceof PropertyOrFieldReference reference)) {
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

		// At this point, we need a TypeDescriptor for a non-null target object
		Assert.state(targetDescriptor != null, "No type descriptor");

		// Indexing into a Map
		if (target instanceof Map<?, ?> map) {
			Object key = index;
			TypeDescriptor mapKeyTypeDescriptor = targetDescriptor.getMapKeyTypeDescriptor();
			if (mapKeyTypeDescriptor != null) {
				key = state.convertValue(key, mapKeyTypeDescriptor);
			}
			this.indexedType = IndexedType.MAP;
			return new MapIndexingValueRef(state.getTypeConverter(), map, key, targetDescriptor);
		}

		// If the object is something that looks indexable by an integer,
		// attempt to treat the index value as a number
		if (target.getClass().isArray() || target instanceof Collection || target instanceof String) {
			int idx = (Integer) state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
			if (target.getClass().isArray()) {
				this.indexedType = IndexedType.ARRAY;
				return new ArrayIndexingValueRef(state.getTypeConverter(), target, idx, targetDescriptor);
			}
			else if (target instanceof Collection<?> collection) {
				if (target instanceof List) {
					this.indexedType = IndexedType.LIST;
				}
				return new CollectionIndexingValueRef(collection, idx, targetDescriptor,
						state.getTypeConverter(), state.getConfiguration().isAutoGrowCollections(),
						state.getConfiguration().getMaximumAutoGrowSize());
			}
			else {
				this.indexedType = IndexedType.STRING;
				return new StringIndexingValueRef((String) target, idx, targetDescriptor);
			}
		}

		// Try and treat the index value as a property of the context object
		TypeDescriptor valueType = indexValue.getTypeDescriptor();
		if (valueType != null && String.class == valueType.getType()) {
			this.indexedType = IndexedType.OBJECT;
			return new PropertyIndexingValueRef(
					target, (String) index, state.getEvaluationContext(), targetDescriptor);
		}

		EvaluationContext evalContext = state.getEvaluationContext();
		List<IndexAccessor> accessorsToTry = getIndexAccessorsToTry(target, evalContext.getIndexAccessors());
		try {
			for (IndexAccessor indexAccessor : accessorsToTry) {
				if (indexAccessor.canRead(evalContext, target, index)) {
					return new IndexAccessorValueRef(indexAccessor, target, index, evalContext, targetDescriptor);
				}
			}
		}
		catch (Exception ex) {
			throw new SpelEvaluationException(
					getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_INDEX_READ, index,
					target.getClass().getTypeName());
		}

		throw new SpelEvaluationException(
				getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetDescriptor);
	}

	@Override
	public boolean isCompilable() {
		if (this.indexedType == IndexedType.ARRAY) {
			return (this.exitTypeDescriptor != null && this.arrayTypeDescriptor != null);
		}
		SpelNodeImpl index = this.children[0];
		if (this.indexedType == IndexedType.LIST) {
			return index.isCompilable();
		}
		else if (this.indexedType == IndexedType.MAP) {
			return (index instanceof PropertyOrFieldReference || index.isCompilable());
		}
		else if (this.indexedType == IndexedType.OBJECT) {
			// If the string name is changing, the accessor is clearly going to change.
			// So compilation is only possible if the index expression is a StringLiteral.
			return (index instanceof StringLiteral &&
					this.cachedReadAccessor instanceof CompilablePropertyAccessor compilablePropertyAccessor &&
					compilablePropertyAccessor.isCompilable());
		}
		return false;
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		String exitTypeDescriptor = this.exitTypeDescriptor;
		String descriptor = cf.lastDescriptor();
		if (descriptor == null) {
			// Stack is empty, should use context object
			cf.loadTarget(mv);
		}

		Label skipIfNull = null;
		if (this.nullSafe) {
			mv.visitInsn(DUP);
			skipIfNull = new Label();
			Label continueLabel = new Label();
			mv.visitJumpInsn(IFNONNULL, continueLabel);
			CodeFlow.insertCheckCast(mv, exitTypeDescriptor);
			mv.visitJumpInsn(GOTO, skipIfNull);
			mv.visitLabel(continueLabel);
		}

		SpelNodeImpl index = this.children[0];

		if (this.indexedType == IndexedType.ARRAY) {
			String arrayTypeDescriptor = this.arrayTypeDescriptor;
			Assert.state(exitTypeDescriptor != null && arrayTypeDescriptor != null,
					"Array not compilable without descriptors");
			CodeFlow.insertCheckCast(mv, arrayTypeDescriptor);
			int insn = switch (arrayTypeDescriptor) {
				case "[D" -> DALOAD;
				case "[F" -> FALOAD;
				case "[J" -> LALOAD;
				case "[I" -> IALOAD;
				case "[S" -> SALOAD;
				case "[B", "[Z" -> BALOAD; // byte[] & boolean[] are both loaded via BALOAD
				case "[C" -> CALOAD;
				default -> AALOAD;
			};

			cf.enterCompilationScope();
			index.generateCode(mv, cf);
			cf.exitCompilationScope();
			mv.visitInsn(insn);
		}

		else if (this.indexedType == IndexedType.LIST) {
			mv.visitTypeInsn(CHECKCAST, "java/util/List");
			cf.enterCompilationScope();
			index.generateCode(mv, cf);
			cf.exitCompilationScope();
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
		}

		else if (this.indexedType == IndexedType.MAP) {
			mv.visitTypeInsn(CHECKCAST, "java/util/Map");
			// Special case when the key is an unquoted string literal that will be parsed as
			// a property/field reference
			if ((index instanceof PropertyOrFieldReference reference)) {
				String mapKeyName = reference.getName();
				mv.visitLdcInsn(mapKeyName);
			}
			else {
				cf.enterCompilationScope();
				index.generateCode(mv, cf);
				cf.exitCompilationScope();
			}
			mv.visitMethodInsn(
					INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		}

		else if (this.indexedType == IndexedType.OBJECT) {
			if (!(index instanceof StringLiteral stringLiteral)) {
				throw new IllegalStateException(
						"Index expression must be a StringLiteral, but was: " + index.getClass().getName());
			}
			CompilablePropertyAccessor compilablePropertyAccessor = (CompilablePropertyAccessor) this.cachedReadAccessor;
			Assert.state(compilablePropertyAccessor != null, "No cached read accessor");
			String propertyName = (String) stringLiteral.getLiteralValue().getValue();
			Assert.state(propertyName != null, "No property name");
			compilablePropertyAccessor.generateCode(propertyName, mv, cf);
		}

		cf.pushDescriptor(exitTypeDescriptor);

		if (skipIfNull != null) {
			if (this.originalPrimitiveExitTypeDescriptor != null) {
				// The output of the indexer is a primitive, but from the logic above it
				// might be null. So, to have a common stack element type at the skipIfNull
				// target, it is necessary to box the primitive.
				CodeFlow.insertBoxIfNecessary(mv, this.originalPrimitiveExitTypeDescriptor);
			}
			mv.visitLabel(skipIfNull);
		}
	}

	@Override
	public String toStringAST() {
		return "[" + getChild(0).toStringAST() + "]";
	}


	private void setExitTypeDescriptor(String descriptor) {
		// If this indexer would return a primitive - and yet it is also marked
		// null-safe - then the exit type descriptor must be promoted to the box
		// type to allow a null value to be passed on.
		if (this.nullSafe && CodeFlow.isPrimitive(descriptor)) {
			this.originalPrimitiveExitTypeDescriptor = descriptor;
			this.exitTypeDescriptor = CodeFlow.toBoxedDescriptor(descriptor);
		}
		else {
			this.exitTypeDescriptor = descriptor;
		}
	}

	/**
	 * Determine the set of index accessors that should be used to try to access
	 * an index on the specified context object.
	 * <p>Delegates to {@link AstUtils#getAccessorsToTry(Class, List)}.
	 * @param targetObject the object upon which index access is being attempted
	 * @param indexAccessors the list of index accessors to process
	 * @return a list of accessors that should be tried in order to access the
	 * index, or an empty list if no suitable accessor could be found
	 */
	private static List<IndexAccessor> getIndexAccessorsToTry(
			@Nullable Object targetObject, List<IndexAccessor> indexAccessors) {

		Class<?> targetType = (targetObject != null ? targetObject.getClass() : null);
		return AstUtils.getAccessorsToTry(targetType, indexAccessors);
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
			Object arrayElement = getArrayElement(this.array, this.index);
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

		private Object getArrayElement(Object ctx, int idx) throws SpelEvaluationException {
			Class<?> arrayComponentType = ctx.getClass().componentType();
			if (arrayComponentType == boolean.class) {
				boolean[] array = (boolean[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("Z");
				Indexer.this.arrayTypeDescriptor = "[Z";
				return array[idx];
			}
			else if (arrayComponentType == byte.class) {
				byte[] array = (byte[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("B");
				Indexer.this.arrayTypeDescriptor = "[B";
				return array[idx];
			}
			else if (arrayComponentType == char.class) {
				char[] array = (char[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("C");
				Indexer.this.arrayTypeDescriptor = "[C";
				return array[idx];
			}
			else if (arrayComponentType == double.class) {
				double[] array = (double[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("D");
				Indexer.this.arrayTypeDescriptor = "[D";
				return array[idx];
			}
			else if (arrayComponentType == float.class) {
				float[] array = (float[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("F");
				Indexer.this.arrayTypeDescriptor = "[F";
				return array[idx];
			}
			else if (arrayComponentType == int.class) {
				int[] array = (int[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("I");
				Indexer.this.arrayTypeDescriptor = "[I";
				return array[idx];
			}
			else if (arrayComponentType == long.class) {
				long[] array = (long[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("J");
				Indexer.this.arrayTypeDescriptor = "[J";
				return array[idx];
			}
			else if (arrayComponentType == short.class) {
				short[] array = (short[]) ctx;
				checkAccess(array.length, idx);
				setExitTypeDescriptor("S");
				Indexer.this.arrayTypeDescriptor = "[S";
				return array[idx];
			}
			else {
				Object[] array = (Object[]) ctx;
				checkAccess(array.length, idx);
				Object retValue = array[idx];
				Indexer.this.exitTypeDescriptor = CodeFlow.toDescriptor(arrayComponentType);
				Indexer.this.arrayTypeDescriptor = CodeFlow.toDescriptor(array.getClass());
				return retValue;
			}
		}

		private void setArrayElement(TypeConverter converter, Object ctx, int idx, @Nullable Object newValue,
				Class<?> arrayComponentType) throws EvaluationException {

			if (arrayComponentType == boolean.class) {
				boolean[] array = (boolean[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, boolean.class);
			}
			else if (arrayComponentType == byte.class) {
				byte[] array = (byte[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, byte.class);
			}
			else if (arrayComponentType == char.class) {
				char[] array = (char[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, char.class);
			}
			else if (arrayComponentType == double.class) {
				double[] array = (double[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, double.class);
			}
			else if (arrayComponentType == float.class) {
				float[] array = (float[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, float.class);
			}
			else if (arrayComponentType == int.class) {
				int[] array = (int[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, int.class);
			}
			else if (arrayComponentType == long.class) {
				long[] array = (long[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, long.class);
			}
			else if (arrayComponentType == short.class) {
				short[] array = (short[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, short.class);
			}
			else {
				Object[] array = (Object[]) ctx;
				checkAccess(array.length, idx);
				array[idx] = convertValue(converter, newValue, arrayComponentType);
			}
		}

		private void checkAccess(int arrayLength, int index) throws SpelEvaluationException {
			if (index >= arrayLength) {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.ARRAY_INDEX_OUT_OF_BOUNDS,
						arrayLength, index);
			}
		}

		@SuppressWarnings("unchecked")
		private static <T> T convertValue(TypeConverter converter, @Nullable Object value, Class<T> targetType) {
			T result = (T) converter.convertValue(
					value, TypeDescriptor.forObject(value), TypeDescriptor.valueOf(targetType));
			if (result == null) {
				throw new IllegalStateException("Null conversion result for index [" + value + "]");
			}
			return result;
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
		@SuppressWarnings("NullAway")
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
						if (accessor instanceof ReflectivePropertyAccessor reflectivePropertyAccessor) {
							accessor = reflectivePropertyAccessor.createOptimalAccessor(
									this.evaluationContext, this.targetObject, this.name);
						}
						Indexer.this.cachedReadAccessor = accessor;
						Indexer.this.cachedReadName = this.name;
						Indexer.this.cachedReadTargetType = targetObjectRuntimeClass;
						if (accessor instanceof CompilablePropertyAccessor compilablePropertyAccessor) {
							setExitTypeDescriptor(CodeFlow.toDescriptor(compilablePropertyAccessor.getPropertyType()));
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
		@SuppressWarnings("NullAway")
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
			if (this.collection instanceof List list) {
				Object o = list.get(this.index);
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
			if (this.collection instanceof List list) {
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
					Constructor<?> ctor = getDefaultConstructor(elementType.getType());
					int newElements = this.index - this.collection.size();
					while (newElements >= 0) {
						// Insert a null value if the element type does not have a default constructor.
						this.collection.add(ctor != null ? ctor.newInstance() : null);
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

		@Nullable
		private static Constructor<?> getDefaultConstructor(Class<?> type) {
			try {
				return ReflectionUtils.accessibleConstructor(type);
			}
			catch (Throwable ex) {
				return null;
			}
		}
	}


	private class StringIndexingValueRef implements ValueRef {

		private final String target;

		private final int index;

		private final TypeDescriptor typeDescriptor;

		public StringIndexingValueRef(String target, int index, TypeDescriptor typeDescriptor) {
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
			return false;
		}
	}


	private class IndexAccessorValueRef implements ValueRef {

		private final IndexAccessor indexAccessor;

		private final Object target;

		private final Object index;

		private final EvaluationContext evaluationContext;

		private final TypeDescriptor typeDescriptor;


		IndexAccessorValueRef(IndexAccessor indexAccessor, Object target, Object index,
				EvaluationContext evaluationContext, TypeDescriptor typeDescriptor) {

			this.indexAccessor = indexAccessor;
			this.target = target;
			this.index = index;
			this.evaluationContext = evaluationContext;
			this.typeDescriptor = typeDescriptor;
		}


		@Override
		public TypedValue getValue() {
			try {
				return this.indexAccessor.read(this.evaluationContext, this.target, this.index);
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(
						getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_INDEX_READ, this.index,
						this.typeDescriptor.toString());
			}
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			try {
				this.indexAccessor.write(this.evaluationContext, this.target, this.index, newValue);
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(
						getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_INDEX_WRITE, this.index,
						this.typeDescriptor.toString());
			}
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

}
