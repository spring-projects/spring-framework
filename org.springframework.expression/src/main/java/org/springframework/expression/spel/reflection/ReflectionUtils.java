/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.reflection;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelException;
import org.springframework.expression.spel.SpelMessages;

/**
 * Utility methods used by the reflection resolver code to discover the correct methods/constructors and fields that
 * should be used in expressions.
 * 
 * @author Andy Clement
 */
@SuppressWarnings("unchecked")
public class ReflectionUtils {

	/**
	 * Locate a constructor on a type. There are three kinds of match that might occur:
	 * <ol>
	 * <li>An exact match where the types of the arguments match the types of the constructor
	 * <li>An in-exact match where the types we are looking for are subtypes of those defined on the constructor
	 * <li>A match where we are able to convert the arguments into those expected by the constructor, according to the
	 * registered type converter.
	 * </ol>
	 * 
	 * @param typeConverter a converter that can be used to determine if the supplied arguments can be converted to
	 * expected arguments
	 * @param type the type being searched for a valid constructor
	 * @param argumentTypes the types of the arguments we want the constructor to have
	 * @return a DiscoveredConstructor object or null if non found
	 * @throws SpelException
	 */
	public static DiscoveredMethod findMethod(TypeConverter typeConverter, String name, Class<?>[] argumentTypes,
			Class<?> type, boolean conversionAllowed) throws SpelException {
		Method[] methods = type.getMethods();
		Method closeMatch = null;
		Integer[] argsToConvert = null;
		boolean multipleOptions = false;
		Method matchRequiringConversion = null;
		for (int i = 0; i < methods.length; i++) {
			Method method = methods[i];
			if (method.isBridge()) {
				continue;
			}
			if (method.getName().equals(name)) {
				ArgumentsMatchInfo matchInfo = null;
				if (method.isVarArgs() && argumentTypes.length >= (method.getParameterTypes().length - 1)) {
					// *sigh* complicated
					matchInfo = compareArgumentsVarargs(method.getParameterTypes(), argumentTypes, typeConverter,
							conversionAllowed);
				} else if (method.getParameterTypes().length == argumentTypes.length) {
					// name and parameter number match, check the arguments
					matchInfo = compareArguments(method.getParameterTypes(), argumentTypes, typeConverter,
							conversionAllowed);
				}
				if (matchInfo != null) {
					if (matchInfo.kind == ArgsMatchKind.EXACT) {
						return new DiscoveredMethod(method, null);
					} else if (matchInfo.kind == ArgsMatchKind.CLOSE) {
						closeMatch = method;
					} else if (matchInfo.kind == ArgsMatchKind.REQUIRES_CONVERSION) {
						if (matchRequiringConversion != null) {
							multipleOptions = true;
						}
						argsToConvert = matchInfo.argsRequiringConversion;
						matchRequiringConversion = method;
					}
				}
			}
		}
		if (closeMatch != null) {
			return new DiscoveredMethod(closeMatch, null);
		} else if (matchRequiringConversion != null) {
			if (multipleOptions) {
				throw new SpelException(SpelMessages.MULTIPLE_POSSIBLE_METHODS, name);
			}
			return new DiscoveredMethod(matchRequiringConversion, argsToConvert);
		} else {
			return null;
		}
	}

