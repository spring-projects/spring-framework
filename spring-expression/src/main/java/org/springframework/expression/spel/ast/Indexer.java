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
import org.springframework.expression.spel.CompilableIndexAccessor;
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
 * <li>Collections (lists, sets, etc.): the n<sup>th</sup> element</li>
 * <li>Strings: the n<sup>th</sup> character as a {@link String}</li>
 * <li>Maps: the value for the specified key</li>
 * <li>Objects: the property with the specified name</li>
 * <li>Custom Structures: via registered {@link IndexAccessor} implementations</li>
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
 * @see org.springframework.expression.IndexAccessor
 * @see org.springframework.expression.spel.CompilableIndexAccessor
 * @see org.springframework.expression.spel.support.ReflectiveIndexAccessor
 */
public class Indexer extends SpelNodeImpl {

	private enum IndexedType {ARRAY, LIST, MAP, STRING, OBJECT, CUSTOM}

	private enum AccessMode {

		READ(true, false),

		WRITE(false, true),

		READ_WRITE(true, true);

		private final boolean supportsReads;
		private final boolean supportsWrites;

		private AccessMode(boolean supportsReads, boolean supportsWrites) {
			this.supportsReads = supportsReads;
			this.supportsWrites = supportsWrites;
		}
	}


	private final boolean nullSafe;

	@Nullable
	private IndexedType indexedType;

	@Nullable
	private volatile String originalPrimitiveExitTypeDescriptor;

	@Nullable
	private volatile String arrayTypeDescriptor;

	@Nullable
	private volatile CachedPropertyState cachedPropertyReadState;

	@Nullable
	private volatile CachedPropertyState cachedPropertyWriteState;

	@Nullable
	private volatile CachedIndexState cachedIndexReadState;

