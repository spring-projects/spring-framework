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
	 * Compare argument arrays and return information about whether they match. A supplied type converter
	 * and conversionAllowed flag allow for matches to take into account that a type may be transformed
	 * into a different type by the converter.
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	static ArgumentsMatchInfo compareArguments(
			Class[] expectedArgTypes, Class[] suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(expectedArgTypes.length == suppliedArgTypes.length,
				"Expected argument types and supplied argument types should be arrays of same length");

		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;
		for (int i = 0; i < expectedArgTypes.length && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class expectedArg = expectedArgTypes[i];
			if (expectedArg != suppliedArg) {
				// The user may supply null - and that will be ok unless a primitive is expected
				if (suppliedArg == null) {
					if (expectedArg.isPrimitive()) {
						match = null;
					}
				}
				else {
					if (ClassUtils.isAssignable(expectedArg, suppliedArg)) {
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
	 * Compare argument arrays and return information about whether they match. A supplied type converter and
	 * conversionAllowed flag allow for matches to take into account that a type may be transformed into a different
	 * type by the converter. This variant of compareArguments also allows for a varargs match.
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter 
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	static ArgumentsMatchInfo compareArgumentsVarargs(
			Class[] expectedArgTypes, Class[] suppliedArgTypes, TypeConverter typeConverter) {
 
		Assert.isTrue(expectedArgTypes != null && expectedArgTypes.length > 0,
				"Expected arguments must at least include one array (the vargargs parameter)");
		Assert.isTrue(expectedArgTypes[expectedArgTypes.length - 1].isArray(),
				"Final expected argument should be array type (the varargs parameter)");
		
		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;

		// Check up until the varargs argument:

		// Deal with the arguments up to 'expected number' - 1 (that is everything but the varargs argument)
		int argCountUpToVarargs = expectedArgTypes.length-1;
		for (int i = 0; i < argCountUpToVarargs && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class<?> expectedArg = expectedArgTypes[i];
			if (suppliedArg==null) {
				if (expectedArg.isPrimitive()) {
					match=null;
				}
			}
			else {
				if (expectedArg != suppliedArg) {
					if (expectedArg.isAssignableFrom(suppliedArg) || ClassUtils.isAssignableValue(expectedArg, suppliedArg)) {
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

		// Special case: there is one parameter left and it is an array and it matches the varargs expected argument -
		// that is a match, the caller has already built the array
		if (suppliedArgTypes.length == expectedArgTypes.length &&
				expectedArgTypes[expectedArgTypes.length - 1] == suppliedArgTypes[suppliedArgTypes.length - 1]) {
		}
		else {
			// Now... we have the final argument in the method we are checking as a match and we have 0 or more other
			// arguments left to pass to it.
			Class varargsParameterType = expectedArgTypes[expectedArgTypes.length - 1].getComponentType();

			// All remaining parameters must be of this type or convertable to this type
			for (int i = expectedArgTypes.length - 1; i < suppliedArgTypes.length; i++) {
				Class suppliedArg = suppliedArgTypes[i];
				if (varargsParameterType != suppliedArg) {
					if (suppliedArg==null) {
						if (varargsParameterType.isPrimitive()) {
							match=null;
						}
					}
					else {
						if (ClassUtils.isAssignable(varargsParameterType, suppliedArg)) {
							if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
								match = ArgsMatchKind.CLOSE;
							}
						}
						else if (typeConverter.canConvert(suppliedArg, varargsParameterType)) {
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
	 * @param argumentsRequiringConversion details which of the input arguments need conversion
	 * @param varargsPosition the known position of the varargs argument, if any
	 * @throws EvaluationException if a problem occurs during conversion
	 */
	static void convertArguments(TypeConverter converter, Object[] arguments, Object methodOrCtor,
			int[] argumentsRequiringConversion, Integer varargsPosition) throws EvaluationException {

		for (int argPosition : argumentsRequiringConversion) {
			TypeDescriptor targetType;
			if (varargsPosition != null && argPosition >= varargsPosition) {
				MethodParameter methodParam = MethodParameter.forMethodOrConstructor(methodOrCtor, varargsPosition);
				targetType = new TypeDescriptor(methodParam, methodParam.getParameterType().getComponentType());
			}
			else {
				targetType = new TypeDescriptor(MethodParameter.forMethodOrConstructor(methodOrCtor, argPosition));
			}
			arguments[argPosition] = converter.convertValue(
					arguments[argPosition], TypeDescriptor.forObject(arguments[argPosition]), targetType);
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
	public static void convertAllArguments(TypeConverter converter, Object[] arguments, Method method) throws SpelEvaluationException {
		Integer varargsPosition = null;
		if (method.isVarArgs()) {
			Class[] paramTypes = method.getParameterTypes();
			varargsPosition = paramTypes.length - 1;
		}
		for (int argPosition = 0; argPosition < arguments.length; argPosition++) {
			TypeDescriptor targetType;
			if (varargsPosition != null && argPosition >= varargsPosition) {
				MethodParameter methodParam = new MethodParameter(method, varargsPosition);
				targetType = new TypeDescriptor(methodParam, methodParam.getParameterType().getComponentType());
			}
			else {
				targetType = new TypeDescriptor(new MethodParameter(method, argPosition));
			}
			try {
				Object argument = arguments[argPosition];
				if (argument != null && !targetType.getObjectType().isInstance(argument)) {
					if (converter == null) {
						throw new SpelEvaluationException(SpelMessage.TYPE_CONVERSION_ERROR, argument.getClass().getName(), targetType);
					}
					arguments[argPosition] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
				}
			}
			catch (EvaluationException ex) {
				// allows for another type converter throwing a different kind of EvaluationException
				if (ex instanceof SpelEvaluationException) {
					throw (SpelEvaluationException)ex;
				}
				else {
					throw new SpelEvaluationException(ex, SpelMessage.TYPE_CONVERSION_ERROR,arguments[argPosition].getClass().getName(), targetType);
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
	public static Object[] setupArgumentsForVarargsInvocation(Class[] requiredParameterTypes, Object... args) {
		// Check if array already built for final argument
		int parameterCount = requiredParameterTypes.length;
		int argumentCount = args.length;

		// Check if repackaging is needed:
		if (parameterCount != args.length ||
				requiredParameterTypes[parameterCount - 1] !=
						(args[argumentCount - 1] == null ? null : args[argumentCount - 1].getClass())) {
			int arraySize = 0; // zero size array if nothing to pass as the varargs parameter
			if (argumentCount >= parameterCount) {
				arraySize = argumentCount - (parameterCount - 1);
			}
			Object[] repackagedArguments = (Object[]) Array.newInstance(requiredParameterTypes[parameterCount - 1].getComponentType(),
					arraySize);
			// Copy all but the varargs arguments
			for (int i = 0; i < arraySize; i++) {
				repackagedArguments[i] = args[parameterCount + i - 1];
			}
			// Create an array for the varargs arguments
			Object[] newArgs = new Object[parameterCount];
			for (int i = 0; i < newArgs.length - 1; i++) {
				newArgs[i] = args[i];
			}
			newArgs[newArgs.length - 1] = repackagedArguments;
			return newArgs;
		}
		return args;
	}


	public static enum ArgsMatchKind {
		// An exact match is where the parameter types exactly match what the method/constructor being invoked is expecting
		EXACT, 
		// A close match is where the parameter types either exactly match or are assignment compatible with the method/constructor being invoked
		CLOSE, 
		// A conversion match is where the type converter must be used to transform some of the parameter types
		REQUIRES_CONVERSION
	}


	/**
	 * An instance of ArgumentsMatchInfo describes what kind of match was achieved between two sets of arguments - the set that a
	 * method/constructor is expecting and the set that are being supplied at the point of invocation. If the kind
	 * indicates that conversion is required for some of the arguments then the arguments that require conversion are
	 * listed in the argsRequiringConversion array.
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
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("ArgumentMatch: ").append(this.kind);
			if (this.argsRequiringConversion != null) {
				sb.append("  (argsForConversion:");
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
