/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.ConstructorExecutor;
import org.springframework.expression.ConstructorResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.ExpressionUtils;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.support.ReflectiveConstructorExecutor;
import org.springframework.util.Assert;

/**
 * Represents the invocation of a constructor: either a constructor on a regular type or
 * construction of an array. When an array is constructed, an initializer can be specified.
 *
 * <h4>Examples</h4>
 * <ul>
 * <li><code>new example.Foo()</code></li>
 * <li><code>new String('hello world')</code></li>
 * <li><code>new int[] {1,2,3,4}</code></li>
 * <li><code>new String[] {'abc','xyz'}</code></li>
 * <li><code>new int[5]</code></li>
 * <li><code>new int[3][4]</code></li>
 * </ul>
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Harry Yang
 * @since 3.0
 */
public class ConstructorReference extends SpelNodeImpl {

	/**
	 * Maximum number of elements permitted in an array declaration, applying
	 * to one-dimensional as well as multi-dimensional arrays.
	 * @since 5.3.17
	 */
	private static final int MAX_ARRAY_ELEMENTS = 256 * 1024; // 256K

	private final boolean isArrayConstructor;

	private final SpelNodeImpl @Nullable [] dimensions;

	/** The cached executor that may be reused on subsequent evaluations. */
	private volatile @Nullable ConstructorExecutor cachedExecutor;


	/**
	 * Create a constructor reference for a regular type.
	 * <p>The first argument is the type. The rest are the arguments to the
	 * constructor.
	 */
	public ConstructorReference(int startPos, int endPos, SpelNodeImpl... arguments) {
		super(startPos, endPos, arguments);
		this.isArrayConstructor = false;
		this.dimensions = null;
	}

