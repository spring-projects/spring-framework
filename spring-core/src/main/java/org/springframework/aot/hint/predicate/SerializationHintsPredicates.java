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

package org.springframework.aot.hint.predicate;

import java.util.function.Predicate;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.Assert;

/**
 * Generator of {@link SerializationHints} predicates, testing whether the
 * given hints match the expected behavior for serialization.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class SerializationHintsPredicates {

	SerializationHintsPredicates() {
	}


	/**
	 * Return a predicate that checks whether a {@link SerializationHints
	 * serialization hint} is registered for the given type.
	 * @param type the type to check
	 * @return the {@link RuntimeHints} predicate
	 * @see java.lang.reflect.Proxy
	 */
	public Predicate<RuntimeHints> onType(Class<?> type) {
		Assert.notNull(type, "'type' must not be null");
		return onType(TypeReference.of(type));
	}

	/**
	 * Return a predicate that checks whether a {@link SerializationHints
	 * serialization hint} is registered for the given type reference.
	 * @param typeReference the type to check
	 * @return the {@link RuntimeHints} predicate
	 * @see java.lang.reflect.Proxy
	 */
	public Predicate<RuntimeHints> onType(TypeReference typeReference) {
		Assert.notNull(typeReference, "'typeReference' must not be null");
		return hints -> hints.serialization().javaSerializationHints().anyMatch(
				hint -> hint.getType().equals(typeReference));
	}

}
