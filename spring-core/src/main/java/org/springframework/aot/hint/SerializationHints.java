/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * Gather the need for Java serialization at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see Serializable
 * @deprecated in favor of {@link ReflectionHints}
 */
@Deprecated(since = "7.0.6", forRemoval = true)
@SuppressWarnings("removal")
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
