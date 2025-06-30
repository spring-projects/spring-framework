/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.aot.hint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Gather the need for reflection at runtime.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class ReflectionHints {

	private final Map<TypeReference, TypeHint.Builder> types = new HashMap<>();


	/**
	 * Return the types that require reflection.
	 * @return the type hints
	 */
	public Stream<TypeHint> typeHints() {
		return this.types.values().stream().map(TypeHint.Builder::build);
	}

	/**
	 * Return the reflection hints for the type defined by the specified
	 * {@link TypeReference}.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	public @Nullable TypeHint getTypeHint(TypeReference type) {
		Builder typeHintBuilder = this.types.get(type);
		return (typeHintBuilder != null ? typeHintBuilder.build() : null);
	}

	/**
	 * Return the reflection hints for the specified type.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	public @Nullable TypeHint getTypeHint(Class<?> type) {
		return getTypeHint(TypeReference.of(type));
	}

	/**
	 * Register or customize reflection hints for the type defined by the
	 * specified {@link TypeReference}.
	 * @param type the type to customize
	 * @param typeHint a builder to further customize hints for that type
	 * @return {@code this}, to facilitate method chaining
	 * @see #registerType(TypeReference, MemberCategory...)
	 */
	public ReflectionHints registerType(TypeReference type, Consumer<TypeHint.Builder> typeHint) {
		Builder builder = this.types.computeIfAbsent(type, TypeHint.Builder::new);
		typeHint.accept(builder);
		return this;
	}

	/**
	 * Register or customize reflection hints for the specified type
	 * using the specified {@link MemberCategory MemberCategories}.
	 * @param type the type to customize
	 * @param memberCategories the member categories to apply
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerType(TypeReference type, MemberCategory... memberCategories) {
		return registerType(type, TypeHint.builtWith(memberCategories));
	}

	/**
	 * Register or customize reflection hints for the specified type.
	 * @param type the type to customize
	 * @param typeHint a builder to further customize hints for that type
	 * @return {@code this}, to facilitate method chaining
	 * @see #registerType(Class, MemberCategory...)
	 */
	public ReflectionHints registerType(Class<?> type, Consumer<TypeHint.Builder> typeHint) {
		Assert.notNull(type, "'type' must not be null");
		if (type.getCanonicalName() != null) {
			registerType(TypeReference.of(type), typeHint);
		}
		return this;
	}

	/**
	 * Register or customize reflection hints for the specified type
	 * using the specified {@link MemberCategory MemberCategories}.
	 * @param type the type to customize
	 * @param memberCategories the member categories to apply
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerType(Class<?> type, MemberCategory... memberCategories) {
		Assert.notNull(type, "'type' must not be null");
		if (type.getCanonicalName() != null) {
			registerType(TypeReference.of(type), memberCategories);
		}
		return this;
	}

	/**
	 * Register or customize reflection hints for the specified type if it
	 * is available using the specified {@link ClassLoader}.
	 * @param classLoader the classloader to use to check if the type is present
	 * @param typeName the type to customize
	 * @param typeHint a builder to further customize hints for that type
	 * @return {@code this}, to facilitate method chaining
	 * @see #registerTypeIfPresent(ClassLoader, String, MemberCategory...)
	 */
	public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader,
			String typeName, Consumer<TypeHint.Builder> typeHint) {

		if (ClassUtils.isPresent(typeName, classLoader)) {
			registerType(TypeReference.of(typeName), typeHint);
		}
		return this;
	}

	/**
	 * Register or customize reflection hints for the specified type if it
	 * is available using the specified {@link ClassLoader}.
	 * @param classLoader the classloader to use to check if the type is present
	 * @param typeName the type to customize
	 * @param memberCategories the member categories to apply
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerTypeIfPresent(@Nullable ClassLoader classLoader,
			String typeName, MemberCategory... memberCategories) {

		return registerTypeIfPresent(classLoader, typeName, TypeHint.builtWith(memberCategories));
	}

	/**
	 * Register or customize reflection hints for the types defined by the
	 * specified list of {@link TypeReference type references}. The specified
	 * {@code typeHint} consumer is invoked for each type.
	 * @param types the types to customize
	 * @param typeHint a builder to further customize hints for each type
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerTypes(Iterable<TypeReference> types, Consumer<TypeHint.Builder> typeHint) {
		types.forEach(type -> registerType(type, typeHint));
		return this;
	}

	/**
	 * Register or customize reflection hints for all the interfaces implemented by
	 * the given type and its parent classes, ignoring the common Java language interfaces.
	 * The specified {@code typeHint} consumer is invoked for each type.
	 * @param type the type to consider
	 * @param typeHint a builder to further customize hints for each type
	 * @return {@code this}, to facilitate method chaining
	 * @since 6.2
	 */
	public ReflectionHints registerForInterfaces(Class<?> type, Consumer<TypeHint.Builder> typeHint) {
		Class<?> currentClass = type;
		while (currentClass != null && currentClass != Object.class) {
			for (Class<?> interfaceType : currentClass.getInterfaces()) {
				if (!ClassUtils.isJavaLanguageInterface(interfaceType)) {
					this.registerType(interfaceType, typeHint);
					registerForInterfaces(interfaceType, typeHint);
				}
			}
			currentClass = currentClass.getSuperclass();
		}
		return this;
	}

	/**
	 * Register the need for reflective field access on the specified {@link Field}.
	 * @param field the field that requires reflective access
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerField(Field field) {
		return registerType(TypeReference.of(field.getDeclaringClass()),
				typeHint -> typeHint.withField(field.getName()));
	}

	/**
	 * Register the need for reflection on the specified {@link Constructor},
	 * using the specified {@link ExecutableMode}.
	 * @param constructor the constructor that requires reflection
	 * @param mode the requested mode
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerConstructor(Constructor<?> constructor, ExecutableMode mode) {
		return registerType(TypeReference.of(constructor.getDeclaringClass()),
				typeHint -> typeHint.withConstructor(mapParameters(constructor), mode));
	}

	/**
	 * Register the need for reflection on the specified {@link Method},
	 * using the specified {@link ExecutableMode}.
	 * @param method the method that requires reflection
	 * @param mode the requested mode
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerMethod(Method method, ExecutableMode mode) {
		return registerType(TypeReference.of(method.getDeclaringClass()),
				typeHint -> typeHint.withMethod(method.getName(), mapParameters(method), mode));
	}

	private List<TypeReference> mapParameters(Executable executable) {
		return TypeReference.listOf(executable.getParameterTypes());
	}

}
