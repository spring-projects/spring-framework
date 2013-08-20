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

package org.springframework.core;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Helper class for resolving generic types against type variables.
 *
 * <p>Mainly intended for usage within the framework, resolving method
 * parameter types even when they are declared generically.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.5.2
 * @see GenericCollectionTypeResolver
 */
public abstract class GenericTypeResolver {

	/** Cache from Class to TypeVariable Map */
	private static final Map<Class, Map<TypeVariable, Type>> typeVariableCache =
			new ConcurrentReferenceHashMap<Class, Map<TypeVariable,Type>>();


	/**
	 * Determine the target type for the given parameter specification.
	 * @param methodParam the method parameter specification
	 * @return the corresponding generic parameter type
	 */
	public static Type getTargetType(MethodParameter methodParam) {
		Assert.notNull(methodParam, "MethodParameter must not be null");
		if (methodParam.getConstructor() != null) {
			return methodParam.getConstructor().getGenericParameterTypes()[methodParam.getParameterIndex()];
		}
		else {
			if (methodParam.getParameterIndex() >= 0) {
				return methodParam.getMethod().getGenericParameterTypes()[methodParam.getParameterIndex()];
			}
			else {
				return methodParam.getMethod().getGenericReturnType();
			}
		}
	}

	/**
	 * Determine the target type for the given generic parameter type.
	 * @param methodParam the method parameter specification
	 * @param clazz the class to resolve type variables against
	 * @return the corresponding generic parameter or return type
	 */
	public static Class<?> resolveParameterType(MethodParameter methodParam, Class<?> clazz) {
		Type genericType = getTargetType(methodParam);
		Assert.notNull(clazz, "Class must not be null");
		Map<TypeVariable, Type> typeVariableMap = getTypeVariableMap(clazz);
		Type rawType = getRawType(genericType, typeVariableMap);
		Class<?> result = (rawType instanceof Class ? (Class) rawType : methodParam.getParameterType());
		methodParam.setParameterType(result);
		methodParam.typeVariableMap = typeVariableMap;
		return result;
	}

	/**
	 * Determine the target type for the generic return type of the given method,
	 * where formal type variables are declared on the given class.
	 * @param method the method to introspect
	 * @param clazz the class to resolve type variables against
	 * @return the corresponding generic parameter or return type
	 * @see #resolveReturnTypeForGenericMethod
	 */
	public static Class<?> resolveReturnType(Method method, Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		Type genericType = method.getGenericReturnType();
		Assert.notNull(clazz, "Class must not be null");
		Map<TypeVariable, Type> typeVariableMap = getTypeVariableMap(clazz);
		Type rawType = getRawType(genericType, typeVariableMap);
		return (rawType instanceof Class ? (Class<?>) rawType : method.getReturnType());
	}

	/**
	 * Determine the target type for the generic return type of the given
	 * <em>generic method</em>, where formal type variables are declared on
	 * the given method itself.
	 * <p>For example, given a factory method with the following signature,
	 * if {@code resolveReturnTypeForGenericMethod()} is invoked with the reflected
	 * method for {@code creatProxy()} and an {@code Object[]} array containing
	 * {@code MyService.class}, {@code resolveReturnTypeForGenericMethod()} will
	 * infer that the target return type is {@code MyService}.
	 * <pre>{@code public static <T> T createProxy(Class<T> clazz)}</pre>
	 * <h4>Possible Return Values</h4>
	 * <ul>
	 * <li>the target return type, if it can be inferred</li>
	 * <li>the {@linkplain Method#getReturnType() standard return type}, if
	 * the given {@code method} does not declare any {@linkplain
	 * Method#getTypeParameters() formal type variables}</li>
	 * <li>the {@linkplain Method#getReturnType() standard return type}, if the
	 * target return type cannot be inferred (e.g., due to type erasure)</li>
	 * <li>{@code null}, if the length of the given arguments array is shorter
	 * than the length of the {@linkplain
	 * Method#getGenericParameterTypes() formal argument list} for the given
	 * method</li>
	 * </ul>
	 * @param method the method to introspect, never {@code null}
	 * @param args the arguments that will be supplied to the method when it is
	 * invoked, never {@code null}
	 * @return the resolved target return type, the standard return type, or {@code null}
	 * @since 3.2
	 * @see #resolveReturnType
	 */
	public static Class<?> resolveReturnTypeForGenericMethod(Method method, Object[] args) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(args, "Argument array must not be null");

