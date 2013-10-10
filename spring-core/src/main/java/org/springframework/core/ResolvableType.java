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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Encapsulates a Java {@link java.lang.reflect.Type}, providing access to
 * {@link #getSuperType() supertypes} , {@link #getInterfaces() interfaces} and
 * {@link #getGeneric(int...) generic parameters} along with the ability to ultimately
 * {@link #resolve() resolve} to a {@link java.lang.Class}.
 *
 * <p>{@code ResolvableTypes} may be obtained from {@link #forField(Field) fields},
 * {@link #forMethodParameter(Method, int) method parameters},
 * {@link #forMethodReturn(Method) method returns}, {@link #forClass(Class) classes} or
 * directly from a {@link #forType(Type) java.lang.reflect.Type}. Most methods on this class
 * will themselves return {@link ResolvableType}s, allowing easy navigation. For example:
 * <pre class="code">
 * private HashMap&lt;Integer, List&lt;String&gt;&gt; myMap;
 *
 * public void example() {
 *     ResolvableType t = ResolvableType.forField(getClass().getDeclaredField("myMap"));
 *     t.getSuperType(); // AbstractMap&lt;Integer, List&lt;String&gt;&gt;
 *     t.asMap(); // Map&lt;Integer, List&lt;String&gt;&gt;
 *     t.getGeneric(0).resolve(); // Integer
 *     t.getGeneric(1).resolve(); // List
 *     t.getGeneric(1); // List&lt;String&gt;
 *     t.resolveGeneric(1, 0); // String
 * }
 * </pre>
 *
 * @author Phillip Webb
 * @since 4.0
 * @see TypeVariableResolver
 * @see #forField(Field)
 * @see #forMethodParameter(Method, int)
 * @see #forMethodReturn(Method)
 * @see #forConstructorParameter(Constructor, int)
 * @see #forClass(Class)
 * @see #forType(Type)
 */
public final class ResolvableType implements TypeVariableResolver {

	private static ConcurrentReferenceHashMap<ResolvableType, ResolvableType> cache =
			new ConcurrentReferenceHashMap<ResolvableType, ResolvableType>();


	/**
	 * {@code ResolvableType} returned when no value is available. {@code NONE} is used
	 * in preference to {@code null} so that multiple method calls can be safely chained.
	 */
	public static final ResolvableType NONE = new ResolvableType(null, null);


	private static final ResolvableType[] EMPTY_TYPES_ARRAY = new ResolvableType[0];


	/**
	 * The underlying java type being managed (only ever {@code null} for {@link #NONE})
	 */
	private final Type type;

	/**
	 * The {@link TypeVariableResolver} to use or {@code null} if no resolver is availble.
	 */
	private final TypeVariableResolver variableResolver;

	/**
	 * Stored copy of the resolved value or {@code null} if the resolve method has not
	 * yet been called. {@code void.class} is used when the resolve method failed.
	 */
	private Class<?> resolved;


	/**
	 * Private constructor used to create a new {@link ResolvableType}.
	 * @param type the underlying java type (may only be {@code null} for {@link #NONE})
	 * @param variableResolver the resolver used for {@link TypeVariable}s (may be {@code null})
	 */
	private ResolvableType(Type type, TypeVariableResolver variableResolver) {
		this.type = type;
		this.variableResolver = variableResolver;
	}


	/**
	 * Return the underling java {@link Type} being managed. With the exception of
	 * the {@link #NONE} constant, this method will never return {@code null}.
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * Determines if this {@code ResolvableType} is assignable from the specified
	 * {@code type}. Attempts to follow the same rules as the Java compiler, considering
	 * if both the {@link #resolve() resolved} {@code Class} is
	 * {@link Class#isAssignableFrom(Class) assignable from} the given {@code type} as
	 * well as if all {@link #getGenerics() generics} are assignable.
	 * @param type the type to be checked
	 * @return {@code true} if the specified {@code type} can be assigned to this
	 *         {@code type}.
	 */
	public boolean isAssignableFrom(ResolvableType type) {
		return isAssignableFrom(type, false);
	}

	private boolean isAssignableFrom(ResolvableType type, boolean checkingGeneric) {
		Assert.notNull(type, "Type must not be null");

		// If we cannot resolve types, we are not assignable
		if (this == NONE || type == NONE) {
			return false;
		}

		// Deal with array by delegating to the component type
		if (isArray()) {
			return (type.isArray() && getComponentType().isAssignableFrom(
					type.getComponentType()));
		}

		// Deal with wildcard bounds
		WildcardBounds ourBounds = WildcardBounds.get(this);
		WildcardBounds typeBounds = WildcardBounds.get(type);

		// in the from X is assignable to <? extends Number>
		if (typeBounds != null) {
			return (ourBounds != null && ourBounds.isSameKind(typeBounds)
					&& ourBounds.isAssignableFrom(typeBounds.getBounds()));
		}

		// in the form <? extends Number> is assignable to X ...
		if (ourBounds != null) {
			return ourBounds.isAssignableFrom(type);
		}

		// Main assignability check
		boolean rtn = resolve(Object.class).isAssignableFrom(type.resolve(Object.class));

		// We need an exact type match for generics
		// List<CharSequence> is not assignable from List<String>
		rtn &= (!checkingGeneric || resolve(Object.class).equals(type.resolve(Object.class)));

		// Recursively check each generic
		for (int i = 0; i < getGenerics().length; i++) {
			rtn &= getGeneric(i).isAssignableFrom(
					type.as(resolve(Object.class)).getGeneric(i), true);
		}

		return rtn;
	}

	/**
	 * Return {@code true} if this type will resolve to a Class that represents an
	 * array.
	 * @see #getComponentType()
	 */
	public boolean isArray() {
		if (this == NONE) {
			return false;
		}
		return (((this.type instanceof Class) &&
				((Class<?>) this.type).isArray()) ||
				this.type instanceof GenericArrayType ||
				this.resolveType().isArray());
	}

	/**
	 * Return the ResolvableType representing the component type of the array or
	 * {@link #NONE} if this type does not represent an array.
	 * @see #isArray()
	 */
	public ResolvableType getComponentType() {
		if (this == NONE) {
			return NONE;
		}
		if (this.type instanceof Class) {
			Class<?> componentType = ((Class<?>) this.type).getComponentType();
			return (componentType == null ? NONE : forType(componentType,
					this.variableResolver));
		}
		if (this.type instanceof GenericArrayType) {
			return forType(((GenericArrayType) this.type).getGenericComponentType(),
					this.variableResolver);
		}
		return resolveType().getComponentType();
	}

	/**
	 * Convenience method to return this type as a resolvable {@link Collection} type.
	 * Returns {@link #NONE} if this type does not implement or extend
	 * {@link Collection}.
	 * @see #as(Class)
	 * @see #asMap()
	 */
	public ResolvableType asCollection() {
		return as(Collection.class);
	}

	/**
	 * Convenience method to return this type as a resolvable {@link Map} type.
	 * Returns {@link #NONE} if this type does not implement or extend
	 * {@link Map}.
	 * @see #as(Class)
	 * @see #asCollection()
	 */
	public ResolvableType asMap() {
		return as(Map.class);
	}

	/**
	 * Return this type as a {@link ResolvableType} of the specified class. Searches
	 * {@link #getSuperType() supertype} and {@link #getInterfaces() interface}
	 * hierarchies to find a match, returning {@link #NONE} if this type does not
	 * implement or extends the specified class.
	 * @param type the required class type
	 * @return a {@link ResolvableType} representing this object as the specified type or
	 *         {@link #NONE}
	 * @see #asCollection()
	 * @see #asMap()
	 * @see #getSuperType()
	 * @see #getInterfaces()
	 */
	public ResolvableType as(Class<?> type) {
		if (this == NONE) {
			return NONE;
		}
		if (ObjectUtils.nullSafeEquals(resolve(), type)) {
			return this;
		}
		for (ResolvableType interfaceType : getInterfaces()) {
			ResolvableType interfaceAsType = interfaceType.as(type);
			if (interfaceAsType != NONE) {
				return interfaceAsType;
			}
		}
		return getSuperType().as(type);
	}

	/**
	 * Return a {@link ResolvableType} representing the direct supertype of this type.
	 * If no supertype is available this method returns {@link #NONE}.
	 * @see #getInterfaces()
	 */
	public ResolvableType getSuperType() {
		Class<?> resolved = resolve();
		if (resolved == null || resolved.getGenericSuperclass() == null) {
			return NONE;
		}
		return forType(resolved.getGenericSuperclass(), this);
	}

	/**
	 * Return a {@link ResolvableType} array representing the direct interfaces
	 * implemented by this type. If this type does not implement any interfaces an
	 * empty array is returned.
	 * @see #getSuperType()
	 */
	public ResolvableType[] getInterfaces() {
		Class<?> resolved = resolve();
		if (resolved == null || ObjectUtils.isEmpty(resolved.getGenericInterfaces())) {
			return EMPTY_TYPES_ARRAY;
		}
		Type[] interfaceTypes = resolved.getGenericInterfaces();
		ResolvableType[] interfaces = new ResolvableType[interfaceTypes.length];
		for (int i = 0; i < interfaceTypes.length; i++) {
			interfaces[i] = forType(interfaceTypes[i], this);
		}
		return interfaces;
	}

	/**
	 * Return {@code true} if this type contains generic parameters.
	 * @see #getGeneric(int...)
	 * @see #getGenerics()
	 */
	public boolean hasGenerics() {
		return (getGenerics().length > 0);
	}

	/**
	 * Returns a {@link ResolvableType} for the specified nesting level. See
	 * {@link #getNested(int, Map)} for details.
	 * @param nestingLevel the nesting level
	 * @return the {@link ResolvableType} type, or {@code #NONE}
	 */
	public ResolvableType getNested(int nestingLevel) {
		return getNested(nestingLevel, null);
	}

	/**
	 * Returns a {@link ResolvableType} for the specified nesting level. The nesting level
	 * refers to the specific generic parameter that should be returned. A nesting level
	 * of 1 indicates this type, 2 indicates the first nested generic, 3 the second and so
	 * on. For example, given {@code List<Set<Integer>>} level 1 refers to the
	 * {@code List}, level 2 the {@code Set} and level 3 the {@code Integer}.
	 *
	 * <p>The {@code typeIndexesPerLevel} map can be used to reference a specific generic
	 * for the given level. For example, an index of 0 would refer to a {@code Map} key,
	 * where as 1 would refer to the value. If the map does not contain an value for a
	 * specific level the last generic will be used (e.g. a {@code Map} value).
	 *
	 * <p>Nesting levels may also apply to array types, for example given
	 * {@code String[]}, a nesting level of 2 referes to {@code String}.
	 *
	 * <p>If a type does not {@link #hasGenerics() contain} generics the
	 * {@link #getSuperType() super-type} hierarchy will be considered.
	 * @param nestingLevel the required nesting level, indexed from 1 for the current
	 *        type, 2 for the first nested generic, 3 for the second and so on.
	 * @param typeIndexesPerLevel a map containing the generic index for a given nesting
	 *        level (may be {@code null}).
	 * @return a {@link ResolvableType} for the nested level or {@link #NONE}.
	 */
	public ResolvableType getNested(int nestingLevel,
			Map<Integer, Integer> typeIndexesPerLevel) {
		ResolvableType result = this;
		for (int i = 2; i <= nestingLevel; i++) {
			if (result.isArray()) {
				result = result.getComponentType();
			}
			else {
				// Handle derived types
				while (result != ResolvableType.NONE && !result.hasGenerics()) {
					result = result.getSuperType();
				}
				Integer index = (typeIndexesPerLevel == null ? null
						: typeIndexesPerLevel.get(i));
				index = (index == null ? result.getGenerics().length - 1 : index);
				result = result.getGeneric(index);
			}
		}
		return result;
	}

	/**
	 * Return a {@link ResolvableType} representing the generic parameter for the given
	 * indexes. Indexes are zero based, for example given the type
	 * {@code Map<Integer, List<String>>}, {@code getGeneric(0)} will access the
	 * {@code Integer}. Nested generics can be accessed by specifying multiple indexes,
	 * for example {@code getGeneric(1, 0)} will access the {@code String} from the nested
	 * {@code List}. For convenience, if no indexes are specified the first generic is
	 * returned.
	 *
	 * <p>If no generic is available at the specified indexes {@link #NONE} is returned.
	 * @param indexes the indexes that refers to the generic parameter (may be omitted to
	 *        return the first generic)
	 * @return a {@link ResolvableType} for the specified generic or {@link #NONE}
	 * @see #hasGenerics()
	 * @see #getGenerics()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType getGeneric(int... indexes) {
		try {
			if (indexes == null || indexes.length == 0) {
				return getGenerics()[0];
			}
			ResolvableType rtn = this;
			for (int index : indexes) {
				rtn = rtn.getGenerics()[index];
			}
			return rtn;
		}
		catch (IndexOutOfBoundsException ex) {
			return NONE;
		}
	}

	/**
	 * Return an array of {@link ResolvableType} representing the generics parameters of
	 * this type. If no generics are available an empty array is returned. If you need to
	 * access a specific generic consider using the {@link #getGeneric(int...)} method as
	 * it allows access to nested generics, and protects against
	 * {@code IndexOutOfBoundsExceptions}
	 * @return an array of {@link ResolvableType}s representing the generic parameters
	 *         (never {@code null})
	 * @see #hasGenerics()
	 * @see #getGeneric(int...)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public ResolvableType[] getGenerics() {
		if (this == NONE) {
			return EMPTY_TYPES_ARRAY;
		}
		if (this.type instanceof ParameterizedType) {
			Type[] genericTypes = ((ParameterizedType) getType()).getActualTypeArguments();
			ResolvableType[] generics = new ResolvableType[genericTypes.length];
			for (int i = 0; i < genericTypes.length; i++) {
				generics[i] = forType(genericTypes[i], this);
			}
			return generics;
		}
		return resolveType().getGenerics();
	}

	/**
	 * Convenience method that will {@link #getGenerics() get} and {@link #resolve()
	 * resolve} generic parameters.
	 * @return an array of resolved generic parameters (the resulting array will never be
	 *         {@code null}, but it may contain {@code null} elements})
	 * @see #getGenerics()
	 * @see #resolve()
	 */
	public Class<?>[] resolveGenerics() {
		ResolvableType[] generics = getGenerics();
		Class<?>[] resolvedGenerics = new Class<?>[generics.length];
		for (int i = 0; i < generics.length; i++) {
			resolvedGenerics[i] = generics[i].resolve();
		}
		return resolvedGenerics;
	}

	/**
	 * Convenience method that will {@link #getGeneric(int...) get} and
	 * {@link #resolve() resolve} a specific generic parameters.
	 * @param indexes the indexes that refers to the generic parameter (may be omitted to
	 *        return the first generic)
	 * @return a resolved {@link Class} or {@code null}
	 * @see #getGeneric(int...)
	 * @see #resolve()
	 */
	public Class<?> resolveGeneric(int... indexes) {
		return getGeneric(indexes).resolve();
	}

	/**
	 * Resolve this type to a {@link java.lang.Class}, returning {@code null} if the type
	 * cannot be resolved. This method will consider bounds of {@link TypeVariable}s and
	 * {@link WildcardType}s if direct resolution fails.
	 * @return the resolved {@link Class} or {@code null}
	 * @see #resolve(Class)
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public Class<?> resolve() {
		return resolve(null);
	}

	/**
	 * Resolve this type to a {@link java.lang.Class}, returning the specified
	 * {@code fallback} if the type cannot be resolved. This method will consider bounds
	 * of {@link TypeVariable}s and {@link WildcardType}s if direct resolution fails.
	 * @param fallback the fallback class to use if resolution fails (may be {@code null})
	 * @return the resolved {@link Class} or the {@code fallback}
	 * @see #resolve()
	 * @see #resolveGeneric(int...)
	 * @see #resolveGenerics()
	 */
	public Class<?> resolve(Class<?> fallback) {
		if (this.resolved == null) {
			synchronized (this) {
				this.resolved = resolveClass();
				this.resolved = (this.resolved == null ? void.class : this.resolved);
			}
		}
		return (this.resolved == void.class ? fallback : this.resolved);
	}

	private Class<?> resolveClass() {
		if (this.type instanceof Class<?> || this.type == null) {
			return (Class<?>) this.type;
		}
		if (this.type instanceof GenericArrayType) {
			return Array.newInstance(getComponentType().resolve(), 0).getClass();
		}
		return resolveType().resolve();
	}

	/**
	 * Resolve this type by a single level, returning the resolved value or {@link #NONE}.
	 */
	ResolvableType resolveType() {
		Type resolved = null;
		if (this.type instanceof ParameterizedType) {
			resolved = ((ParameterizedType) this.type).getRawType();
		}
		else if (this.type instanceof WildcardType) {
			resolved = resolveBounds(((WildcardType) this.type).getUpperBounds());
			if (resolved == null) {
				resolved = resolveBounds(((WildcardType) this.type).getLowerBounds());
			}
		}
		else if (this.type instanceof TypeVariable) {
			if (this.variableResolver != null) {
				resolved = this.variableResolver.resolveVariable((TypeVariable<?>) this.type);
			}
			if (resolved == null) {
				resolved = resolveBounds(((TypeVariable<?>) this.type).getBounds());
			}
		}
		return (resolved == null ? NONE : forType(resolved, this.variableResolver));
	}

	private Type resolveBounds(Type[] bounds) {
		if (ObjectUtils.isEmpty(bounds) || Object.class.equals(bounds[0])) {
			return null;
		}
		return bounds[0];
	}

	public Type resolveVariable(TypeVariable<?> variable) {
		Assert.notNull("Variable must not be null");
		if (this.type instanceof ParameterizedType) {

			ParameterizedType parameterizedType = (ParameterizedType) this.type;
			Type owner = parameterizedType.getOwnerType();

			if (parameterizedType.getRawType().equals(variable.getGenericDeclaration())) {
				TypeVariable<?>[] variables = resolve().getTypeParameters();
				for (int i = 0; i < variables.length; i++) {
					if (ObjectUtils.nullSafeEquals(variables[i].getName(), variable.getName())) {
						return parameterizedType.getActualTypeArguments()[i];
					}
				}
			}

			Type resolved = null;
			if (this.variableResolver != null) {
					resolved = this.variableResolver.resolveVariable(variable);
			}
			if (resolved == null && owner != null) {
				resolved = forType(owner, this.variableResolver).resolveVariable(variable);
			}
			return resolved;
		}

		if (this.type instanceof TypeVariable<?>) {
			return resolveType().resolveVariable(variable);
		}

		return null;
	}

	/**
	 * Return a string representation of this type in its fully resolved form
	 * (including any generic parameters).
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (isArray()) {
			return getComponentType() + "[]";
		}
		StringBuilder result = new StringBuilder();
		result.append(resolve() == null ? "?" : resolve().getName());
		if (hasGenerics()) {
			result.append("<");
			result.append(StringUtils.arrayToDelimitedString(getGenerics(), ", "));
			result.append(">");
		}
		return result.toString();
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.type) * 31
				+ ObjectUtils.nullSafeHashCode(this.variableResolver);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof ResolvableType) {
			ResolvableType other = (ResolvableType) obj;
			return ObjectUtils.nullSafeEquals(this.type, other.type)
					&& ObjectUtils.nullSafeEquals(this.variableResolver,
							other.variableResolver);
		}
		return false;
	}


	/**
	 * Return a {@link ResolvableType} for the specified {@link Class}. For example
	 * {@code ResolvableType.forClass(MyArrayList.class)}.
	 * @param sourceClass the source class (must not be {@code null}
	 * @return a {@link ResolvableType} for the specified class
	 * @see #forClass(Class, Class)
	 */
	public static ResolvableType forClass(Class<?> sourceClass) {
		Assert.notNull(sourceClass, "Source class must not be null");
		return forType(sourceClass);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Class} with a given
	 * implementation. For example
	 * {@code ResolvableType.forClass(List.class, MyArrayList.class)}.
	 * @param sourceClass the source class (must not be {@code null}
	 * @param implementationClass the implementation class (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified class backed by the given
	 *         implementation class
	 * @see #forClass(Class)
	 */
	public static ResolvableType forClass(Class<?> sourceClass, Class<?> implementationClass) {
		Assert.notNull(sourceClass, "Source class must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		ResolvableType asType = forType(implementationClass).as(sourceClass);
		return (asType == NONE ? forType(sourceClass) : asType);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field}.
	 * @param field the source field
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field, Class)
	 */
	public static ResolvableType forField(Field field) {
		Assert.notNull(field, "Field must not be null");
		return forType(field.getGenericType());
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Field} with a given
	 * implementation. Use this variant when the class that declares the field includes
	 * generic parameter variables that are satisfied by the implementation class.
	 * @param field the source field
	 * @param implementationClass the implementation class (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified field
	 * @see #forField(Field)
	 */
	public static ResolvableType forField(Field field, Class<?> implementationClass) {
		Assert.notNull(field, "Field must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		TypeVariableResolver variableResolver = forType(implementationClass).as(
				field.getDeclaringClass());
		return forType(field.getGenericType(), variableResolver);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Constructor} parameter.
	 * @param constructor the source constructor (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @return a {@link ResolvableType} for the specified constructor parameter
	 * @see #forConstructorParameter(Constructor, int, Class)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor,
			int parameterIndex) {
		Assert.notNull(constructor, "Constructor must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(constructor,
				parameterIndex));
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Constructor} parameter
	 * with a given implementation. Use this variant when the class that declares the
	 * constructor includes generic parameter variables that are satisfied by the
	 * implementation class.
	 * @param constructor the source constructor (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @param implementationClass the implementation class (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified constructor parameter
	 * @see #forConstructorParameter(Constructor, int)
	 */
	public static ResolvableType forConstructorParameter(Constructor<?> constructor,
			int parameterIndex, Class<?> implementationClass) {
		Assert.notNull(constructor, "Constructor must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		return forMethodParameter(
				MethodParameter.forMethodOrConstructor(constructor, parameterIndex),
				implementationClass);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} parameter.
	 * @param method the source method (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(MethodParameter.forMethodOrConstructor(method,
				parameterIndex));
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} parameter with a
	 * given implementation. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation
	 * class.
	 * @param method the source method (must not be {@code null})
	 * @param parameterIndex the parameter index
	 * @param implementationClass the implementation class (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(Method, int, Class)
	 * @see #forMethodParameter(MethodParameter)
	 */
	public static ResolvableType forMethodParameter(Method method, int parameterIndex,
			Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		return forMethodParameter(
				MethodParameter.forMethodOrConstructor(method, parameterIndex),
				implementationClass);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter}.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(MethodParameter, Class)
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		return forType(methodParameter.getGenericParameterType()).getNested(
				methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link MethodParameter} with a
	 * given implementation. Use this variant when the class that declares the method
	 * includes generic parameter variables that are satisfied by the implementation
	 * class.
	 * @param methodParameter the source method parameter (must not be {@code null})
	 * @param implementationClass the implementation class (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified method parameter
	 * @see #forMethodParameter(MethodParameter)
	 * @see #forMethodParameter(Method, int)
	 */
	public static ResolvableType forMethodParameter(MethodParameter methodParameter,
			Class<?> implementationClass) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		TypeVariableResolver variableResolver = forType(implementationClass).as(
				methodParameter.getMember().getDeclaringClass());
		return forType(methodParameter.getGenericParameterType(), variableResolver).getNested(
				methodParameter.getNestingLevel(), methodParameter.typeIndexesPerLevel);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} return.
	 * @param method the source for the method return
	 * @return a {@link ResolvableType} for the specified method return
	 * @see #forMethodReturn(Method, Class)
	 */
	public static ResolvableType forMethodReturn(Method method) {
		Assert.notNull(method, "Method must not be null");
		return forType(method.getGenericReturnType());
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link Method} return. Use this
	 * variant when the class that declares the method includes generic parameter
	 * variables that are satisfied by the implementation class.
	 * @param method the source for the method return
	 * @param implementationClass the implementation class (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified method return
	 * @see #forMethodReturn(Method)
	 */
	public static ResolvableType forMethodReturn(Method method,
			Class<?> implementationClass) {
		Assert.notNull(method, "Method must not be null");
		Assert.notNull(implementationClass, "ImplementationClass must not be null");
		TypeVariableResolver variableResolver = forType(implementationClass).as(
				method.getDeclaringClass());
		return forType(method.getGenericReturnType(), variableResolver);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link java.lang.reflect.Type}.
	 * @param type the source type (must not be {@code null})
	 * @return a {@link ResolvableType} for the specified {@link java.lang.reflect.Type}
	 */
	public static ResolvableType forType(Type type) {
		return forType(type, null);
	}

	/**
	 * Return a {@link ResolvableType} for the specified {@link java.lang.reflect.Type}
	 * backed by a given {@link TypeVariableResolver}.
	 * @param type the source type (must not be {@code null})
	 * @param variableResolver the variable resolver
	 * @return a {@link ResolvableType} for the specified {@link java.lang.reflect.Type}
	 *         and {@link TypeVariableResolver}
	 */
	public static ResolvableType forType(Type type, TypeVariableResolver variableResolver) {
		ResolvableType key = new ResolvableType(type, variableResolver);
		// Check the cache, we may have a ResolvableType that may have already been resolved
		ResolvableType resolvableType = cache.get(key);
		if (resolvableType == null) {
			resolvableType = key;
			cache.put(key, resolvableType);
		}
		return resolvableType;
	}


	/**
	 * Internal helper to handle bounds from {@link WildcardType}s.
	 */
	private static class WildcardBounds {


		private final Kind kind;

		private final ResolvableType[] bounds;


		/**
		 * Private constructor to create a new {@link WildcardBounds} instance.
		 * @param kind the kind of bounds
		 * @param bounds the bounds
		 * @see #get(ResolvableType)
		 */
		private WildcardBounds(Kind kind, ResolvableType[] bounds) {
			this.kind = kind;
			this.bounds = bounds;
		}


		/**
		 * Return {@code true} if this bounds is the same kind as the specified bounds.
		 */
		public boolean isSameKind(WildcardBounds bounds) {
			return this.kind == bounds.kind;
		}

		/**
		 * Return {@code true} if this bounds is assignable to all the specified types.
		 * @param types the types to test against
		 * @return {@code true} if this bounds is assignable to all types
		 */
		public boolean isAssignableFrom(ResolvableType... types) {
			for (ResolvableType bound : this.bounds) {
				for (ResolvableType type : types) {
					if (!isAssignable(bound, type)) {
						return false;
					}
				}
			}
			return true;
		}

		private boolean isAssignable(ResolvableType source, ResolvableType from) {
			return (this.kind == Kind.UPPER ? source.isAssignableFrom(from)
					: from.isAssignableFrom(source));
		}

		/**
		 * Return the underlying bounds.
		 */
		public ResolvableType[] getBounds() {
			return bounds;
		}


		/**
		 * Get a {@link WildcardBounds} instance for the specified type, returning
		 * {@code null} if the specified type cannot be resolved to a {@link WildcardType}.
		 * @param type the source type
		 * @return a {@link WildcardBounds} instance or {@code null}
		 */
		public static WildcardBounds get(ResolvableType type) {
			ResolvableType resolveToWildcard = type;
			while(!(resolveToWildcard.getType() instanceof WildcardType)) {
				if (resolveToWildcard == NONE) {
					return null;
				}
				resolveToWildcard = resolveToWildcard.resolveType();
			}
			WildcardType wildcardType = (WildcardType) resolveToWildcard.type;
			Kind boundsType = (wildcardType.getLowerBounds().length > 0 ? Kind.LOWER
					: Kind.UPPER);
			Type[] bounds = boundsType == Kind.UPPER ? wildcardType.getUpperBounds()
					: wildcardType.getLowerBounds();
			ResolvableType[] resolvableBounds = new ResolvableType[bounds.length];
			for (int i = 0; i < bounds.length; i++) {
				resolvableBounds[i] = forType(bounds[i], type.variableResolver);
			}
			return new WildcardBounds(boundsType, resolvableBounds);
		}


		/**
		 * The various kinds of bounds.
		 */
		static enum Kind { UPPER, LOWER }

	}

}
