/*
 * Copyright 2002-2008 the original author or authors.
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

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.springframework.util.Assert;

/**
 * Helper class for resolving generic types against type variables.
 *
 * <p>Mainly intended for usage within the framework, resolving method
 * parameter types even when they are declared generically.
 *
 * <p>Only usable on Java 5. Use an appropriate JdkVersion check before
 * calling this class, if a fallback for JDK 1.4 is desirable.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 2.5.2
 * @see GenericCollectionTypeResolver
 * @see JdkVersion
 */
public abstract class GenericTypeResolver {

	/** Cache from Class to TypeVariable Map */
	private static final Map typeVariableCache = Collections.synchronizedMap(new WeakHashMap());


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
	public static Class resolveParameterType(MethodParameter methodParam, Class clazz) {
		Type genericType = getTargetType(methodParam);
		Assert.notNull(clazz, "Class must not be null");
		Map typeVariableMap = getTypeVariableMap(clazz);
		Type rawType = getRawType(genericType, typeVariableMap);
		Class result = (rawType instanceof Class ? (Class) rawType : methodParam.getParameterType());
		methodParam.setParameterType(result);
		methodParam.typeVariableMap = typeVariableMap;
		return result;
	}

	/**
	 * Determine the target type for the generic return type of the given method.
	 * @param method the method to introspect
	 * @param clazz the class to resolve type variables against
	 * @return the corresponding generic parameter or return type
	 */
	public static Class resolveReturnType(Method method, Class clazz) {
		Assert.notNull(method, "Method must not be null");
		Type genericType = method.getGenericReturnType();
		Assert.notNull(clazz, "Class must not be null");
		Map typeVariableMap = getTypeVariableMap(clazz);
		Type rawType = getRawType(genericType, typeVariableMap);
		return (rawType instanceof Class ? (Class) rawType : method.getReturnType());
	}


	/**
	 * Resolve the specified generic type against the given TypeVariable map.
	 * @param genericType the generic type to resolve
	 * @param typeVariableMap the TypeVariable Map to resolved against
	 * @return the type if it resolves to a Class, or <code>Object.class</code> otherwise
	 */
	static Class resolveType(Type genericType, Map typeVariableMap) {
		Type rawType = getRawType(genericType, typeVariableMap);
		return (rawType instanceof Class ? (Class) rawType : Object.class);
	}

	/**
	 * Determine the raw type for the given generic parameter type.
	 * @param genericType the generic type to resolve
	 * @param typeVariableMap the TypeVariable Map to resolved against
	 * @return the resolved raw type
	 */
	static Type getRawType(Type genericType, Map typeVariableMap) {
		Type resolvedType = genericType;
		if (genericType instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) genericType;
			resolvedType = (Type) typeVariableMap.get(tv);
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
	 * Build a mapping of {@link TypeVariable#getName TypeVariable names} to concrete
	 * {@link Class} for the specified {@link Class}. Searches all super types,
	 * enclosing types and interfaces.
	 */
	static Map getTypeVariableMap(Class clazz) {
		Map typeVariableMap = (Map) typeVariableCache.get(clazz);

		if (typeVariableMap == null) {
			typeVariableMap = new HashMap();

			// interfaces
			extractTypeVariablesFromGenericInterfaces(clazz.getGenericInterfaces(), typeVariableMap);

			// super class
			Type genericType = clazz.getGenericSuperclass();
			Class type = clazz.getSuperclass();
			while (type != null && !Object.class.equals(type)) {
				if (genericType instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) genericType;
					populateTypeMapFromParameterizedType(pt, typeVariableMap);
				}
				extractTypeVariablesFromGenericInterfaces(type.getGenericInterfaces(), typeVariableMap);
				genericType = type.getGenericSuperclass();
				type = type.getSuperclass();
			}

			// enclosing class
			type = clazz;
			while (type.isMemberClass()) {
				genericType = type.getGenericSuperclass();
				if (genericType instanceof ParameterizedType) {
					ParameterizedType pt = (ParameterizedType) genericType;
					populateTypeMapFromParameterizedType(pt, typeVariableMap);
				}
				type = type.getEnclosingClass();
			}

			typeVariableCache.put(clazz, typeVariableMap);
		}

		return typeVariableMap;
	}

	/**
	 * Extracts the bound <code>Type</code> for a given {@link TypeVariable}.
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

	private static void extractTypeVariablesFromGenericInterfaces(Type[] genericInterfaces, Map typeVariableMap) {
		for (int i = 0; i < genericInterfaces.length; i++) {
			Type genericInterface = genericInterfaces[i];
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
	 * For '<code>FooImpl</code>' the following mappings would be added to the {@link Map}:
	 * {S=java.lang.String, T=java.lang.Integer}.
	 */
	private static void populateTypeMapFromParameterizedType(ParameterizedType type, Map typeVariableMap) {
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
					Type resolvedType = (Type) typeVariableMap.get(typeVariableArgument);
					if (resolvedType == null) {
						resolvedType = extractBoundForTypeVariable(typeVariableArgument);
					}
					typeVariableMap.put(variable, resolvedType);
				}
			}
		}
	}

}