	/**
	 * Create a constructor reference for an array.
	 * <p>The first argument is the array component type. The second argument is
	 * an {@link InlineList} representing the array initializer, if an initializer
	 * was supplied in the expression.
	 */
	public ConstructorReference(int startPos, int endPos, SpelNodeImpl[] dimensions, SpelNodeImpl... arguments) {
		super(startPos, endPos, arguments);
		this.isArrayConstructor = true;
		this.dimensions = dimensions;
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

	/**
	 * Create a new ordinary object and return it.
	 * @param state the expression state within which this expression is being evaluated
	 * @return the new object
	 * @throws EvaluationException if there is a problem creating the object
	 */
	private TypedValue createNewInstance(ExpressionState state) throws EvaluationException {
		@Nullable Object[] arguments = new Object[getChildCount() - 1];
		List<TypeDescriptor> argumentTypes = new ArrayList<>(getChildCount() - 1);
		for (int i = 0; i < arguments.length; i++) {
			TypedValue childValue = this.children[i + 1].getValueInternal(state);
			Object value = childValue.getValue();
			arguments[i] = value;
			argumentTypes.add(TypeDescriptor.forObject(value));
		}

		ConstructorExecutor executorToUse = this.cachedExecutor;
		if (executorToUse != null) {
			try {
				return executorToUse.execute(state.getEvaluationContext(), arguments);
			}
			catch (AccessException ex) {
				// Two reasons this can occur:
				// 1. the constructor invoked actually threw a real exception
				// 2. the constructor invoked was not passed the arguments it expected and has become 'stale'

				// In the first case we should not retry, in the second case we should see if there is a
				// better suited constructor.

				// To determine which situation it is, the AccessException will contain a cause.
				// If the cause is an InvocationTargetException, a user exception was thrown inside the constructor.
				// Otherwise, the constructor could not be invoked.
				if (ex.getCause() instanceof InvocationTargetException cause) {
					// User exception was the root cause - exit now
					Throwable rootCause = cause.getCause();
					if (rootCause instanceof RuntimeException runtimeException) {
						throw runtimeException;
					}
					else {
						String typeName = (String) this.children[0].getValueInternal(state).getValue();
						throw new SpelEvaluationException(getStartPosition(), rootCause,
								SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
								FormatHelper.formatMethodForMessage("", argumentTypes));
					}
				}

				// At this point we know it wasn't a user problem so worth a retry if a better candidate can be found
				this.cachedExecutor = null;
			}
		}

		// Either there was no ConstructorExecutor or it no longer exists
		String typeName = (String) this.children[0].getValueInternal(state).getValue();
		Assert.state(typeName != null, "No type name");
		executorToUse = findExecutorForConstructor(typeName, argumentTypes, state);
		try {
			this.cachedExecutor = executorToUse;
			if (executorToUse instanceof ReflectiveConstructorExecutor reflectiveConstructorExecutor) {
				this.exitTypeDescriptor = CodeFlow.toDescriptor(
						reflectiveConstructorExecutor.getConstructor().getDeclaringClass());

			}
			return executorToUse.execute(state.getEvaluationContext(), arguments);
		}
		catch (AccessException ex) {
			throw new SpelEvaluationException(getStartPosition(), ex,
					SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
					FormatHelper.formatMethodForMessage("", argumentTypes));
		}
	}

	/**
	 * Go through the list of registered constructor resolvers and see if any can find a
	 * constructor that takes the specified set of arguments.
	 * @param typeName the type trying to be constructed
	 * @param argumentTypes the types of the arguments supplied that the constructor must take
	 * @param state the current state of the expression
	 * @return a reusable ConstructorExecutor that can be invoked to run the constructor or null
	 * @throws SpelEvaluationException if there is a problem locating the constructor
	 */
	private ConstructorExecutor findExecutorForConstructor(String typeName,
			List<TypeDescriptor> argumentTypes, ExpressionState state) throws SpelEvaluationException {

		EvaluationContext evalContext = state.getEvaluationContext();
		List<ConstructorResolver> ctorResolvers = evalContext.getConstructorResolvers();
		for (ConstructorResolver ctorResolver : ctorResolvers) {
			try {
				ConstructorExecutor ce = ctorResolver.resolve(state.getEvaluationContext(), typeName, argumentTypes);
				if (ce != null) {
					return ce;
				}
			}
			catch (AccessException ex) {
				throw new SpelEvaluationException(getStartPosition(), ex,
						SpelMessage.CONSTRUCTOR_INVOCATION_PROBLEM, typeName,
						FormatHelper.formatMethodForMessage("", argumentTypes));
			}
		}
		throw new SpelEvaluationException(getStartPosition(), SpelMessage.CONSTRUCTOR_NOT_FOUND, typeName,
				FormatHelper.formatMethodForMessage("", argumentTypes));
	}

	@Override
	public String toStringAST() {
		StringBuilder sb = new StringBuilder("new ");
		sb.append(getChild(0).toStringAST()); // constructor or array type

		// Arrays
		if (this.isArrayConstructor) {
			if (hasInitializer()) {
				// new int[] {1, 2, 3, 4, 5}, etc.
				InlineList initializer = (InlineList) getChild(1);
				sb.append("[] ").append(initializer.toStringAST());
			}
			else if (this.dimensions != null) {
				// new int[3], new java.lang.String[3][4], etc.
				for (SpelNodeImpl dimension : this.dimensions) {
					sb.append('[').append(dimension.toStringAST()).append(']');
				}
			}
		}
		// Constructors
		else {
			// new String('hello'), new org.example.Person('Jane', 32), etc.
			StringJoiner sj = new StringJoiner(",", "(", ")");
			int count = getChildCount();
			for (int i = 1; i < count; i++) {
				sj.add(getChild(i).toStringAST());
			}
			sb.append(sj.toString());
		}

		return sb.toString();
	}

	/**
	 * Create an array and return it.
	 * @param state the expression state within which this expression is being evaluated
	 * @return the new array
	 * @throws EvaluationException if there is a problem creating the array
	 */
	private TypedValue createArray(ExpressionState state) throws EvaluationException {
		// First child gives us the array type which will either be a primitive or reference type
		Object intendedArrayType = getChild(0).getValue(state);
		if (!(intendedArrayType instanceof String type)) {
			throw new SpelEvaluationException(getChild(0).getStartPosition(),
					SpelMessage.TYPE_NAME_EXPECTED_FOR_ARRAY_CONSTRUCTION,
					FormatHelper.formatClassNameForMessage(
							intendedArrayType != null ? intendedArrayType.getClass() : null));
		}

		if (state.getEvaluationContext().getConstructorResolvers().isEmpty()) {
			// No constructor resolver -> no array construction either (as of 6.0)
			throw new SpelEvaluationException(getStartPosition(), SpelMessage.CONSTRUCTOR_NOT_FOUND,
					type + "[]", "[]");
		}

		Class<?> componentType;
		TypeCode arrayTypeCode = TypeCode.forName(type);
		if (arrayTypeCode == TypeCode.OBJECT) {
			componentType = state.findType(type);
		}
		else {
			componentType = arrayTypeCode.getType();
		}

		Object newArray = null;
		if (!hasInitializer()) {
			// Confirm all dimensions were specified (for example [3][][5] is missing the 2nd dimension)
			if (this.dimensions != null) {
				for (SpelNodeImpl dimension : this.dimensions) {
					if (dimension == null) {
						throw new SpelEvaluationException(getStartPosition(), SpelMessage.MISSING_ARRAY_DIMENSION);
					}
				}
				TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
				if (this.dimensions.length == 1) {
					// Shortcut for 1-dimensional
					TypedValue o = this.dimensions[0].getTypedValue(state);
					int arraySize = ExpressionUtils.toInt(typeConverter, o);
					checkNumElements(arraySize);
					newArray = Array.newInstance(componentType, arraySize);
				}
				else {
					// Multidimensional - hold onto your hat!
					int[] dims = new int[this.dimensions.length];
					long numElements = 1;
					for (int d = 0; d < this.dimensions.length; d++) {
						TypedValue o = this.dimensions[d].getTypedValue(state);
						int arraySize = ExpressionUtils.toInt(typeConverter, o);
						dims[d] = arraySize;
						numElements *= arraySize;
						checkNumElements(numElements);
					}
					newArray = Array.newInstance(componentType, dims);
				}
			}
		}
		else {
			// There is an initializer
			if (this.dimensions == null || this.dimensions.length > 1) {
				// There is an initializer, but this is a multidimensional array
				// (for example, new int[][]{{1,2},{3,4}}), which is not supported.
				throw new SpelEvaluationException(getStartPosition(),
						SpelMessage.MULTIDIM_ARRAY_INITIALIZER_NOT_SUPPORTED);
			}
			TypeConverter typeConverter = state.getEvaluationContext().getTypeConverter();
			InlineList initializer = (InlineList) getChild(1);
			// If a dimension was specified, check it matches the initializer length
			if (this.dimensions[0] != null) {
				TypedValue dValue = this.dimensions[0].getTypedValue(state);
				int i = ExpressionUtils.toInt(typeConverter, dValue);
				if (i != initializer.getChildCount()) {
					throw new SpelEvaluationException(getStartPosition(), SpelMessage.INITIALIZER_LENGTH_INCORRECT);
				}
			}
			newArray = switch (arrayTypeCode) {
				case OBJECT -> createReferenceTypeArray(state, typeConverter, initializer.children, componentType);
				case BOOLEAN -> createBooleanArray(state, typeConverter, initializer.children);
				case CHAR -> createCharArray(state, typeConverter, initializer.children);
				case BYTE -> createByteArray(state, typeConverter, initializer.children);
				case SHORT -> createShortArray(state, typeConverter, initializer.children);
				case INT -> createIntArray(state, typeConverter, initializer.children);
				case LONG -> createLongArray(state, typeConverter, initializer.children);
				case FLOAT -> createFloatArray(state, typeConverter, initializer.children);
				case DOUBLE -> createDoubleArray(state, typeConverter, initializer.children);
				default -> throw new IllegalStateException("Unsupported TypeCode: " + arrayTypeCode);
			};
		}
		return new TypedValue(newArray);
	}

	private void checkNumElements(long numElements) {
		if (numElements >= MAX_ARRAY_ELEMENTS) {
			throw new SpelEvaluationException(getStartPosition(),
					SpelMessage.MAX_ARRAY_ELEMENTS_THRESHOLD_EXCEEDED, MAX_ARRAY_ELEMENTS);
		}
	}

	private Object createReferenceTypeArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children,
			Class<?> componentType) {

		@Nullable Object[] array = (Object[]) Array.newInstance(componentType, children.length);
		TypeDescriptor targetType = TypeDescriptor.valueOf(componentType);
		for (int i = 0; i < array.length; i++) {
			Object value = children[i].getValue(state);
			array[i] = typeConverter.convertValue(value, TypeDescriptor.forObject(value), targetType);
		}
		return array;
	}