	/**
	 * Locate a constructor on the type. There are three kinds of match that might occur:
	 * <ol>
	 * <li>An exact match where the types of the arguments match the types of the constructor
	 * <li>An in-exact match where the types we are looking for are subtypes of those defined on the constructor
	 * <li>A match where we are able to convert the arguments into those expected by the constructor, according to the
	 * registered type converter.
	 * </ol>
	 * 
	 * @param typeConverter a converter that can be used to determine if the supplied arguments can be converted to
	 * expected arguments
	 * @param type the type being searched for a valid constructor
	 * @param argumentTypes the types of the arguments we want the constructor to have
	 * @return a DiscoveredConstructor object or null if non found
	 */
	public static DiscoveredConstructor findConstructor(TypeConverter typeConverter, Class<?> type,
			Class<?>[] argumentTypes, boolean conversionAllowed) {
		Constructor[] ctors = type.getConstructors();
		Constructor closeMatch = null;
		Integer[] argsToConvert = null;
		Constructor matchRequiringConversion = null;
		for (int i = 0; i < ctors.length; i++) {
			Constructor ctor = ctors[i];
			if (ctor.isVarArgs() && argumentTypes.length >= (ctor.getParameterTypes().length - 1)) {
				// *sigh* complicated
				// Basically.. we have to have all parameters match up until the varargs one, then the rest of what is
				// being provided should be
				// the same type whilst the final argument to the method must be an array of that (oh, how easy...not) -
				// or the final parameter
				// we are supplied does match exactly (it is an array already).
				ArgumentsMatchInfo matchInfo = compareArgumentsVarargs(ctor.getParameterTypes(), argumentTypes,
						typeConverter, conversionAllowed);
				if (matchInfo != null) {
					if (matchInfo.kind == ArgsMatchKind.EXACT) {
						return new DiscoveredConstructor(ctor, null);
					} else if (matchInfo.kind == ArgsMatchKind.CLOSE) {
						closeMatch = ctor;
					} else if (matchInfo.kind == ArgsMatchKind.REQUIRES_CONVERSION) {
						argsToConvert = matchInfo.argsRequiringConversion;
						matchRequiringConversion = ctor;
					}
				}

			} else if (ctor.getParameterTypes().length == argumentTypes.length) {
				// worth a closer look
				ArgumentsMatchInfo matchInfo = compareArguments(ctor.getParameterTypes(), argumentTypes, typeConverter,
						conversionAllowed);
				if (matchInfo != null) {
					if (matchInfo.kind == ArgsMatchKind.EXACT) {
						return new DiscoveredConstructor(ctor, null);
					} else if (matchInfo.kind == ArgsMatchKind.CLOSE) {
						closeMatch = ctor;
					} else if (matchInfo.kind == ArgsMatchKind.REQUIRES_CONVERSION) {
						argsToConvert = matchInfo.argsRequiringConversion;
						matchRequiringConversion = ctor;
					}
				}
			}
		}
		if (closeMatch != null) {
			return new DiscoveredConstructor(closeMatch, null);
		} else if (matchRequiringConversion != null) {
			return new DiscoveredConstructor(matchRequiringConversion, argsToConvert);
		} else {
			return null;
		}
	}

