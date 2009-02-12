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
	 * @param conversionAllowed if true then allow for what the type converter can do when seeing if a supplied type can
	 * match an expected type
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	static ArgumentsMatchInfo compareArguments(
			Class[] expectedArgTypes, Class[] suppliedArgTypes, TypeConverter typeConverter) {

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
	 * type by the converter. This variant of compareArguments allows for a varargs match.
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @param conversionAllowed if true then allow for what the type converter can do when seeing if a supplied type can
	 * match an expected type
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	static ArgumentsMatchInfo compareArgumentsVarargs(
			Class[] expectedArgTypes, Class[] suppliedArgTypes, TypeConverter typeConverter) {

		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;

		// Check up until the varargs argument:

		// Deal with the arguments up to 'expected number' - 1
		for (int i = 0; i < expectedArgTypes.length - 1 && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class expectedArg = expectedArgTypes[i];
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
				} else {
					match = null;
				}
			}
		}
		// Already does not match
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

	static void convertArguments(Class[] parameterTypes, boolean isVarargs, TypeConverter converter,
			int[] argsRequiringConversion, Object... arguments) throws EvaluationException {
		Class varargsType = null;
		if (isVarargs) {
			varargsType = parameterTypes[parameterTypes.length - 1].getComponentType();
		}
		for (Integer argPosition : argsRequiringConversion) {
			Class<?> targetType = null;
			if (isVarargs && argPosition >= (parameterTypes.length - 1)) {
				targetType = varargsType;
			}
			else {
				targetType = parameterTypes[argPosition];
			}
			// try {
			arguments[argPosition] = converter.convertValue(arguments[argPosition], targetType);
			// } catch (EvaluationException e) {
			// throw new SpelException(e, SpelMessages.PROBLEM_DURING_TYPE_CONVERSION, "Converter failed to convert '"
			// + arguments[argPosition] + " to type '" + targetType + "'");
			// }
		}
	}

	public static void convertArguments(Class[] parameterTypes, boolean isVarargs, TypeConverter converter,
			Object... arguments) throws EvaluationException {

		Class varargsType = null;
		if (isVarargs) {
			varargsType = parameterTypes[parameterTypes.length - 1].getComponentType();
		}
		for (int i = 0; i < arguments.length; i++) {
			Class<?> targetType = null;
			if (isVarargs && i >= (parameterTypes.length - 1)) {
				targetType = varargsType;
			}
			else {
				targetType = parameterTypes[i];
			}
			if (converter == null) {
				throw new SpelException(SpelMessages.PROBLEM_DURING_TYPE_CONVERSION,
						"No converter available to convert '" + arguments[i] + " to type '" + targetType + "'");
			}
			try {
				if (arguments[i] != null && arguments[i].getClass() != targetType) {
					arguments[i] = converter.convertValue(arguments[i], targetType);
				}
			}
			catch (EvaluationException ex) {
				// allows for another type converter throwing a different kind of EvaluationException
				if (ex instanceof SpelException) {
					throw ex;
				}
				else {
					throw new SpelException(ex, SpelMessages.PROBLEM_DURING_TYPE_CONVERSION,
							"Converter failed to convert '" + arguments[i].getClass().getName() + "' to type '" + targetType + "'");
				}
			}
		}
	}

	/**
	 * Package up the arguments so that they correctly match what is expected in parameterTypes. For example, if
	 * parameterTypes is (int, String[]) because the second parameter was declared String... then if arguments is
	 * [1,"a","b"] then it must be repackaged as [1,new String[]{"a","b"}] in order to match the expected
	 * parameterTypes.
	 * @param paramTypes the types of the parameters for the invocation
	 * @param args the arguments to be setup ready for the invocation
	 * @return a repackaged array of arguments where any varargs setup has been done
	 */
	public static Object[] setupArgumentsForVarargsInvocation(Class[] paramTypes, Object... args) {
		// Check if array already built for final argument
		int nParams = paramTypes.length;
		int nArgs = args.length;

		// Check if repackaging is needed:
		if (nParams != args.length || paramTypes[nParams - 1] != (args[nArgs - 1] == null ? null : args[nArgs - 1].getClass())) {
			int arraySize = 0; // zero size array if nothing to pass as the varargs parameter
			if (nArgs >= nParams) {
				arraySize = nArgs - (nParams - 1);
			}
			Object[] repackagedArguments = (Object[]) Array.newInstance(paramTypes[nParams - 1].getComponentType(),
					arraySize);

			// Copy all but the varargs arguments
			for (int i = 0; i < arraySize; i++) {
				repackagedArguments[i] = args[nParams + i - 1];
			}
			// Create an array for the varargs arguments
			Object[] newArgs = new Object[nParams];
			for (int i = 0; i < newArgs.length - 1; i++) {
				newArgs[i] = args[i];
			}
			newArgs[newArgs.length - 1] = repackagedArguments;
			return newArgs;
		}
		return args;
	}


	static enum ArgsMatchKind {

		EXACT, CLOSE, REQUIRES_CONVERSION
	}


	/**
	 * An instance of ArgumentsMatchInfo describes what kind of match was achieved between two sets of arguments - the set that a
	 * method/constructor is expecting and the set that are being supplied at the point of invocation. If the kind
	 * indicates that conversion is required for some of the arguments then the arguments that require conversion are
	 * listed in the argsRequiringConversion array.
	 */
	static class ArgumentsMatchInfo {

		public ArgsMatchKind kind;

		public int[] argsRequiringConversion;

		ArgumentsMatchInfo(ArgsMatchKind kind, int[] integers) {
			this.kind = kind;
			argsRequiringConversion = integers;
		}

		ArgumentsMatchInfo(ArgsMatchKind kind) {
			this.kind = kind;
		}
	}

}