		TypeVariable<Method>[] declaredTypeVariables = method.getTypeParameters();
		Type genericReturnType = method.getGenericReturnType();
		Type[] methodArgumentTypes = method.getGenericParameterTypes();

		// No declared type variables to inspect, so just return the standard return type.
		if (declaredTypeVariables.length == 0) {
			return method.getReturnType();
		}

		// The supplied argument list is too short for the method's signature, so
		// return null, since such a method invocation would fail.
		if (args.length < methodArgumentTypes.length) {
			return null;
		}

		// Ensure that the type variable (e.g., T) is declared directly on the method
		// itself (e.g., via <T>), not on the enclosing class or interface.
		boolean locallyDeclaredTypeVariableMatchesReturnType = false;
		for (TypeVariable<Method> currentTypeVariable : declaredTypeVariables) {
			if (currentTypeVariable.equals(genericReturnType)) {
				locallyDeclaredTypeVariableMatchesReturnType = true;
				break;
			}
		}

		if (locallyDeclaredTypeVariableMatchesReturnType) {
			for (int i = 0; i < methodArgumentTypes.length; i++) {
				Type currentMethodArgumentType = methodArgumentTypes[i];
				if (currentMethodArgumentType.equals(genericReturnType)) {
					return args[i].getClass();
				}
				if (currentMethodArgumentType instanceof ParameterizedType) {
					ParameterizedType parameterizedType = (ParameterizedType) currentMethodArgumentType;
					Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
					for (Type typeArg : actualTypeArguments) {
						if (typeArg.equals(genericReturnType)) {
							if (args[i] instanceof Class) {
								return (Class<?>) args[i];
							}
							else {
								// Consider adding logic to determine the class of the typeArg, if possible.
								// For now, just fall back...
								return method.getReturnType();
							}
						}
					}
				}
			}
		}

