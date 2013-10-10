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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
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
 * @author Phillip Webb
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
	 * @deprecated as of Spring 4.0 use {@link MethodParameter#getGenericParameterType()}
	 */
	@Deprecated
	public static Type getTargetType(MethodParameter methodParam) {
		Assert.notNull(methodParam, "MethodParameter must not be null");
		return methodParam.getGenericParameterType();
	}

	/**
	 * Determine the target type for the given generic parameter type.
	 * @param methodParam the method parameter specification
	 * @param clazz the class to resolve type variables against
	 * @return the corresponding generic parameter or return type
	 */
	public static Class<?> resolveParameterType(MethodParameter methodParam, Class<?> clazz) {
		Assert.notNull(methodParam, "MethodParameter must not be null");
		Assert.notNull(clazz, "Class must not be null");
		methodParam.resolveClass = clazz;
		methodParam.setParameterType(ResolvableType.forMethodParameter(methodParam, clazz).resolve());
		return methodParam.getParameterType();
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
		Assert.notNull(clazz, "Class must not be null");
		return ResolvableType.forMethodReturn(method, clazz).resolve(method.getReturnType());
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
	 * <pre class="code">{@code public static <T> T createProxy(Class<T> clazz)}</pre>
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
		ResolvableType resolvableType = ResolvableType.forMethodReturn(method).as(genericIfc);
		if (!resolvableType.hasGenerics() || resolvableType.getType() instanceof WildcardType) {
			return null;
		}
		return getSingleGeneric(resolvableType);
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
		ResolvableType resolvableType = ResolvableType.forClass(clazz).as(genericIfc);
		if (!resolvableType.hasGenerics()) {
			return null;
		}
		return getSingleGeneric(resolvableType);
	}

	private static Class<?> getSingleGeneric(ResolvableType resolvableType) {
		if (resolvableType.getGenerics().length > 1) {
			throw new IllegalArgumentException("Expected 1 type argument on generic interface [" +
					resolvableType + "] but found " + resolvableType.getGenerics().length);
		}
		return resolvableType.getGeneric().resolve();
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
		ResolvableType type = ResolvableType.forClass(clazz).as(genericIfc);
		if(!type.hasGenerics()) {
			return null;
		}
		return type.resolveGenerics();
	}

	/**
	 * Resolve the specified generic type against the given TypeVariable map.
	 * @param genericType the generic type to resolve
	 * @param typeVariableMap the TypeVariable Map to resolved against
	 * @return the type if it resolves to a Class, or {@code Object.class} otherwise
	 * @deprecated as of Spring 4.0 in favor of {@link ResolvableType}
	 */
	@Deprecated
	public static Class<?> resolveType(Type genericType, Map<TypeVariable, Type> typeVariableMap) {
		TypeVariableResolver variableResolver = new TypeVariableMapResolver(typeVariableMap);
		Class<?> resolved = ResolvableType.forType(genericType, variableResolver).resolve();
		return (resolved == null ? Object.class : resolved);
	}

	/**
	 * Build a mapping of {@link TypeVariable#getName TypeVariable names} to
	 * {@link Class concrete classes} for the specified {@link Class}. Searches
	 * all super types, enclosing types and interfaces.
	 * @deprecated as of Spring 4.0 in favor of {@link ResolvableType}
	 */
	@Deprecated
	public static Map<TypeVariable, Type> getTypeVariableMap(Class<?> clazz) {
		Map<TypeVariable, Type> typeVariableMap = typeVariableCache.get(clazz);
		if (typeVariableMap == null) {
			typeVariableMap = new HashMap<TypeVariable, Type>();
			buildTypeVaraibleMap(ResolvableType.forClass(clazz), typeVariableMap);
			typeVariableCache.put(clazz, Collections.unmodifiableMap(typeVariableMap));
		}
		return typeVariableMap;
	}

	private static void buildTypeVaraibleMap(ResolvableType type,
			Map<TypeVariable, Type> typeVariableMap) {
		if(type != ResolvableType.NONE) {
			if(type.getType() instanceof ParameterizedType) {
				TypeVariable<?>[] variables = type.resolve().getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					ResolvableType generic = type.getGeneric(i);
					while (generic.getType() instanceof TypeVariable<?>) {
						generic = generic.resolveType();
					}
					if (generic != ResolvableType.NONE) {
						typeVariableMap.put(variables[i], generic.getType());
					}
				}
			}
			buildTypeVaraibleMap(type.getSuperType(), typeVariableMap);
			for (ResolvableType interfaceType : type.getInterfaces()) {
				buildTypeVaraibleMap(interfaceType, typeVariableMap);
			}
			if (type.resolve().isMemberClass()) {
				buildTypeVaraibleMap(ResolvableType.forClass(type.resolve().getEnclosingClass()),
						typeVariableMap);
			}
		}
	}

	/**
	 * Determine the raw type for the given generic parameter type.
	 * @param genericType the generic type to resolve
	 * @param typeVariableMap the TypeVariable Map to resolved against
	 * @return the resolved raw type
	 */
	@Deprecated
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
	 * Extracts the bound {@code Type} for a given {@link TypeVariable}.
	 */
	@Deprecated
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


	/**
	 * Adapts a {@code typeVariableMap} to a {@link TypeVariableResolver}.
	 */
	private static class TypeVariableMapResolver implements TypeVariableResolver {

		private Map<TypeVariable, Type> typeVariableMap;


		public TypeVariableMapResolver(Map<TypeVariable, Type> typeVariableMap) {
			Assert.notNull("TypeVariableMap must not be null");
			this.typeVariableMap = typeVariableMap;
		}


		@Override
		public Type resolveVariable(TypeVariable typeVariable) {
			return typeVariableMap.get(typeVariable);
		}

		@Override
		public int hashCode() {
			return typeVariableMap.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if(obj == this) {
				return true;
			}
			if(obj instanceof TypeVariableMapResolver) {
				TypeVariableMapResolver other = (TypeVariableMapResolver) obj;
				return this.typeVariableMap.equals(other.typeVariableMap);
			}
			return false;
		}

	}

}