	private boolean[] createBooleanArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children) {
		boolean[] array = new boolean[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toBoolean(typeConverter, typedValue);
		}
		return array;
	}

	private char[] createCharArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children) {
		char[] array = new char[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toChar(typeConverter, typedValue);
		}
		return array;
	}

	private byte[] createByteArray(ExpressionState state, TypeConverter converter, SpelNodeImpl[] children) {
		byte[] array = new byte[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toByte(converter, typedValue);
		}
		return array;
	}

	private short[] createShortArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children) {
		short[] array = new short[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toShort(typeConverter, typedValue);
		}
		return array;
	}

	private int[] createIntArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children) {
		int[] array = new int[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toInt(typeConverter, typedValue);
		}
		return array;
	}

	private long[] createLongArray(ExpressionState state, TypeConverter converter, SpelNodeImpl[] children) {
		long[] array = new long[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toLong(converter, typedValue);
		}
		return array;
	}

	private float[] createFloatArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children) {
		float[] array = new float[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toFloat(typeConverter, typedValue);
		}
		return array;
	}

	private double[] createDoubleArray(ExpressionState state, TypeConverter typeConverter, SpelNodeImpl[] children) {
		double[] array = new double[children.length];
		for (int i = 0; i < array.length; i++) {
			TypedValue typedValue = children[i].getTypedValue(state);
			array[i] = ExpressionUtils.toDouble(typeConverter, typedValue);
		}
		return array;
	}

	private boolean hasInitializer() {
		return (getChildCount() > 1);
	}

	@Override
	public boolean isCompilable() {
		if (!(this.cachedExecutor instanceof ReflectiveConstructorExecutor executor) ||
			this.exitTypeDescriptor == null) {
			return false;
		}

		for (int i = 1; i < this.children.length; i++) {
			if (!this.children[i].isCompilable()) {
				return false;
			}
		}

		Constructor<?> constructor = executor.getConstructor();
		return (Modifier.isPublic(constructor.getModifiers()) &&
				Modifier.isPublic(constructor.getDeclaringClass().getModifiers()));
	}

	@Override
	public void generateCode(MethodVisitor mv, CodeFlow cf) {
		ReflectiveConstructorExecutor executor = ((ReflectiveConstructorExecutor) this.cachedExecutor);
		Assert.state(executor != null, "No cached executor");

		Constructor<?> constructor = executor.getConstructor();
		String classDesc = constructor.getDeclaringClass().getName().replace('.', '/');
		mv.visitTypeInsn(NEW, classDesc);
		mv.visitInsn(DUP);

		// children[0] is the type of the constructor, don't want to include that in argument processing
		SpelNodeImpl[] arguments = new SpelNodeImpl[this.children.length - 1];
		System.arraycopy(this.children, 1, arguments, 0, this.children.length - 1);
		generateCodeForArguments(mv, cf, constructor, arguments);
		mv.visitMethodInsn(INVOKESPECIAL, classDesc, "<init>", CodeFlow.createSignatureDescriptor(constructor), false);
		cf.pushDescriptor(this.exitTypeDescriptor);
	}

}