		// Fall back...
		return method.getReturnType();
	}

	/**
	 * Resolve the single type argument of the given generic interface against the given
	 * target method which is assumed to return the given interface or an implementation
	 * of it.
	 * @param method the target method to check the return type of
	 * @param genericIfc the generic interface or superclass to resolve the type argument from
	 * @return the resolved parameter type of the method return type, or {@code null}
	 * if not resolvable or if the single argument is of type {@link WildcardType}.
	 */
	public static Class<?> resolveReturnTypeArgument(Method method, Class<?> genericIfc) {
		Assert.notNull(method, "method must not be null");
		Type returnType = method.getReturnType();
		Type genericReturnType = method.getGenericReturnType();
		if (returnType.equals(genericIfc)) {
			if (genericReturnType instanceof ParameterizedType) {
				ParameterizedType targetType = (ParameterizedType) genericReturnType;
				Type[] actualTypeArguments = targetType.getActualTypeArguments();
				Type typeArg = actualTypeArguments[0];
				if (!(typeArg instanceof WildcardType)) {
					return (Class<?>) typeArg;
				}
			}
			else {
				return null;
			}
		}
		return resolveTypeArgument((Class<?>) returnType, genericIfc);
	}

	/**
	 * Resolve the single type argument of the given generic interface against
	 * the given target class which is assumed to implement the generic interface
	 * and possibly declare a concrete type for its type variable.
	 * @param clazz the target class to check against
	 * @param genericIfc the generic interface or superclass to resolve the type argument from
	 * @return the resolved type of the argument, or {@code null} if not resolvable
	 */
	public static Class<?> resolveTypeArgument(Class<?> clazz, Class<?> genericIfc) {
		Class[] typeArgs = resolveTypeArguments(clazz, genericIfc);
		if (typeArgs == null) {
			return null;
		}
		if (typeArgs.length != 1) {
			throw new IllegalArgumentException("Expected 1 type argument on generic interface [" +
					genericIfc.getName() + "] but found " + typeArgs.length);
		}
		return typeArgs[0];
	}

	/**
	 * Resolve the type arguments of the given generic interface against the given
	 * target class which is assumed to implement the generic interface and possibly
	 * declare concrete types for its type variables.
	 * @param clazz the target class to check against
	 * @param genericIfc the generic interface or superclass to resolve the type argument from
	 * @return the resolved type of each argument, with the array size matching the
	 * number of actual type arguments, or {@code null} if not resolvable
	 */
	public static Class[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc) {
		return doResolveTypeArguments(clazz, clazz, genericIfc);
	}

	private static Class[] doResolveTypeArguments(Class<?> ownerClass, Class<?> classToIntrospect, Class<?> genericIfc) {
		while (classToIntrospect != null) {
			if (genericIfc.isInterface()) {
				Type[] ifcs = classToIntrospect.getGenericInterfaces();
				for (Type ifc : ifcs) {
					Class[] result = doResolveTypeArguments(ownerClass, ifc, genericIfc);
					if (result != null) {
						return result;
					}
				}
			}
			else {
				try {
					Class[] result = doResolveTypeArguments(ownerClass, classToIntrospect.getGenericSuperclass(), genericIfc);
					if (result != null) {
						return result;
					}
				}
				catch (MalformedParameterizedTypeException ex) {
					// from getGenericSuperclass() - return null to skip further superclass traversal
					return null;
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return null;
	}

	private static Class[] doResolveTypeArguments(Class<?> ownerClass, Type ifc, Class<?> genericIfc) {
		if (ifc instanceof ParameterizedType) {
			ParameterizedType paramIfc = (ParameterizedType) ifc;
			Type rawType = paramIfc.getRawType();
			if (genericIfc.equals(rawType)) {
				Type[] typeArgs = paramIfc.getActualTypeArguments();
				Class[] result = new Class[typeArgs.length];
				for (int i = 0; i < typeArgs.length; i++) {
					Type arg = typeArgs[i];
					result[i] = extractClass(ownerClass, arg);
				}
				return result;
			}
			else if (genericIfc.isAssignableFrom((Class) rawType)) {
				return doResolveTypeArguments(ownerClass, (Class) rawType, genericIfc);
			}
		}
		else if (ifc != null && genericIfc.isAssignableFrom((Class) ifc)) {
			return doResolveTypeArguments(ownerClass, (Class) ifc, genericIfc);
		}
		return null;
	}

	/**
	 * Extract a Class from the given Type.
	 */
	private static Class<?> extractClass(Class<?> ownerClass, Type arg) {
		if (arg instanceof ParameterizedType) {
			return extractClass(ownerClass, ((ParameterizedType) arg).getRawType());
		}
		else if (arg instanceof GenericArrayType) {
			GenericArrayType gat = (GenericArrayType) arg;
			Type gt = gat.getGenericComponentType();
			Class<?> componentClass = extractClass(ownerClass, gt);
			return Array.newInstance(componentClass, 0).getClass();
		}
		else if (arg instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) arg;
			arg = getTypeVariableMap(ownerClass).get(tv);
			if (arg == null) {
				arg = extractBoundForTypeVariable(tv);
				if (arg instanceof ParameterizedType) {
					return extractClass(ownerClass, ((ParameterizedType) arg).getRawType());
				}
			}
			else {
				return extractClass(ownerClass, arg);
			}
		}
		return (arg instanceof Class ? (Class) arg : Object.class);
	}

	/**
	 * Resolve the specified generic type against the given TypeVariable map.
	 * @param genericType the generic type to resolve
	 * @param typeVariableMap the TypeVariable Map to resolved against
	 * @return the type if it resolves to a Class, or {@code Object.class} otherwise
	 */
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> typeVariableMap) {
		Type resolvedType = getRawType(genericType, typeVariableMap);
		if (resolvedType instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) resolvedType).getGenericComponentType();
			Class<?> componentClass = resolveType(componentType, typeVariableMap);
			resolvedType = Array.newInstance(componentClass, 0).getClass();
		}
		return (resolvedType instanceof Class ? (Class) resolvedType : Object.class);
	}

	/**
	 * Determine the raw type for the given generic parameter type.
	 * @param genericType the generic type to resolve
	 * @param typeVariableMap the TypeVariable Map to resolved against
	 * @return the resolved raw type
	 */
	static Type getRawType(Type genericType, Map<TypeVariable, Type> typeVariableMap) {
		Type resolvedType = genericType;
		if (genericType instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) genericType;
			resolvedType = typeVariableMap.get(tv);
			if (resolvedType == null) {
				resolvedType = extractBoundForTypeVariable(tv);
			}
		}
		if (resolvedType instanceof ParameterizedType) {
			return ((ParameterizedType) resolvedType).getRawType();
		}
		else {
			return resolvedType;
		}
	}

	/**
	 * Build a mapping of {@link TypeVariable#getName TypeVariable names} to
	 * {@link Class concrete classes} for the specified {@link Class}. Searches
	 * all super types, enclosing types and interfaces.
	 */
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);

		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<TypeVariable, Type>();

			// interfaces
			extractTypeVariablesFromGenericInterfaces(clazz.getGenericInterfaces(), typeVariableMap);

			try {
				// super class
				Class<?> type = clazz;
				while (type.getSuperclass() != null && !Object.class.equals(type.getSuperclass())) {
					Type genericType = type.getGenericSuperclass();
					if (genericType instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) genericType;
						populateTypeMapFromParameterizedType(pt, typeVariableMap);
					}
					extractTypeVariablesFromGenericInterfaces(type.getSuperclass().getGenericInterfaces(), typeVariableMap);
					type = type.getSuperclass();
				}
			}
			catch (MalformedParameterizedTypeException ex) {
				// from getGenericSuperclass() - ignore and continue with member class check
			}

			try {
				// enclosing class
				Class<?> type = clazz;
				while (type.isMemberClass()) {
					Type genericType = type.getGenericSuperclass();
					if (genericType instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) genericType;
						populateTypeMapFromParameterizedType(pt, typeVariableMap);
					}
					type = type.getEnclosingClass();
				}
			}
			catch (MalformedParameterizedTypeException ex) {
				// from getGenericSuperclass() - ignore and preserve previously accumulated type variables
			}

			typeVariableCache.put(clazz, typeVariableMap);
		}

		return typeVariableMap;
	}

	/**
	 * Extracts the bound {@code Type} for a given {@link TypeVariable}.
	 */
	static Type extractBoundForTypeVariable(TypeVariable typeVariable) {
		Type[] bounds = typeVariable.getBounds();
		if (bounds.length == 0) {
			return Object.class;
		}
		Type bound = bounds[0];
		if (bound instanceof TypeVariable) {
			bound = extractBoundForTypeVariable((TypeVariable) bound);
		}
		return bound;
	}

	private static void extractTypeVariablesFromGenericInterfaces(Type[] genericInterfaces, Map<TypeVariable, Type> typeVariableMap) {
		for (Type genericInterface : genericInterfaces) {
			if (genericInterface instanceof ParameterizedType) {
				ParameterizedType pt = (ParameterizedType) genericInterface;
				populateTypeMapFromParameterizedType(pt, typeVariableMap);
				if (pt.getRawType() instanceof Class) {
					extractTypeVariablesFromGenericInterfaces(
							((Class) pt.getRawType()).getGenericInterfaces(), typeVariableMap);
				}
			}
			else if (genericInterface instanceof Class) {
				extractTypeVariablesFromGenericInterfaces(
						((Class) genericInterface).getGenericInterfaces(), typeVariableMap);
			}
		}
	}

	/**
	 * Read the {@link TypeVariable TypeVariables} from the supplied {@link ParameterizedType}
	 * and add mappings corresponding to the {@link TypeVariable#getName TypeVariable name} ->
	 * concrete type to the supplied {@link Map}.
	 * <p>Consider this case:
	 * <pre class="code>
	 * public interface Foo<S, T> {
	 *  ..
	 * }
	 *
	 * public class FooImpl implements Foo<String, Integer> {
	 *  ..
	 * }</pre>
	 * For '{@code FooImpl}' the following mappings would be added to the {@link Map}:
	 * {S=java.lang.String, T=java.lang.Integer}.
	 */
	private static void populateTypeMapFromParameterizedType(ParameterizedType type, Map<TypeVariable, Type> typeVariableMap) {
		if (type.getRawType() instanceof Class) {
			Type[] actualTypeArguments = type.getActualTypeArguments();
			TypeVariable[] typeVariables = ((Class) type.getRawType()).getTypeParameters();
			for (int i = 0; i < actualTypeArguments.length; i++) {
				Type actualTypeArgument = actualTypeArguments[i];
				TypeVariable variable = typeVariables[i];
				if (actualTypeArgument instanceof Class) {
					typeVariableMap.put(variable, actualTypeArgument);
				}
				else if (actualTypeArgument instanceof GenericArrayType) {
					typeVariableMap.put(variable, actualTypeArgument);
				}
				else if (actualTypeArgument instanceof ParameterizedType) {
					typeVariableMap.put(variable, actualTypeArgument);
				}
				else if (actualTypeArgument instanceof TypeVariable) {
					// We have a type that is parameterized at instantiation time
					// the nearest match on the bridge method will be the bounded type.
					TypeVariable typeVariableArgument = (TypeVariable) actualTypeArgument;
					Type resolvedType = typeVariableMap.get(typeVariableArgument);
					if (resolvedType == null) {
						resolvedType = extractBoundForTypeVariable(typeVariableArgument);
					}
					typeVariableMap.put(variable, resolvedType);
				}
			}
		}
	}

}
