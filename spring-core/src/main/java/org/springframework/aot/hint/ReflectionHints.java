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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.aot.hint.TypeHint.Builder;

/**
 * Gather the need for reflection at runtime.
 *
 * @author Stephane Nicoll
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
	 * Register or customize reflection hints for the type defined by the
	 * specified {@link TypeReference}.
	 * @param type the type to customize
	 * @param typeHint a builder to further customize hints for that type
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerType(TypeReference type, Consumer<TypeHint.Builder> typeHint) {
		Builder builder = this.types.computeIfAbsent(type, TypeHint.Builder::new);
		typeHint.accept(builder);
		return this;
	}

	/**
	 * Register or customize reflection hints for the specified type.
	 * @param type the type to customize
	 * @param typeHint a builder to further customize hints for that type
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerType(Class<?> type, Consumer<TypeHint.Builder> typeHint) {
		return registerType(TypeReference.of(type), typeHint);
	}

	/**
	 * Register the need for reflection on the specified {@link Field}.
	 * @param field the field that requires reflection
	 * @param fieldHint a builder to further customize the hints of this field
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerField(Field field, Consumer<FieldHint.Builder> fieldHint) {
		return registerType(TypeReference.of(field.getDeclaringClass()),
				typeHint -> typeHint.withField(field.getName(), fieldHint));
	}

	/**
	 * Register the need for reflection on the specified {@link Field},
	 * enabling write access.
	 * @param field the field that requires reflection
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerField(Field field) {
		return registerField(field, fieldHint -> fieldHint.allowWrite(true));
	}

	/**
	 * Register the need for reflection on the specified {@link Constructor}.
	 * @param constructor the constructor that requires reflection
	 * @param constructorHint a builder to further customize the hints of this
	 * constructor
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerConstructor(Constructor<?> constructor, Consumer<ExecutableHint.Builder> constructorHint) {
		return registerType(TypeReference.of(constructor.getDeclaringClass()),
				typeHint -> typeHint.withConstructor(mapParameters(constructor), constructorHint));
	}

	/**
	 * Register the need for reflection on the specified {@link Constructor},
	 * enabling {@link ExecutableMode#INVOKE}.
	 * @param constructor the constructor that requires reflection
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerConstructor(Constructor<?> constructor) {
		return registerConstructor(constructor, constructorHint ->
				constructorHint.withMode(ExecutableMode.INVOKE));
	}

	/**
	 * Register the need for reflection on the specified {@link Method}.
	 * @param method the method that requires reflection
	 * @param methodHint a builder to further customize the hints of this method
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerMethod(Method method, Consumer<ExecutableHint.Builder> methodHint) {
		return registerType(TypeReference.of(method.getDeclaringClass()),
				typeHint -> typeHint.withMethod(method.getName(), mapParameters(method), methodHint));
	}

	/**
	 * Register the need for reflection on the specified {@link Method},
	 * enabling {@link ExecutableMode#INVOKE}.
	 * @param method the method that requires reflection
	 * @return {@code this}, to facilitate method chaining
	 */
	public ReflectionHints registerMethod(Method method) {
		return registerMethod(method, methodHint -> methodHint.withMode(ExecutableMode.INVOKE));
	}

	private List<TypeReference> mapParameters(Executable executable) {
		return Arrays.stream(executable.getParameterTypes()).map(TypeReference::of)
				.collect(Collectors.toList());
	}

}
