/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.event;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * All Spring application events fired during the test execution.
 *
 * <p>Copied from the Moduliths project.
 *
 * @author Oliver Drotbohm
 * @since 5.3.3
 */
public interface PublishedEvents {

	/**
	 * Creates a new {@link PublishedEvents} instance for the given events.
	 *
	 * @param events must not be {@literal null}
	 * @return will never be {@literal null}
	 */
	public static PublishedEvents of(Object... events) {
		return of(Arrays.asList(events));
	}

	/**
	 * Returns all application events of the given type that were fired during the test execution.
	 *
	 * @param <T> the event type
	 * @param type must not be {@literal null}
	 */
	<T> TypedPublishedEvents<T> ofType(Class<T> type);

	/**
	 * All application events of a given type that were fired during a test execution.
	 *
	 * @param <T> the event type
	 */
	interface TypedPublishedEvents<T> extends Iterable<T> {

		/**
		 * Further constrain the event type for downstream assertions.
		 *
		 * @param subType the subtype
		 * @return will never be {@literal null}
		 */
		<S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType);

		/**
		 * Returns all {@link TypedPublishedEvents} that match the given predicate.
		 *
		 * @param predicate must not be {@literal null}
		 * @return will never be {@literal null}
		 */
		TypedPublishedEvents<T> matching(Predicate<? super T> predicate);

		/**
		 * Returns all {@link TypedPublishedEvents} that match the given predicate
		 * after applying the given mapping step.
		 *
		 * @param <S> the intermediate type to apply the {@link Predicate} on
		 * @param mapper the mapping step to extract a part of the original event
		 * subject to test for the {@link Predicate}
		 * @param predicate the {@link Predicate} to apply on the value extracted
		 * @return will never be {@literal null}
		 */
		<S> TypedPublishedEvents<T> matchingMapped(Function<T, S> mapper, Predicate<? super S> predicate);
	}

}