	/**
	 * Compare argument arrays and return information about whether they match. A supplied type converter and
	 * conversionAllowed flag allow for matches to take into account that a type may be transformed into a different
	 * type by the converter.
	 * 
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @param conversionAllowed if true then allow for what the type converter can do when seeing if a supplied type can
	 * match an expected type
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	private static ArgumentsMatchInfo compareArguments(Class[] expectedArgTypes, Class[] suppliedArgTypes,
			TypeConverter typeConverter, boolean conversionAllowed) {
		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;
		for (int i = 0; i < expectedArgTypes.length && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class expectedArg = expectedArgTypes[i];
			if (expectedArg != suppliedArg) {
				if (expectedArg.isAssignableFrom(suppliedArg) || areBoxingCompatible(expectedArg, suppliedArg)
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
				return new ArgumentsMatchInfo(match, argsRequiringConversion.toArray(new Integer[] {}));
			} else {
				return new ArgumentsMatchInfo(match);
			}
		}
	}

	/**
	 * Compare argument arrays and return information about whether they match. A supplied type converter and
	 * conversionAllowed flag allow for matches to take into account that a type may be transformed into a different
	 * type by the converter. This variant of compareArguments allows for a varargs match.
	 * 
	 * @param expectedArgTypes the array of types the method/constructor is expecting
	 * @param suppliedArgTypes the array of types that are being supplied at the point of invocation
	 * @param typeConverter a registered type converter
	 * @param conversionAllowed if true then allow for what the type converter can do when seeing if a supplied type can
	 * match an expected type
	 * @return a MatchInfo object indicating what kind of match it was or null if it was not a match
	 */
	private static ArgumentsMatchInfo compareArgumentsVarargs(Class[] expectedArgTypes, Class[] suppliedArgTypes,
			TypeConverter typeConverter, boolean conversionAllowed) {
		ArgsMatchKind match = ArgsMatchKind.EXACT;
		List<Integer> argsRequiringConversion = null;

		// Check up until the varargs argument:

		// Deal with the arguments up to 'expected number' - 1
		for (int i = 0; i < expectedArgTypes.length - 1 && match != null; i++) {
			Class suppliedArg = suppliedArgTypes[i];
			Class expectedArg = expectedArgTypes[i];
			if (expectedArg != suppliedArg) {
				if (expectedArg.isAssignableFrom(suppliedArg) || areBoxingCompatible(expectedArg, suppliedArg)
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
					if (varargsParameterType.isAssignableFrom(suppliedArg)
							|| areBoxingCompatible(varargsParameterType, suppliedArg)
					/* || isWidenableTo(expectedArg, suppliedArg) */) {
						if (match != ArgsMatchKind.REQUIRES_CONVERSION) {
							match = ArgsMatchKind.CLOSE;
						}
					} else if (typeConverter.canConvert(suppliedArg, varargsParameterType)) {
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
		}

		if (match == null) {
			return null;
		} else {
			if (match == ArgsMatchKind.REQUIRES_CONVERSION) {
				return new ArgumentsMatchInfo(match, argsRequiringConversion.toArray(new Integer[] {}));
			} else {
				return new ArgumentsMatchInfo(match);
			}
		}
	}

	// TODO optimize implementation of areBoxingCompatible
	private static boolean areBoxingCompatible(Class class1, Class class2) {
		if (class1 == Integer.class && class2 == Integer.TYPE)
			return true;
		if (class1 == Float.class && class2 == Float.TYPE)
			return true;
		if (class1 == Double.class && class2 == Double.TYPE)
			return true;
		if (class1 == Short.class && class2 == Short.TYPE)
			return true;
		if (class1 == Long.class && class2 == Long.TYPE)
			return true;
		if (class1 == Boolean.class && class2 == Boolean.TYPE)
			return true;
		if (class1 == Character.class && class2 == Character.TYPE)
			return true;
		if (class1 == Byte.class && class2 == Byte.TYPE)
			return true;
		if (class2 == Integer.class && class1 == Integer.TYPE)
			return true;
		if (class2 == Float.class && class1 == Float.TYPE)
			return true;
		if (class2 == Double.class && class1 == Double.TYPE)
			return true;
		if (class2 == Short.class && class1 == Short.TYPE)
			return true;
		if (class2 == Long.class && class1 == Long.TYPE)
			return true;
		if (class2 == Boolean.class && class1 == Boolean.TYPE)
			return true;
		if (class2 == Character.class && class1 == Character.TYPE)
			return true;
		if (class2 == Byte.class && class1 == Byte.TYPE)
			return true;
		return false;
	}

	/**
	 * Find a field of a certain name on a specified class
	 */
	public final static Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
		Field[] fields = clazz.getFields(); // TODO use getDeclaredFields() and search up hierarchy?
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			if (field.getName().equals(name) && (mustBeStatic ? Modifier.isStatic(field.getModifiers()) : true)) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Find a getter method for the specified property. A getter is defined as a method whose name start with the prefix
	 * 'get' and the rest of the name is the same as the property name (with the first character uppercased).
	 */
	public static Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method[] ms = clazz.getMethods();// TODO use getDeclaredMethods() and search up hierarchy?
		StringBuilder sb = new StringBuilder();
		sb.append("get").append(propertyName.substring(0, 1).toUpperCase()).append(propertyName.substring(1));
		String expectedGetterName = sb.toString();
		for (int i = 0; i < ms.length; i++) {
			Method method = ms[i];
			if (method.getParameterTypes().length == 0
					&& (mustBeStatic ? Modifier.isStatic(method.getModifiers()) : true)
					&& method.getName().equals(expectedGetterName)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Find a setter method for the specified property
	 */
	public static Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method[] ms = clazz.getMethods(); // TODO use getDeclaredMethods() and search up hierarchy?
		StringBuilder sb = new StringBuilder();
		sb.append("set").append(propertyName.substring(0, 1).toUpperCase()).append(propertyName.substring(1));
		String setterName = sb.toString();
		for (int i = 0; i < ms.length; i++) {
			Method method = ms[i];
			if (method.getParameterTypes().length == 1
					&& (mustBeStatic ? Modifier.isStatic(method.getModifiers()) : true)
					&& method.getName().equals(setterName)) {
				return method;
			}
		}
		return null;
	}

	/**
	 * An instance of MatchInfo describes what kind of match was achieved between two sets of arguments - the set that a
	 * method/constructor is expecting and the set that are being supplied at the point of invocation. If the kind
	 * indicates that conversion is required for some of the arguments then the arguments that require conversion are
	 * listed in the argsRequiringConversion array.
	 * 
	 */
	private static class ArgumentsMatchInfo {
		ArgsMatchKind kind;
		Integer[] argsRequiringConversion;

		ArgumentsMatchInfo(ArgsMatchKind kind, Integer[] integers) {
			this.kind = kind;
			argsRequiringConversion = integers;
		}

		ArgumentsMatchInfo(ArgsMatchKind kind) {
			this.kind = kind;
		}
	}

	private static enum ArgsMatchKind {
		EXACT, CLOSE, REQUIRES_CONVERSION;
	}

	/**
	 * When a match is found searching for a particular constructor, this object captures the constructor object and
	 * details of which arguments require conversion for the call to be allowed.
	 */
	public static class DiscoveredConstructor {
		public Constructor theConstructor;
		public Integer[] argumentsRequiringConversion;

		public DiscoveredConstructor(Constructor theConstructor, Integer[] argsToConvert) {
			this.theConstructor = theConstructor;
			argumentsRequiringConversion = argsToConvert;
		}
	}

	/**
	 * When a match is found searching for a particular method, this object captures the method object and details of
	 * which arguments require conversion for the call to be allowed.
	 */
	public static class DiscoveredMethod {
		public Method theMethod;
		public Integer[] argumentsRequiringConversion;

		public DiscoveredMethod(Method theMethod, Integer[] argsToConvert) {
			this.theMethod = theMethod;
			argumentsRequiringConversion = argsToConvert;
		}
	}

	static void convertArguments(Class[] parameterTypes, boolean isVarargs, TypeConverter converter,
			Integer[] argsRequiringConversion, Object... arguments) throws EvaluationException {
		Class varargsType = null;
		if (isVarargs) {
			varargsType = parameterTypes[parameterTypes.length - 1].getComponentType();
		}
		for (int i = 0; i < argsRequiringConversion.length; i++) {
			int argPosition = argsRequiringConversion[i];
			Class targetType = null;
			if (isVarargs && argPosition >= (parameterTypes.length - 1)) {
				targetType = varargsType;
			} else {
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
			Class targetType = null;
			if (isVarargs && i >= (parameterTypes.length - 1)) {
				targetType = varargsType;
			} else {
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
			} catch (EvaluationException e) {
				// allows for another type converter throwing a different kind of EvaluationException
				if (!(e instanceof SpelException)) {
					throw new SpelException(e, SpelMessages.PROBLEM_DURING_TYPE_CONVERSION,
							"Converter failed to convert '" + arguments[i].getClass().getName() + "' to type '"
									+ targetType + "'");
				}
				throw e;
			}
		}
	}

	/**
	 * Package up the arguments so that they correctly match what is expected in parameterTypes. For example, if
	 * parameterTypes is (int, String[]) because the second parameter was declared String... then if arguments is
	 * [1,"a","b"] then it must be repackaged as [1,new String[]{"a","b"}] in order to match the expected
	 * parameterTypes.
	 * 
	 * @param parameterTypes the types of the parameters for the invocation
	 * @param arguments the arguments to be setup ready for the invocation
	 * @return a repackaged array of arguments where any varargs setup has been done
	 */
	public static Object[] setupArgumentsForVarargsInvocation(Class[] parameterTypes, Object... arguments) {
		// Check if array already built for final argument
		int nParams = parameterTypes.length;
		int nArgs = arguments.length;

		// Check if repackaging is needed:
		if (nParams != arguments.length
				|| parameterTypes[nParams - 1] != (arguments[nArgs - 1] == null ? null : arguments[nArgs - 1]
						.getClass())) {
			int arraySize = 0; // zero size array if nothing to pass as the varargs parameter
			if (arguments != null && nArgs >= nParams) {
				arraySize = nArgs - (nParams - 1);
			}
			Object[] repackagedArguments = (Object[]) Array.newInstance(parameterTypes[nParams - 1].getComponentType(),
					arraySize);

			// Copy all but the varargs arguments
			for (int i = 0; i < arraySize; i++) {
				repackagedArguments[i] = arguments[nParams + i - 1];
			}
			// Create an array for the varargs arguments
			Object[] newArgs = new Object[nParams];
			for (int i = 0; i < newArgs.length - 1; i++) {
				newArgs[i] = arguments[i];
			}
			newArgs[newArgs.length - 1] = repackagedArguments;
			return newArgs;
		}
		return arguments;
	}

	public static Object[] prepareArguments(TypeConverter converter, Method m, Object[] arguments)
			throws EvaluationException {
		if (arguments != null) {
			ReflectionUtils.convertArguments(m.getParameterTypes(), m.isVarArgs(), converter, arguments);
		}
		if (m.isVarArgs()) {
			arguments = ReflectionUtils.setupArgumentsForVarargsInvocation(m.getParameterTypes(), arguments);
		}
		return arguments;
	}

}
