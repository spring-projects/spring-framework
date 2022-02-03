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
import java.util.stream.Stream;

/**
 * Gather the need for Java serialization at runtime.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see Serializable
 */
public class JavaSerializationHints {

	private final Set<TypeReference> types;


	public JavaSerializationHints() {
		this.types = new LinkedHashSet<>();
	}

	/**
	 * Return the {@link TypeReference types} that need to be serialized using
	 * Java serialization at runtime.
	 * @return a stream of {@link Serializable} types
	 */
	public Stream<TypeReference> types() {
		return this.types.stream();
	}

	/**
	 * Register that the type defined by the specified {@link TypeReference}
	 * need to be serialized using java serialization.
	 * @param type the type to register
	 * @return {@code this}, to facilitate method chaining
	 */
	public JavaSerializationHints registerType(TypeReference type) {
		this.types.add(type);
		return this;
	}

	/**
	 * Register that the specified type need to be serialized using java
	 * serialization.
	 * @param type the type to register
	 * @return {@code this}, to facilitate method chaining
	 */
	public JavaSerializationHints registerType(Class<? extends Serializable> type) {
		return registerType(TypeReference.of(type));
	}

}
