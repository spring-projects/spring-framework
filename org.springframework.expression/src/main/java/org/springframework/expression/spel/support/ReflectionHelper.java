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
import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Utility methods used by the reflection resolver code to discover the appropriae
 * methods/constructors and fields that should be used in expressions.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectionHelper {

	/**
	 * Compare argument arrays and return information about whether they match. A supplied type converter and
	 * conversionAllowed flag allow for matches to take into account that a type may be transformed into a different
	 * type by the converter.
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	public static ArgumentsMatchInfo compareArguments( 
			Class[] expectedArgTypes, Class[] suppliedArgTypes, TypeConverter typeConverter) {

		Assert.isTrue(expectedArgTypes.length==suppliedArgTypes.length, "Expected argument types and supplied argument types should be arrays of same length");

		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;
		for (int i = 0; i < expectedArgTypes.length && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class expectedArg = expectedArgTypes[i];
			if (expectedArg != suppliedArg) {
				if (ClassUtils.isAssignable(expectedArg, suppliedArg)
				/* || isWidenableTo(expectedArg, suppliedArg) */) {
					if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
						match = ArgsMatchKind.CLOSE;
					} 
				} else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
					if (argsRequiringConversion == null) {
						argsRequiringConversion = new ArrayList<Integer>();
					}
					argsRequiringConversion.add(i);
					match = ArgsMatchKind.REQUIRES_CONVERSION;
				} else {
					match = null;
				}
			}
		}
		if (match == null) {
			return null;
		} else {
			if (match == ArgsMatchKind.REQUIRES_CONVERSION) {
				int[] argsArray = new int[argsRequiringConversion.size()];
				for (int i = 0; i < argsRequiringConversion.size(); i++) {
					argsArray[i] = argsRequiringConversion.get(i);
				}
				return new ArgumentsMatchInfo(match, argsArray);
			} else {
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
	public static ArgumentsMatchInfo compareArgumentsVarargs(
			Class[] expectedArgTypes, Class[] suppliedArgTypes, TypeConverter typeConverter) {
 
		Assert.isTrue(expectedArgTypes!=null && expectedArgTypes.length>0, "Expected arguments must at least include one array (the vargargs parameter)");
		Assert.isTrue(expectedArgTypes[expectedArgTypes.length-1].isArray(), "Final expected argument should be array type (the varargs parameter)");
		
		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;

		// Check up until the varargs argument:

		// Deal with the arguments up to 'expected number' - 1 (that is everything but the varargs argument)
		int argCountUpToVarargs = expectedArgTypes.length-1;
		for (int i = 0; i < argCountUpToVarargs && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class expectedArg = expectedArgTypes[i];
			if (expectedArg != suppliedArg) {
				if (expectedArg.isAssignableFrom(suppliedArg) || ClassUtils.isAssignableValue(expectedArg, suppliedArg)) {
					if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
						match = ArgsMatchKind.CLOSE;
					}
				} else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
					if (argsRequiringConversion == null) {
						argsRequiringConversion = new ArrayList<Integer>();
					}
					argsRequiringConversion.add(i);
					match = ArgsMatchKind.REQUIRES_CONVERSION;
				} else {
					match = null;
				}
			}
		}
		// If already confirmed it cannot be a match, then returnW
		if (match == null) {
			return null;
		}

		// Special case: there is one parameter left and it is an array and it matches the varargs expected argument -
		// that is a match, the caller has already built the array
		if (suppliedArgTypes.length == expectedArgTypes.length
				&& expectedArgTypes[expectedArgTypes.length - 1] == suppliedArgTypes[suppliedArgTypes.length - 1]) {
		} else {
			// Now... we have the final argument in the method we are checking as a match and we have 0 or more other
			// arguments left to pass to it.
			Class varargsParameterType = expectedArgTypes[expectedArgTypes.length - 1].getComponentType();

			// All remaining parameters must be of this type or convertable to this type
			for (int i = expectedArgTypes.length - 1; i < suppliedArgTypes.length; i++) {
				Class suppliedArg = suppliedArgTypes[i];
				if (varargsParameterType != suppliedArg) {
					if (ClassUtils.isAssignable(varargsParameterType, suppliedArg)) {
						if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
							match = ArgsMatchKind.CLOSE;
						}
					} else if (typeConverter.canConvert(suppliedArg, varargsParameterType)) {
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
	 * Takes an input set of argument values and, following the positions specified in the int array, it converts
	 * them to the types specified as the required parameter types.  The arguments are converted 'in-place' in the
	 * input array.
	 * @param requiredParameterTypes the types that the caller would like to have
	 * @param isVarargs whether the requiredParameterTypes is a varargs list
	 * @param converter the type converter to use for attempting conversions
	 * @param argumentsRequiringConversion details which of the input arguments need conversion
	 * @param arguments the actual arguments that need conversion
	 * @throws EvaluationException if a problem occurs during conversion
	 */
	public static void convertArguments(Class[] requiredParameterTypes, boolean isVarargs, TypeConverter converter,
			int[] argumentsRequiringConversion, Object[] arguments) throws EvaluationException {
		 
		Assert.notNull(argumentsRequiringConversion,"should not be called if no conversions required");
		Assert.notNull(arguments,"should not be called if no conversions required");
		
		Class varargsType = null;
		if (isVarargs) {
			Assert.isTrue(requiredParameterTypes[requiredParameterTypes.length-1].isArray(),"if varargs then last parameter type must be array");
			varargsType = requiredParameterTypes[requiredParameterTypes.length - 1].getComponentType();
		}
		for (Integer argPosition : argumentsRequiringConversion) {
			Class<?> targetType = null;
			if (isVarargs && argPosition >= (requiredParameterTypes.length - 1)) {
				targetType = varargsType;
			} else {
				targetType = requiredParameterTypes[argPosition];
			}
			arguments[argPosition] = converter.convertValue(arguments[argPosition], targetType);
		}
	}

	/**
	 * Convert a supplied set of arguments into the requested types.  If the parameterTypes are related to 
	 * a varargs method then the final entry in the parameterTypes array is going to be an array itself whose
	 * component type should be used as the conversion target for extraneous arguments. (For example, if the
	 * parameterTypes are {Integer, String[]} and the input arguments are {Integer, boolean, float} then both
	 * the boolean and float must be converted to strings).  This method does not repackage the arguments
	 * into a form suitable for the varargs invocation
	 * @param parameterTypes the types to be converted to
	 * @param isVarargs whether parameterTypes relates to a varargs method
	 * @param converter the converter to use for type conversions
	 * @param arguments the arguments to convert to the requested parameter types
	 * @throws SpelException if there is a problem with conversion
	 */
	public static void convertAllArguments(Class[] parameterTypes, boolean isVarargs, TypeConverter converter,
			Object[] arguments) throws SpelException {

		Assert.notNull(arguments,"should not be called if nothing to convert");
		
		Class varargsType = null;
		if (isVarargs) {
			Assert.isTrue(parameterTypes[parameterTypes.length-1].isArray(),"if varargs then last parameter type must be array");
			varargsType = parameterTypes[parameterTypes.length - 1].getComponentType();
		}
		for (int i = 0; i < arguments.length; i++) {
			Class<?> targetType = null;
			if (isVarargs && i >= (parameterTypes.length - 1)) {
				targetType = varargsType;
			} else {
				targetType = parameterTypes[i];
			}
			try {
				if (arguments[i] != null && arguments[i].getClass() != targetType) {
					if (converter == null) {
						throw new SpelException(SpelMessages.TYPE_CONVERSION_ERROR, arguments[i].getClass().getName(),targetType);
					}
					arguments[i] = converter.convertValue(arguments[i], targetType);
				}
			} catch (EvaluationException ex) {
				// allows for another type converter throwing a different kind of EvaluationException
				if (ex instanceof SpelException) {
					throw (SpelException)ex;
				} else {
					throw new SpelException(ex, SpelMessages.TYPE_CONVERSION_ERROR,arguments[i].getClass().getName(),targetType);
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
		if (parameterCount != args.length || requiredParameterTypes[parameterCount - 1] != (args[argumentCount - 1] == null ? null : args[argumentCount - 1].getClass())) {
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

		public ArgsMatchKind kind;

		public int[] argsRequiringConversion;

		ArgumentsMatchInfo(ArgsMatchKind kind, int[] integers) {
			this.kind = kind;
			argsRequiringConversion = integers;
		}

		ArgumentsMatchInfo(ArgsMatchKind kind) {
			this.kind = kind;
		}
		
		public boolean isExactMatch() {
			return kind==ArgsMatchKind.EXACT;
		}
		
		public boolean isCloseMatch() {
			return kind==ArgsMatchKind.CLOSE;
		}

		public boolean isMatchRequiringConversion() {
			return kind==ArgsMatchKind.REQUIRES_CONVERSION;
		}
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append("ArgumentMatch: ").append(kind);
			if (argsRequiringConversion!=null) {
				sb.append("  (argsForConversion:");
				for (int i=0;i<argsRequiringConversion.length;i++) {
					if (i>0) {
						sb.append(",");
					}
					sb.append(argsRequiringConversion[i]);
				}
				sb.append(")");
			}
			return sb.toString();
		}
	}

}
