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

package org.springframework.expression.spel.support;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MethodInvoker;

/**
 * Utility methods used by the reflection resolver code to discover the appropriate
 * methods, constructors, and fields that should be used in expressions.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
public abstract class ReflectionHelper {

	/**
	 * Compare argument arrays and return information about whether they match.
	 * <p>The supplied type converter allows for matches to take into account that a type
	 * may be transformed into a different type by the converter.
	 * @param expectedArgTypes the types the method/constructor is expecting
	 * @param suppliedArgTypes the types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @return an {@code ArgumentsMatchInfo} object indicating what kind of match it was,
	 * or {@code null} if it was not a match
	 */
	@Nullable
	static ArgumentsMatchKind compareArguments(
			List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(expectedArgTypes.size() == suppliedArgTypes.size(),
				"Expected argument types and supplied argument types should be lists of the same size");

		ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;
		for (int i = 0; i < expectedArgTypes.size() && match != null; i++) {
			TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
			TypeDescriptor expectedArg = expectedArgTypes.get(i);
			// The user may supply null, and that will be OK unless a primitive is expected.
			if (suppliedArg == null) {
				if (expectedArg.isPrimitive()) {
					match = null;
				}
			}
			else if (!expectedArg.equals(suppliedArg)) {
				if (suppliedArg.isAssignableTo(expectedArg)) {
					if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
						match = ArgumentsMatchKind.CLOSE;
					}
				}
				else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
					match = ArgumentsMatchKind.REQUIRES_CONVERSION;
				}
				else {
					match = null;
				}
			}
		}
		return match;
	}

	/**
	 * Based on {@link MethodInvoker#getTypeDifferenceWeight(Class[], Object[])} but operates on TypeDescriptors.
	 */
	public static int getTypeDifferenceWeight(List<TypeDescriptor> paramTypes, List<TypeDescriptor> argTypes) {
		int result = 0;
		for (int i = 0; i < paramTypes.size(); i++) {
			TypeDescriptor paramType = paramTypes.get(i);
			TypeDescriptor argType = (i < argTypes.size() ? argTypes.get(i) : null);
			if (argType == null) {
				if (paramType.isPrimitive()) {
					return Integer.MAX_VALUE;
				}
			}
			else {
				Class<?> paramTypeClazz = paramType.getType();
				if (!ClassUtils.isAssignable(paramTypeClazz, argType.getType())) {
					return Integer.MAX_VALUE;
				}
				if (paramTypeClazz.isPrimitive()) {
					paramTypeClazz = Object.class;
				}
				Class<?> superClass = argType.getType().getSuperclass();
				while (superClass != null) {
					if (paramTypeClazz.equals(superClass)) {
						result = result + 2;
						superClass = null;
					}
					else if (ClassUtils.isAssignable(paramTypeClazz, superClass)) {
						result = result + 2;
						superClass = superClass.getSuperclass();
					}
					else {
						superClass = null;
					}
				}
				if (paramTypeClazz.isInterface()) {
					result = result + 1;
				}
			}
		}
		return result;
	}

	/**
	 * Compare argument arrays and return information about whether they match.
	 * <p>The supplied type converter allows for matches to take into account that a type
	 * may be transformed into a different type by the converter.
	 * <p>This variant of {@link #compareArguments(List, List, TypeConverter)} also allows
	 * for a varargs match.
	 * @param expectedArgTypes the types the method/constructor is expecting
	 * @param suppliedArgTypes the types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @return an {@code ArgumentsMatchKind} object indicating what kind of match it was,
	 * or {@code null} if it was not a match
	 */
	@Nullable
	static ArgumentsMatchKind compareArgumentsVarargs(
			List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(!CollectionUtils.isEmpty(expectedArgTypes),
				"Expected arguments must at least include one array (the varargs parameter)");
		Assert.isTrue(expectedArgTypes.get(expectedArgTypes.size() - 1).isArray(),
				"Final expected argument should be array type (the varargs parameter)");

		ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;

		// Check up until the varargs argument:

		// Deal with the arguments up to 'expected number' - 1 (that is everything but the varargs argument)
		int argCountUpToVarargs = expectedArgTypes.size() - 1;
		for (int i = 0; i < argCountUpToVarargs && match != null; i++) {
			TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
			TypeDescriptor expectedArg = expectedArgTypes.get(i);
			if (suppliedArg == null) {
				if (expectedArg.isPrimitive()) {
					match = null;
				}
			}
			else {
				if (!expectedArg.equals(suppliedArg)) {
					if (suppliedArg.isAssignableTo(expectedArg)) {
						if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
							match = ArgumentsMatchKind.CLOSE;
						}
					}
					else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
						match = ArgumentsMatchKind.REQUIRES_CONVERSION;
					}
					else {
						match = null;
					}
				}
			}
		}

		// If already confirmed it cannot be a match, then return
		if (match == null) {
			return null;
		}

		if (suppliedArgTypes.size() == expectedArgTypes.size() &&
				expectedArgTypes.get(expectedArgTypes.size() - 1).equals(
						suppliedArgTypes.get(suppliedArgTypes.size() - 1))) {
			// Special case: there is one parameter left and it is an array and it matches the varargs
			// expected argument - that is a match, the caller has already built the array. Proceed with it.
		}
		else {
			// Now... we have the final argument in the method we are checking as a match and we have 0
			// or more other arguments left to pass to it.
			TypeDescriptor varargsDesc = expectedArgTypes.get(expectedArgTypes.size() - 1);
			TypeDescriptor componentTypeDesc = varargsDesc.getElementTypeDescriptor();
			Assert.state(componentTypeDesc != null, "Component type must not be null for a varargs array");
			Class<?> varargsComponentType = componentTypeDesc.getType();

			// All remaining parameters must be of this type or convertible to this type
			for (int i = expectedArgTypes.size() - 1; i < suppliedArgTypes.size(); i++) {
				TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
				if (suppliedArg == null) {
					if (varargsComponentType.isPrimitive()) {
						match = null;
					}
				}
				else {
					if (varargsComponentType != suppliedArg.getType()) {
						if (ClassUtils.isAssignable(varargsComponentType, suppliedArg.getType())) {
							if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
								match = ArgumentsMatchKind.CLOSE;
							}
						}
						else if (typeConverter.canConvert(suppliedArg, TypeDescriptor.valueOf(varargsComponentType))) {
							match = ArgumentsMatchKind.REQUIRES_CONVERSION;
						}
						else {
							match = null;
						}
					}
				}
			}
		}

		return match;
	}

	/**
	 * Convert the supplied set of arguments into the parameter types of the supplied
	 * {@link Method}.
	 * <p>If the supplied method is a varargs method, the final parameter type must be an
	 * array whose component type should be used as the conversion target for extraneous
	 * arguments. For example, if the parameter types are <code>{Integer, String[]}</code>
	 * and the input arguments are <code>{Integer, boolean, float}</code>, then both the
	 * {@code boolean} and the {@code float} must be converted to strings.
	 * <p>This method does <strong>not</strong> repackage the arguments into a form suitable
	 * for the varargs invocation: a subsequent call to
	 * {@link #setupArgumentsForVarargsInvocation(Class[], Object...)} is required for that.
	 * @param converter the converter to use for type conversions
	 * @param arguments the arguments to convert to the required parameter types
	 * @param method the target {@code Method}
	 * @return {@code true} if some kind of conversion occurred on an argument
	 * @throws SpelEvaluationException if a problem occurs during conversion
	 */
	public static boolean convertAllArguments(TypeConverter converter, Object[] arguments, Method method)
			throws SpelEvaluationException {

		Integer varargsPosition = (method.isVarArgs() ? method.getParameterCount() - 1 : null);
		return convertArguments(converter, arguments, method, varargsPosition);
	}

	/**
	 * Convert the supplied set of arguments into the parameter types of the supplied
	 * {@link Executable}, taking the varargs position into account.
	 * <p>The arguments are converted 'in-place' in the input array.
	 * @param converter the converter to use for type conversions
	 * @param arguments the arguments to convert to the required parameter types
	 * @param executable the target {@code Method} or {@code Constructor}
	 * @param varargsPosition the known position of the varargs argument, if any
	 * ({@code null} if not varargs)
	 * @return {@code true} if some kind of conversion occurred on an argument
	 * @throws EvaluationException if a problem occurs during conversion
	 */
	static boolean convertArguments(TypeConverter converter, Object[] arguments, Executable executable,
			@Nullable Integer varargsPosition) throws EvaluationException {

		boolean conversionOccurred = false;
		if (varargsPosition == null) {
			for (int i = 0; i < arguments.length; i++) {
				TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forExecutable(executable, i));
				Object argument = arguments[i];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				arguments[i] = converter.convertValue(argument, sourceType, targetType);
				conversionOccurred |= (argument != arguments[i]);
			}
		}
		else {
			// Convert everything up to the varargs position
			for (int i = 0; i < varargsPosition; i++) {
				TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forExecutable(executable, i));
				Object argument = arguments[i];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				arguments[i] = converter.convertValue(argument, sourceType, targetType);
				conversionOccurred |= (argument != arguments[i]);
			}

			MethodParameter methodParam = MethodParameter.forExecutable(executable, varargsPosition);
			TypeDescriptor targetType = new TypeDescriptor(methodParam);
			TypeDescriptor componentTypeDesc = targetType.getElementTypeDescriptor();
			Assert.state(componentTypeDesc != null, "Component type must not be null for a varargs array");

			// If the target is varargs and there is just one more argument, then convert it here.
			if (varargsPosition == arguments.length - 1) {
				Object argument = arguments[varargsPosition];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				if (argument == null) {
					// Perform the equivalent of GenericConversionService.convertNullSource() for a single argument.
					if (componentTypeDesc.getObjectType() == Optional.class) {
						arguments[varargsPosition] = Optional.empty();
						conversionOccurred = true;
					}
				}
				// If the argument type is assignable to the varargs component type, there is no need to
				// convert it or wrap it in an array. For example, using StringToArrayConverter to convert
				// a String containing a comma would result in the String being split and repackaged in an
				// array when it should be used as-is. Similarly, if the argument is an array that is
				// assignable to the varargs array type, there is no need to convert it. However, if the
				// argument is a java.util.List, we let the TypeConverter convert the list to an array.
				else if (!sourceType.isAssignableTo(componentTypeDesc) ||
						(sourceType.isArray() && !sourceType.isAssignableTo(targetType)) ||
						(argument instanceof List)) {

					TypeDescriptor targetTypeToUse =
							(sourceType.isArray() || argument instanceof List ? targetType : componentTypeDesc);
					arguments[varargsPosition] = converter.convertValue(argument, sourceType, targetTypeToUse);
				}
				// Possible outcomes of the above if-else block:
				// 1) the input argument was null, and nothing was done.
				// 2) the input argument was null; the varargs component type is Optional; and the argument was converted to Optional.empty().
				// 3) the input argument was the correct type but not wrapped in an array, and nothing was done.
				// 4) the input argument was already compatible (i.e., array of valid type), and nothing was done.
				// 5) the input argument was the wrong type and got converted and wrapped in an array.
				if (argument != arguments[varargsPosition] &&
						!isFirstEntryInArray(argument, arguments[varargsPosition])) {
					conversionOccurred = true; // case 5
				}
			}
			// Otherwise, convert remaining arguments to the varargs component type.
			else {
				for (int i = varargsPosition; i < arguments.length; i++) {
					Object argument = arguments[i];
					TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
					arguments[i] = converter.convertValue(argument, sourceType, componentTypeDesc);
					conversionOccurred |= (argument != arguments[i]);
				}
			}
		}
		return conversionOccurred;
	}

	/**
	 * Convert the supplied set of arguments into the parameter types of the supplied
	 * {@link MethodHandle}, taking the varargs position into account.
	 * <p>The arguments are converted 'in-place' in the input array.
	 * @param converter the converter to use for type conversions
	 * @param arguments the arguments to convert to the required parameter types
	 * @param methodHandle the target {@code MethodHandle}
	 * @param varargsPosition the known position of the varargs argument, if any
	 * ({@code null} if not varargs)
	 * @return {@code true} if some kind of conversion occurred on an argument
	 * @throws EvaluationException if a problem occurs during conversion
	 * @since 6.1
	 */
	public static boolean convertAllMethodHandleArguments(TypeConverter converter, Object[] arguments,
			MethodHandle methodHandle, @Nullable Integer varargsPosition) throws EvaluationException {

		boolean conversionOccurred = false;
		MethodType methodHandleType = methodHandle.type();
		if (varargsPosition == null) {
			for (int i = 0; i < arguments.length; i++) {
				Class<?> argumentClass = methodHandleType.parameterType(i);
				ResolvableType resolvableType = ResolvableType.forClass(argumentClass);
				TypeDescriptor targetType = new TypeDescriptor(resolvableType, argumentClass, null);

				Object argument = arguments[i];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				arguments[i] = converter.convertValue(argument, sourceType, targetType);
				conversionOccurred |= (argument != arguments[i]);
			}
		}
		else {
			// Convert everything up to the varargs position
			for (int i = 0; i < varargsPosition; i++) {
				Class<?> argumentClass = methodHandleType.parameterType(i);
				ResolvableType resolvableType = ResolvableType.forClass(argumentClass);
				TypeDescriptor targetType = new TypeDescriptor(resolvableType, argumentClass, null);

				Object argument = arguments[i];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				arguments[i] = converter.convertValue(argument, sourceType, targetType);
				conversionOccurred |= (argument != arguments[i]);
			}

			Class<?> varargsArrayClass = methodHandleType.lastParameterType();
			// We use the wrapper type for a primitive varargs array, since we eventually
			// need an Object array in order to invoke the MethodHandle in
			// FunctionReference#executeFunctionViaMethodHandle().
			Class<?> varargsComponentClass = ClassUtils.resolvePrimitiveIfNecessary(varargsArrayClass.componentType());
			TypeDescriptor varargsArrayType = TypeDescriptor.array(TypeDescriptor.valueOf(varargsComponentClass));
			TypeDescriptor varargsComponentType = varargsArrayType.getElementTypeDescriptor();
			Assert.state(varargsComponentType != null, "Component type must not be null for a varargs array");

			// If the target is varargs and there is just one more argument, then convert it here.
			if (varargsPosition == arguments.length - 1) {
				Object argument = arguments[varargsPosition];
				TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
				if (argument == null) {
					// Perform the equivalent of GenericConversionService.convertNullSource() for a single argument.
					if (varargsComponentType.getObjectType() == Optional.class) {
						arguments[varargsPosition] = Optional.empty();
						conversionOccurred = true;
					}
				}
				// If the argument type is assignable to the varargs component type, there is no need to
				// convert it. For example, using StringToArrayConverter to convert a String containing a
				// comma would result in the String being split and repackaged in an array when it should
				// be used as-is. Similarly, if the argument is an array that is assignable to the varargs
				// array type, there is no need to convert it. However, if the argument is a java.util.List,
				// we let the TypeConverter convert the list to an array.
				else if (!sourceType.isAssignableTo(varargsComponentType) ||
						(sourceType.isArray() && !sourceType.isAssignableTo(varargsArrayType)) ||
						(argument instanceof List)) {

					TypeDescriptor targetTypeToUse =
							(sourceType.isArray() || argument instanceof List ? varargsArrayType : varargsComponentType);
					arguments[varargsPosition] = converter.convertValue(argument, sourceType, targetTypeToUse);
				}
				// Possible outcomes of the above if-else block:
				// 1) the input argument was null, and nothing was done.
				// 2) the input argument was null; the varargs component type is Optional; and the argument was converted to Optional.empty().
				// 3) the input argument was the correct type but not wrapped in an array, and nothing was done.
				// 4) the input argument was already compatible (i.e., an Object array of valid type), and nothing was done.
				// 5) the input argument was the wrong type and got converted as explained in the comments above.
				if (argument != arguments[varargsPosition] &&
						!isFirstEntryInArray(argument, arguments[varargsPosition])) {
					conversionOccurred = true; // case 5
				}
			}
			// Otherwise, convert remaining arguments to the varargs component type.
			else {
				for (int i = varargsPosition; i < arguments.length; i++) {
					Object argument = arguments[i];
					TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
					arguments[i] = converter.convertValue(argument, sourceType, varargsComponentType);
					conversionOccurred |= (argument != arguments[i]);
				}
			}
		}
		return conversionOccurred;
	}

	/**
	 * Check if the supplied value is the first entry in the array represented by the possibleArray value.
	 * @param value the value to check for in the array
	 * @param possibleArray an array object that may have the supplied value as the first element
	 * @return true if the supplied value is the first entry in the array
	 */
	private static boolean isFirstEntryInArray(Object value, @Nullable Object possibleArray) {
		if (possibleArray == null) {
			return false;
		}
		Class<?> type = possibleArray.getClass();
		if (!type.isArray() || Array.getLength(possibleArray) == 0 ||
				!ClassUtils.isAssignableValue(type.componentType(), value)) {
			return false;
		}
		Object arrayValue = Array.get(possibleArray, 0);
		return (type.componentType().isPrimitive() ? arrayValue.equals(value) : arrayValue == value);
	}

	/**
	 * Package up the supplied {@code args} so that they correctly match what is
	 * expected in {@code requiredParameterTypes}.
	 * <p>For example, if {@code requiredParameterTypes} is {@code (int, String[])}
	 * because the second parameter was declared as {@code String...}, then if
	 * {@code args} is {@code [1, "a", "b"]} it must be repackaged as
	 * {@code [1, new String[] {"a", "b"}]} in order to match the expected types.
	 * @param requiredParameterTypes the types of the parameters for the invocation
	 * @param args the arguments to be set up for the invocation
	 * @return a repackaged array of arguments where any varargs setup has been performed
	 */
	public static Object[] setupArgumentsForVarargsInvocation(Class<?>[] requiredParameterTypes, Object... args) {
		Assert.notEmpty(requiredParameterTypes, "Required parameter types array must not be empty");

		int parameterCount = requiredParameterTypes.length;
		Class<?> lastRequiredParameterType = requiredParameterTypes[parameterCount - 1];
		Assert.isTrue(lastRequiredParameterType.isArray(),
				"The last required parameter type must be an array to support varargs invocation");

		int argumentCount = args.length;
		Object lastArgument = (argumentCount > 0 ? args[argumentCount - 1] : null);

		// Check if repackaging is needed...
		if (parameterCount != argumentCount || !lastRequiredParameterType.isInstance(lastArgument)) {
			// Create an array for the leading arguments plus the varargs array argument.
			Object[] newArgs = new Object[parameterCount];
			// Copy all leading arguments to the new array, omitting the varargs array argument.
			System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);

			// Now sort out the final argument, which is the varargs one. Before entering this method,
			// the arguments should have been converted to the box form of the required type.
			int varargsArraySize = 0;  // zero size array if nothing to pass as the varargs parameter
			if (argumentCount >= parameterCount) {
				varargsArraySize = argumentCount - (parameterCount - 1);
			}
			Class<?> componentType = lastRequiredParameterType.componentType();
			Object varargsArray = Array.newInstance(componentType, varargsArraySize);
			for (int i = 0; i < varargsArraySize; i++) {
				Array.set(varargsArray, i, args[parameterCount - 1 + i]);
			}
			// Finally, add the varargs array to the new arguments array.
			newArgs[newArgs.length - 1] = varargsArray;
			return newArgs;
		}

		return args;
	}


	/**
	 * {@code ArgumentsMatchKind} describes what kind of match was achieved between two sets
	 * of arguments: the set that a method/constructor is expecting and the set that is being
	 * supplied at the point of invocation.
	 */
	enum ArgumentsMatchKind {

		/**
		 * An exact match is where the parameter types exactly match what the method/constructor is expecting.
		 */
		EXACT,

		/**
		 * A close match is where the parameter types either exactly match or are assignment-compatible.
		 */
		CLOSE,

		/**
		 * A conversion match is where the type converter must be used to transform some of the parameter types.
		 */
		REQUIRES_CONVERSION;

		public boolean isExactMatch() {
			return (this == EXACT);
		}

		public boolean isCloseMatch() {
			return (this == CLOSE);
		}

		public boolean isMatchRequiringConversion() {
			return (this == REQUIRES_CONVERSION);
		}
	}

}
