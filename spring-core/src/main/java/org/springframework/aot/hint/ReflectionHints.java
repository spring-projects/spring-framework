/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.aot.hint.TypeHint.Builder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Gather the need for reflection at runtime.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
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
	@Nullable
	public TypeHint getTypeHint(TypeReference type) {
		Builder typeHintBuilder = this.types.get(type);
		return (typeHintBuilder != null ? typeHintBuilder.build() : null);
	}

	/**
	 * Return the reflection hints for the specified type.
	 * @param type the type to inspect
	 * @return the reflection hints for this type, or {@code null}
	 */
	@Nullable
	public TypeHint getTypeHint(Class<?> type) {
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
		return registerType(TypeReference.of(type), typeHint);
	}

	/**
	 * Register or customize reflection hints for the specified type
	 * using the specified {@link MemberCategory MemberCategories}.
	 * @param type the type to customize
	 * @param memberCategories the member categories to apply
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerType(Class<?> type, MemberCategory... memberCategories) {
		return registerType(TypeReference.of(type), memberCategories);
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
	 * Register the need for reflection on the specified {@link Field}.
	 * @param field the field that requires reflection
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerField(Field field) {
		return registerType(TypeReference.of(field.getDeclaringClass()),
				typeHint -> typeHint.withField(field.getName()));
	}

	/**
	 * Register the need for reflection on the specified {@link Constructor},
	 * enabling {@link ExecutableMode#INVOKE}.
	 * @param constructor the constructor that requires reflection
	 * @return {@code this}, to facilitate method chaining
	 * @deprecated in favor of {@link #registerConstructor(Constructor, ExecutableMode)}
	 */
	@Deprecated
	public ReflectionHints registerConstructor(Constructor<?> constructor) {
		return registerConstructor(constructor, ExecutableMode.INVOKE);
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
	 * Register the need for reflection on the specified {@link Constructor}.
	 * @param constructor the constructor that requires reflection
	 * @param constructorHint a builder to further customize the hints of this
	 * constructor
	 * @return {@code this}, to facilitate method chaining`
	 * @deprecated in favor of {@link #registerConstructor(Constructor, ExecutableMode)}
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	public ReflectionHints registerConstructor(Constructor<?> constructor, Consumer<ExecutableHint.Builder> constructorHint) {
		return registerType(TypeReference.of(constructor.getDeclaringClass()),
				typeHint -> typeHint.withConstructor(mapParameters(constructor), constructorHint));
	}

	/**
	 * Register the need for reflection on the specified {@link Method},
	 * enabling {@link ExecutableMode#INVOKE}.
	 * @param method the method that requires reflection
	 * @return {@code this}, to facilitate method chaining
	 * @deprecated in favor of {@link #registerMethod(Method, ExecutableMode)}
	 */
	@Deprecated
	public ReflectionHints registerMethod(Method method) {
		return registerMethod(method, ExecutableMode.INVOKE);
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

	/**
	 * Register the need for reflection on the specified {@link Method}.
	 * @param method the method that requires reflection
	 * @param methodHint a builder to further customize the hints of this method
	 * @return {@code this}, to facilitate method chaining
	 * @deprecated in favor of {@link #registerMethod(Method, ExecutableMode)}
	 */
	@Deprecated
	@SuppressWarnings("deprecation")
	public ReflectionHints registerMethod(Method method, Consumer<ExecutableHint.Builder> methodHint) {
		return registerType(TypeReference.of(method.getDeclaringClass()),
				typeHint -> typeHint.withMethod(method.getName(), mapParameters(method), methodHint));
	}

	private List<TypeReference> mapParameters(Executable executable) {
		return TypeReference.listOf(executable.getParameterTypes());
	}

}
