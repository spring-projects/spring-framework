/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @deprecated in favor of {@link ReflectionHintsPredicates}
 */
@Deprecated(since = "7.0.6", forRemoval = true)
@SuppressWarnings("removal")
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
