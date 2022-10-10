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

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

/**
 * Gather the need for Java serialization at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see Serializable
 */
public class SerializationHints {

	private final Set<JavaSerializationHint> javaSerializationHints;


	public SerializationHints() {
		this.javaSerializationHints = new LinkedHashSet<>();
	}

	/**
	 * Return the {@link JavaSerializationHint java serialization hints} for types
	 * that need to be serialized using Java serialization at runtime.
	 * @return a stream of {@link JavaSerializationHint java serialization hints}
	 */
	public Stream<JavaSerializationHint> javaSerializationHints() {
		return this.javaSerializationHints.stream();
	}

	/**
	 * Register that the type defined by the specified {@link TypeReference}
	 * need to be serialized using java serialization.
	 * @param type the type to register
	 * @param serializationHint a builder to further customize the serialization
	 * @return {@code this}, to facilitate method chaining
	 */
	public SerializationHints registerType(TypeReference type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
		JavaSerializationHint.Builder builder = new JavaSerializationHint.Builder(type);
		if (serializationHint != null) {
			serializationHint.accept(builder);
		}
		this.javaSerializationHints.add(builder.build());
		return this;
	}

	/**
	 * Register that the type defined by the specified {@link TypeReference}
	 * need to be serialized using java serialization.
	 * @param type the type to register
	 * @return {@code this}, to facilitate method chaining
	 */
	public SerializationHints registerType(TypeReference type) {
		return registerType(type, null);
	}

	/**
	 * Register that the specified type need to be serialized using java
	 * serialization.
	 * @param type the type to register
	 * @param serializationHint a builder to further customize the serialization
	 * @return {@code this}, to facilitate method chaining
	 */
	public SerializationHints registerType(Class<? extends Serializable> type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
		return registerType(TypeReference.of(type), serializationHint);
	}

	/**
	 * Register that the specified type need to be serialized using java
	 * serialization.
	 * @param type the type to register
	 * @return {@code this}, to facilitate method chaining
	 */
	public SerializationHints registerType(Class<? extends Serializable> type) {
		return registerType(type, null);
	}

}
