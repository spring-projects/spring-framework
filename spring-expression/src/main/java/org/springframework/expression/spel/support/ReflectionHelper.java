/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;

/**
 * Utility methods used by the reflection resolver code to discover the appropriate
 * methods/constructors and fields that should be used in expressions.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectionHelper {

	/**
	 * Compare argument arrays and return information about whether they match. A supplied
	 * type converter and conversionAllowed flag allow for matches to take into account
	 * that a type may be transformed into a different type by the converter.
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	static ArgumentsMatchInfo compareArguments(
			List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(expectedArgTypes.size() == suppliedArgTypes.size(),
				"Expected argument types and supplied argument types should be arrays of same length");

		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;
		for (int i = 0; i < expectedArgTypes.size() && match != null; i++) {
			TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
			TypeDescriptor expectedArg = expectedArgTypes.get(i);
			if (!expectedArg.equals(suppliedArg)) {
				// The user may supply null - and that will be ok unless a primitive is expected
				if (suppliedArg == null) {
					if (expectedArg.isPrimitive()) {
						match = null;
					}
				}
				else {
					if (suppliedArg.isAssignableTo(expectedArg)) {
						if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
							match = ArgsMatchKind.CLOSE;
						}
					}
					else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
						if (argsRequiringConversion == null) {
							argsRequiringConversion = new ArrayList<Integer>();
						}
						argsRequiringConversion.add(i);
						match = ArgsMatchKind.REQUIRES_CONVERSION;
					}
					else {
						match = null;
					}
				}
			}
		}
		if (match == null) {
			return null;
		}
		else {
			if (match == ArgsMatchKind.REQUIRES_CONVERSION) {
				int[] argsArray = new int[argsRequiringConversion.size()];
				for (int i = 0; i < argsRequiringConversion.size(); i++) {
					argsArray[i] = argsRequiringConversion.get(i);
				}
				return new ArgumentsMatchInfo(match, argsArray);
			}
			else {
				return new ArgumentsMatchInfo(match);
			}
		}
	}

	/**
	 * Based on {@link MethodInvoker#getTypeDifferenceWeight(Class[], Object[])} but operates on TypeDescriptors.
	 */
	public static int getTypeDifferenceWeight(List<TypeDescriptor> paramTypes, List<TypeDescriptor> argTypes) {
		int result = 0;
		for (int i = 0; i < paramTypes.size(); i++) {
			TypeDescriptor paramType = paramTypes.get(i);
			TypeDescriptor argType = argTypes.get(i);
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
	 * Compare argument arrays and return information about whether they match. A supplied type converter and
	 * conversionAllowed flag allow for matches to take into account that a type may be transformed into a different
	 * type by the converter. This variant of compareArguments also allows for a varargs match.
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	static ArgumentsMatchInfo compareArgumentsVarargs(
			List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(expectedArgTypes != null && expectedArgTypes.size() > 0,
				"Expected arguments must at least include one array (the vargargs parameter)");
		Assert.isTrue(expectedArgTypes.get(expectedArgTypes.size() - 1).isArray(),
				"Final expected argument should be array type (the varargs parameter)");

		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;

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
						if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
							match = ArgsMatchKind.CLOSE;
						}
					}
					else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
						if (argsRequiringConversion == null) {
							argsRequiringConversion = new ArrayList<Integer>();
						}
						argsRequiringConversion.add(i);
						match = ArgsMatchKind.REQUIRES_CONVERSION;
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
			Class<?> varargsParamType = expectedArgTypes.get(expectedArgTypes.size() - 1).getElementTypeDescriptor().getType();

			// All remaining parameters must be of this type or convertable to this type
			for (int i = expectedArgTypes.size() - 1; i < suppliedArgTypes.size(); i++) {
				TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
				if (suppliedArg == null) {
					if (varargsParamType.isPrimitive()) {
						match = null;
					}
				}
				else {
					if (varargsParamType != suppliedArg.getType()) {
						if (ClassUtils.isAssignable(varargsParamType, suppliedArg.getType())) {
							if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
								match = ArgsMatchKind.CLOSE;
							}
						}
						else if (typeConverter.canConvert(suppliedArg, TypeDescriptor.valueOf(varargsParamType))) {
							if (argsRequiringConversion == null) {
								argsRequiringConversion = new ArrayList<Integer>();
							}
							argsRequiringConversion.add(i);
							match = ArgsMatchKind.REQUIRES_CONVERSION;
						}
						else {
							match = null;
						}
					}
				}
			}
		}

		if (match == null) {
			return null;
		}
		else {
			if (match == ArgsMatchKind.REQUIRES_CONVERSION) {
				int[] argsArray = new int[argsRequiringConversion.size()];
				for (int i = 0; i < argsRequiringConversion.size(); i++) {
					argsArray[i] = argsRequiringConversion.get(i);
				}
				return new ArgumentsMatchInfo(match, argsArray);
			}
			else {
				return new ArgumentsMatchInfo(match);
			}
		}
	}

	/**
	 * Takes an input set of argument values and, following the positions specified in the int array,
	 * it converts them to the types specified as the required parameter types. The arguments are
	 * converted 'in-place' in the input array.
	 * @param converter the type converter to use for attempting conversions
	 * @param arguments the actual arguments that need conversion
	 * @param methodOrCtor the target Method or Constructor
	 * @param argumentsRequiringConversion details which of the input arguments for sure need conversion
	 * @param varargsPosition the known position of the varargs argument, if any
	 * @throws EvaluationException if a problem occurs during conversion
	 */
	static void convertArguments(TypeConverter converter, Object[] arguments, Object methodOrCtor,
			int[] argumentsRequiringConversion, Integer varargsPosition) throws EvaluationException {

		if (varargsPosition == null) {
			for (int i = 0; i < arguments.length; i++) {
				TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forMethodOrConstructor(methodOrCtor, i));
				Object argument = arguments[i];
				arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
			}
		}
		else {
			for (int i = 0; i < varargsPosition; i++) {
				TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forMethodOrConstructor(methodOrCtor, i));
				Object argument = arguments[i];
				arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
			}
			MethodParameter methodParam = MethodParameter.forMethodOrConstructor(methodOrCtor, varargsPosition);
			if (varargsPosition == arguments.length - 1) {
				TypeDescriptor targetType = new TypeDescriptor(methodParam);
				Object argument = arguments[varargsPosition];
				arguments[varargsPosition] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
			}
			else {
				TypeDescriptor targetType = TypeDescriptor.nested(methodParam, 1);
				for (int i = varargsPosition; i < arguments.length; i++) {
					Object argument = arguments[i];
					arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
				}
			}
		}
	}

	/**
	 * Convert a supplied set of arguments into the requested types. If the parameterTypes are related to
	 * a varargs method then the final entry in the parameterTypes array is going to be an array itself whose
	 * component type should be used as the conversion target for extraneous arguments. (For example, if the
	 * parameterTypes are {Integer, String[]} and the input arguments are {Integer, boolean, float} then both
	 * the boolean and float must be converted to strings). This method does not repackage the arguments
	 * into a form suitable for the varargs invocation
	 * @param converter the converter to use for type conversions
	 * @param arguments the arguments to convert to the requested parameter types
	 * @param method the target Method
	 * @throws SpelEvaluationException if there is a problem with conversion
	 */
	public static void convertAllArguments(TypeConverter converter, Object[] arguments, Method method)
			throws SpelEvaluationException {

		Integer varargsPosition = null;
		if (method.isVarArgs()) {
			Class<?>[] paramTypes = method.getParameterTypes();
			varargsPosition = paramTypes.length - 1;
		}
		for (int argPos = 0; argPos < arguments.length; argPos++) {
			TypeDescriptor targetType;
			if (varargsPosition != null && argPos >= varargsPosition) {
				MethodParameter methodParam = new MethodParameter(method, varargsPosition);
				targetType = TypeDescriptor.nested(methodParam, 1);
			}
			else {
				targetType = new TypeDescriptor(new MethodParameter(method, argPos));
			}
			try {
				Object argument = arguments[argPos];
				if (argument != null && !targetType.getObjectType().isInstance(argument)) {
					if (converter == null) {
						throw new SpelEvaluationException(
								SpelMessage.TYPE_CONVERSION_ERROR, argument.getClass().getName(), targetType);
					}
					arguments[argPos] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
				}
			}
			catch (EvaluationException ex) {
				// allows for another type converter throwing a different kind of EvaluationException
				if (ex instanceof SpelEvaluationException) {
					throw (SpelEvaluationException)ex;
				}
				else {
					throw new SpelEvaluationException(ex,
							SpelMessage.TYPE_CONVERSION_ERROR,arguments[argPos].getClass().getName(), targetType);
				}
			}
		}
	}

	/**
	 * Package up the arguments so that they correctly match what is expected in parameterTypes. For example, if
	 * parameterTypes is (int, String[]) because the second parameter was declared String... then if arguments is
	 * [1,"a","b"] then it must be repackaged as [1,new String[]{"a","b"}] in order to match the expected
	 * parameterTypes.
	 * @param requiredParameterTypes the types of the parameters for the invocation
	 * @param args the arguments to be setup ready for the invocation
	 * @return a repackaged array of arguments where any varargs setup has been done
	 */
	public static Object[] setupArgumentsForVarargsInvocation(Class<?>[] requiredParameterTypes, Object... args) {
		// Check if array already built for final argument
		int parameterCount = requiredParameterTypes.length;
		int argumentCount = args.length;

		// Check if repackaging is needed:
		if (parameterCount != args.length ||
				requiredParameterTypes[parameterCount - 1] !=
						(args[argumentCount - 1] != null ? args[argumentCount - 1].getClass() : null)) {

			int arraySize = 0;  // zero size array if nothing to pass as the varargs parameter
			if (argumentCount >= parameterCount) {
				arraySize = argumentCount - (parameterCount - 1);
			}

			// Create an array for the varargs arguments
			Object[] newArgs = new Object[parameterCount];
			System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);

			// Now sort out the final argument, which is the varargs one. Before entering this method,
			// the arguments should have been converted to the box form of the required type.
			Class<?> componentType = requiredParameterTypes[parameterCount - 1].getComponentType();
			Object repackagedArgs = Array.newInstance(componentType, arraySize);
			for (int i = 0; i < arraySize; i++) {
				Array.set(repackagedArgs, i, args[parameterCount - 1 + i]);
			}
			newArgs[newArgs.length - 1] = repackagedArgs;
			return newArgs;
		}
		return args;
	}


	/**
	 * @deprecated as of Spring 3.2.7, since there is no need to ever refer to this directly...
	 * and {@link #REQUIRES_CONVERSION} is effectively unused, going away in Spring 4.0
	 */
	@Deprecated
	public static enum ArgsMatchKind {

		/** An exact match is where the parameter types exactly match what the method/constructor is expecting */
		EXACT,

		/** A close match is where the parameter types either exactly match or are assignment-compatible */
		CLOSE,

		/** A conversion match is where the type converter must be used to transform some of the parameter types */
		REQUIRES_CONVERSION
	}


	/**
	 * An instance of ArgumentsMatchInfo describes what kind of match was achieved between two sets of arguments -
	 * the set that a method/constructor is expecting and the set that are being supplied at the point of invocation.
	 * If the kind indicates that conversion is required for some of the arguments then the arguments that require
	 * conversion are listed in the argsRequiringConversion array.
	 */
	public static class ArgumentsMatchInfo {

		public final ArgsMatchKind kind;

		public int[] argsRequiringConversion;

		ArgumentsMatchInfo(ArgsMatchKind kind, int[] integers) {
			this.kind = kind;
			this.argsRequiringConversion = integers;
		}

		ArgumentsMatchInfo(ArgsMatchKind kind) {
			this.kind = kind;
		}

		public boolean isExactMatch() {
			return (this.kind == ArgsMatchKind.EXACT);
		}

		public boolean isCloseMatch() {
			return (this.kind == ArgsMatchKind.CLOSE);
		}

		public boolean isMatchRequiringConversion() {
			return (this.kind == ArgsMatchKind.REQUIRES_CONVERSION);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("ArgumentsMatchInfo: ").append(this.kind);
			if (this.argsRequiringConversion != null) {
				sb.append(" (argsRequiringConversion:");
				for (int i = 0; i < this.argsRequiringConversion.length;i++) {
					if (i > 0) {
						sb.append(",");
					}
					sb.append(this.argsRequiringConversion[i]);
				}
				sb.append(")");
			}
			return sb.toString();
		}
	}

}