	@Nullable
	private volatile CachedIndexState cachedIndexWriteState;


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
		return getValueRef(state, AccessMode.READ).getValue();
	}

	@Override
	public TypedValue setValueInternal(ExpressionState state, Supplier<TypedValue> valueSupplier)
			throws EvaluationException {

		TypedValue typedValue = valueSupplier.get();
		getValueRef(state, AccessMode.WRITE).setValue(typedValue.getValue());
		return typedValue;
	}

	@Override
	public boolean isWritable(ExpressionState expressionState) throws SpelEvaluationException {
		return getValueRef(expressionState, AccessMode.WRITE).isWritable();
	}

	@Override
	protected ValueRef getValueRef(ExpressionState state) throws EvaluationException {
		return getValueRef(state, AccessMode.READ_WRITE);
	}

	private ValueRef getValueRef(ExpressionState state, AccessMode accessMode) throws EvaluationException {
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

		// Indexing into an array
		if (target.getClass().isArray()) {
			int intIndex = convertIndexToInt(state, index);
			this.indexedType = IndexedType.ARRAY;
			return new ArrayIndexingValueRef(state.getTypeConverter(), target, intIndex, targetDescriptor);
		}

		// Indexing into a List
		if (target instanceof List<?> list) {
			int intIndex = convertIndexToInt(state, index);
			this.indexedType = IndexedType.LIST;
			return new CollectionIndexingValueRef(list, intIndex, targetDescriptor,
					state.getTypeConverter(), state.getConfiguration().isAutoGrowCollections(),
					state.getConfiguration().getMaximumAutoGrowSize());
		}

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

		// Indexing into a String
		if (target instanceof String string) {
			int intIndex = convertIndexToInt(state, index);
			this.indexedType = IndexedType.STRING;
			return new StringIndexingValueRef(string, intIndex, targetDescriptor);
		}

		// Check for a custom IndexAccessor.
		EvaluationContext evalContext = state.getEvaluationContext();
		List<IndexAccessor> accessorsToTry =
				AstUtils.getAccessorsToTry(target, evalContext.getIndexAccessors());
		if (accessMode.supportsReads) {
			try {
				for (IndexAccessor indexAccessor : accessorsToTry) {
					if (indexAccessor.canRead(evalContext, target, index)) {
						this.indexedType = IndexedType.CUSTOM;
						return new IndexAccessorValueRef(target, index, evalContext, targetDescriptor);
					}
				}
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(
						getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_INDEX_READ,
						index, target.getClass().getTypeName());
			}
		}
		if (accessMode.supportsWrites) {
			try {
				for (IndexAccessor indexAccessor : accessorsToTry) {
					if (indexAccessor.canWrite(evalContext, target, index)) {
						this.indexedType = IndexedType.CUSTOM;
						return new IndexAccessorValueRef(target, index, evalContext, targetDescriptor);
					}
				}
			}
			catch (Exception ex) {
				throw new SpelEvaluationException(
						getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_INDEX_WRITE,
						index, target.getClass().getTypeName());
			}
		}

		// Fallback indexing support for collections
		if (target instanceof Collection<?> collection) {
			int intIndex = convertIndexToInt(state, index);
			return new CollectionIndexingValueRef(collection, intIndex, targetDescriptor,
					state.getTypeConverter(), state.getConfiguration().isAutoGrowCollections(),
					state.getConfiguration().getMaximumAutoGrowSize());
		}

		// As a last resort, try to treat the index value as a property of the context object.
		TypeDescriptor valueType = indexValue.getTypeDescriptor();
		if (valueType != null && String.class == valueType.getType()) {
			this.indexedType = IndexedType.OBJECT;
			return new PropertyAccessorValueRef(
					target, (String) index, state.getEvaluationContext(), targetDescriptor);
		}

		throw new SpelEvaluationException(
				getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, targetDescriptor);
	}

	@Override
	public boolean isCompilable() {
		if (this.exitTypeDescriptor == null) {
			return false;
		}
		if (this.indexedType == IndexedType.ARRAY) {
			return (this.arrayTypeDescriptor != null);
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
			CachedPropertyState cachedPropertyReadState = this.cachedPropertyReadState;
			return (index instanceof StringLiteral && cachedPropertyReadState != null &&
					cachedPropertyReadState.accessor instanceof CompilablePropertyAccessor cpa &&
					cpa.isCompilable());
		}
		else if (this.indexedType == IndexedType.CUSTOM) {
			CachedIndexState cachedIndexReadState = this.cachedIndexReadState;
			return (cachedIndexReadState != null &&
					cachedIndexReadState.accessor instanceof CompilableIndexAccessor cia &&
					cia.isCompilable() && index.isCompilable());
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

			generateIndexCode(mv, cf, index, int.class);
			mv.visitInsn(insn);
		}

		else if (this.indexedType == IndexedType.LIST) {
			mv.visitTypeInsn(CHECKCAST, "java/util/List");
			generateIndexCode(mv, cf, index, int.class);
			mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);
		}

		else if (this.indexedType == IndexedType.MAP) {
			mv.visitTypeInsn(CHECKCAST, "java/util/Map");
			// Special case when the key is an unquoted string literal that will be parsed as
			// a property/field reference
			if (index instanceof PropertyOrFieldReference reference) {
				String mapKeyName = reference.getName();
				mv.visitLdcInsn(mapKeyName);
			}
			else {
				generateIndexCode(mv, cf, index, Object.class);
			}
			mv.visitMethodInsn(
					INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
		}

		else if (this.indexedType == IndexedType.OBJECT) {
			if (!(index instanceof StringLiteral stringLiteral)) {
				throw new IllegalStateException(
						"Index expression must be a StringLiteral, but was: " + index.getClass().getName());
			}

			CachedPropertyState cachedPropertyReadState = this.cachedPropertyReadState;
			Assert.state(cachedPropertyReadState != null, "No cached PropertyAccessor for reading");
			if (!(cachedPropertyReadState.accessor instanceof CompilablePropertyAccessor compilablePropertyAccessor)) {
				throw new IllegalStateException(
						"Cached PropertyAccessor must be a CompilablePropertyAccessor, but was: " +
							cachedPropertyReadState.accessor.getClass().getName());
			}
			String propertyName = (String) stringLiteral.getLiteralValue().getValue();
			Assert.state(propertyName != null, "No property name");
			compilablePropertyAccessor.generateCode(propertyName, mv, cf);
		}

		else if (this.indexedType == IndexedType.CUSTOM) {
			CachedIndexState cachedIndexReadState = this.cachedIndexReadState;
			Assert.state(cachedIndexReadState != null, "No cached IndexAccessor for reading");
			if (!(cachedIndexReadState.accessor instanceof CompilableIndexAccessor compilableIndexAccessor)) {
				throw new IllegalStateException(
						"Cached IndexAccessor must be a CompilableIndexAccessor, but was: " +
							cachedIndexReadState.accessor.getClass().getName());
			}
			compilableIndexAccessor.generateCode(index, mv, cf);
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

	private void generateIndexCode(MethodVisitor mv, CodeFlow cf, SpelNodeImpl indexNode, Class<?> indexType) {
		cf.generateCodeForArgument(mv, indexNode, indexType);
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

	private static int convertIndexToInt(ExpressionState state, Object index) {
		return (Integer) state.convertValue(index, TypeDescriptor.valueOf(Integer.class));
	}

	private static Class<?> getObjectType(Object obj) {
		return (obj instanceof Class<?> clazz ? clazz : obj.getClass());
	}


	/**
	 * Tracks state when the {@code Indexer} is being used as a {@link PropertyAccessor}.
	 *
	 * <p>If the current target type and property name match these values, the
	 * cached {@code PropertyAccessor} is used to access the property.
	 *
	 * <p>If they do not match, a suitable {@code PropertyAccessor} is discovered
	 * and cached for later use.
	 */
	private record CachedPropertyState(PropertyAccessor accessor, Class<?> targetType, String name) {
	}

	/**
	 * Tracks state when the {@code Indexer} is being used as an {@link IndexAccessor}.
	 *
	 * <p>If the current target type and index match these values, the cached
	 * {@code IndexAccessor} is used to access the index.
	 *
	 * @param accessor the cached {@code IndexAccessor}
	 * @param targetType the target type on which the index is being accessed
	 * @param index the index value: the value inside the square brackets, such
	 * as the Integer 0 in [0], the String "name" in ['name'], etc.
	 */
	private record CachedIndexState(IndexAccessor accessor, Class<?> targetType, Object index) {
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


	private class PropertyAccessorValueRef implements ValueRef {

		private final Object targetObject;

		private final String name;

		private final EvaluationContext evaluationContext;

		private final TypeDescriptor targetObjectTypeDescriptor;

		public PropertyAccessorValueRef(Object targetObject, String name,
				EvaluationContext evaluationContext, TypeDescriptor targetObjectTypeDescriptor) {

			this.targetObject = targetObject;
			this.name = name;
			this.evaluationContext = evaluationContext;
			this.targetObjectTypeDescriptor = targetObjectTypeDescriptor;
		}

		@Override
		public TypedValue getValue() {
			Class<?> targetType = getObjectType(this.targetObject);
			try {
				CachedPropertyState cachedPropertyReadState = Indexer.this.cachedPropertyReadState;
				if (cachedPropertyReadState != null) {
					String cachedPropertyName = cachedPropertyReadState.name;
					Class<?> cachedTargetType = cachedPropertyReadState.targetType;
					// Is it OK to use the cached accessor?
					if (cachedPropertyName.equals(this.name) && cachedTargetType.equals(targetType)) {
						PropertyAccessor accessor = cachedPropertyReadState.accessor;
						return accessor.read(this.evaluationContext, this.targetObject, this.name);
					}
					// If the above code block did not use a cached accessor and return a value,
					// we need to reset our cached state.
					Indexer.this.cachedPropertyReadState = null;
				}
				List<PropertyAccessor> accessorsToTry =
						AstUtils.getAccessorsToTry(targetType, this.evaluationContext.getPropertyAccessors());
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canRead(this.evaluationContext, this.targetObject, this.name)) {
						if (accessor instanceof ReflectivePropertyAccessor reflectivePropertyAccessor) {
							accessor = reflectivePropertyAccessor.createOptimalAccessor(
									this.evaluationContext, this.targetObject, this.name);
						}
						TypedValue result = accessor.read(this.evaluationContext, this.targetObject, this.name);
						Indexer.this.cachedPropertyReadState = new CachedPropertyState(accessor, targetType, this.name);
						if (accessor instanceof CompilablePropertyAccessor compilablePropertyAccessor) {
							setExitTypeDescriptor(CodeFlow.toDescriptor(compilablePropertyAccessor.getPropertyType()));
						}
						return result;
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
			Class<?> targetType = getObjectType(this.targetObject);
			try {
				CachedPropertyState cachedPropertyWriteState = Indexer.this.cachedPropertyWriteState;
				if (cachedPropertyWriteState != null) {
					String cachedPropertyName = cachedPropertyWriteState.name;
					Class<?> cachedTargetType = cachedPropertyWriteState.targetType;
					// Is it OK to use the cached accessor?
					if (cachedPropertyName.equals(this.name) && cachedTargetType.equals(targetType)) {
						PropertyAccessor accessor = cachedPropertyWriteState.accessor;
						accessor.write(this.evaluationContext, this.targetObject, this.name, newValue);
						return;
					}
					// If the above code block did not use a cached accessor and return,
					// we need to reset our cached state.
					Indexer.this.cachedPropertyWriteState = null;
				}
				List<PropertyAccessor> accessorsToTry =
						AstUtils.getAccessorsToTry(targetType, this.evaluationContext.getPropertyAccessors());
				for (PropertyAccessor accessor : accessorsToTry) {
					if (accessor.canWrite(this.evaluationContext, this.targetObject, this.name)) {
						accessor.write(this.evaluationContext, this.targetObject, this.name, newValue);
						Indexer.this.cachedPropertyWriteState = new CachedPropertyState(accessor, targetType, this.name);
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
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.COLLECTION_INDEX_OUT_OF_BOUNDS,
					this.collection.size(), this.index);
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			if (!(this.collection instanceof List list)) {
				throw new SpelEvaluationException(getStartPosition(), SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE,
						this.collectionEntryDescriptor.toString());
			}

			growCollectionIfNecessary();
			if (this.collectionEntryDescriptor.getElementTypeDescriptor() != null) {
				newValue = this.typeConverter.convertValue(newValue, TypeDescriptor.forObject(newValue),
						this.collectionEntryDescriptor.getElementTypeDescriptor());
			}
			list.set(this.index, newValue);
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
			return (this.collection instanceof List);
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

		private final Object target;

		private final Object index;

		private final EvaluationContext evaluationContext;

		private final TypeDescriptor typeDescriptor;


		IndexAccessorValueRef(Object target, Object index, EvaluationContext evaluationContext,
				TypeDescriptor typeDescriptor) {

			this.target = target;
			this.index = index;
			this.evaluationContext = evaluationContext;
			this.typeDescriptor = typeDescriptor;
		}


		@Override
		public TypedValue getValue() {
			Class<?> targetType = getObjectType(this.target);
			Exception exception = null;
			try {
				CachedIndexState cachedIndexReadState = Indexer.this.cachedIndexReadState;
				if (cachedIndexReadState != null) {
					Object cachedIndex = cachedIndexReadState.index;
					Class<?> cachedTargetType = cachedIndexReadState.targetType;
					// Is it OK to use the cached IndexAccessor?
					if (cachedIndex.equals(this.index) && cachedTargetType.equals(targetType)) {
						IndexAccessor accessor = cachedIndexReadState.accessor;
						if (this.evaluationContext.getIndexAccessors().contains(accessor)) {
							try {
								return accessor.read(this.evaluationContext, this.target, this.index);
							}
							catch (Exception ex) {
								// This is OK: it may have gone stale due to a class change.
								// So, we track the exception and try to find a new accessor
								// before giving up...
								exception = ex;
							}
						}
					}
					// If the above code block did not use a cached accessor and return a value,
					// we need to reset our cached state.
					Indexer.this.cachedIndexReadState = null;
				}
				List<IndexAccessor> accessorsToTry =
						AstUtils.getAccessorsToTry(this.target, this.evaluationContext.getIndexAccessors());
				for (IndexAccessor indexAccessor : accessorsToTry) {
					if (indexAccessor.canRead(this.evaluationContext, this.target, this.index)) {
						TypedValue result = indexAccessor.read(this.evaluationContext, this.target, this.index);
						Indexer.this.cachedIndexReadState = new CachedIndexState(indexAccessor, targetType, this.index);
						if (indexAccessor instanceof CompilableIndexAccessor compilableIndexAccessor) {
							setExitTypeDescriptor(CodeFlow.toDescriptor(compilableIndexAccessor.getIndexedValueType()));
						}
						return result;
					}
				}
			}
			catch (Exception ex) {
				exception = ex;
			}

			if (exception != null) {
				throw new SpelEvaluationException(
						getStartPosition(), exception, SpelMessage.EXCEPTION_DURING_INDEX_READ, this.index,
						this.typeDescriptor.toString());
			}
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, this.typeDescriptor.toString());
		}

		@Override
		public void setValue(@Nullable Object newValue) {
			Class<?> targetType = getObjectType(this.target);
			Exception exception = null;
			try {
				CachedIndexState cachedIndexWriteState = Indexer.this.cachedIndexWriteState;
				if (cachedIndexWriteState != null) {
					Object cachedIndex = cachedIndexWriteState.index;
					Class<?> cachedTargetType = cachedIndexWriteState.targetType;
					// Is it OK to use the cached IndexAccessor?
					if (cachedIndex.equals(this.index) && cachedTargetType.equals(targetType)) {
						IndexAccessor accessor = cachedIndexWriteState.accessor;
						if (this.evaluationContext.getIndexAccessors().contains(accessor)) {
							try {
								accessor.write(this.evaluationContext, this.target, this.index, newValue);
								return;
							}
							catch (Exception ex) {
								// This is OK: it may have gone stale due to a class change.
								// So, we track the exception and try to find a new accessor
								// before giving up...
								exception = ex;
							}
						}
					}
					// If the above code block did not use a cached accessor and return,
					// we need to reset our cached state.
					Indexer.this.cachedIndexWriteState = null;
				}
				List<IndexAccessor> accessorsToTry =
						AstUtils.getAccessorsToTry(this.target, this.evaluationContext.getIndexAccessors());
				for (IndexAccessor indexAccessor : accessorsToTry) {
					if (indexAccessor.canWrite(this.evaluationContext, this.target, this.index)) {
						indexAccessor.write(this.evaluationContext, this.target, this.index, newValue);
						Indexer.this.cachedIndexWriteState = new CachedIndexState(indexAccessor, targetType, this.index);
						return;
					}
				}
			}
			catch (Exception ex) {
				exception = ex;
			}

			if (exception != null) {
				throw new SpelEvaluationException(
						getStartPosition(), exception, SpelMessage.EXCEPTION_DURING_INDEX_WRITE, this.index,
						this.typeDescriptor.toString());
			}
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.INDEXING_NOT_SUPPORTED_FOR_TYPE, this.typeDescriptor.toString());
		}

		@Override
		public boolean isWritable() {
			return true;
		}
	}

}
