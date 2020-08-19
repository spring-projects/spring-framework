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

package org.springframework.test.context.junit.jupiter;

import java.util.function.Predicate;

/**
 * All Spring application events fired during the test execution.
 *
 * @author Oliver Drotbohm
 */
public interface PublishedEvents {

	/**
	 * Returns all application events of the given type that were fired during the test execution.
	 *
	 * @param <T> the event type
	 * @param type must not be {@literal null}.
	 * @return
	 */
	<T> TypedPublishedEvents<T> ofType(Class<T> type);

	/**
	 * All application events of a given type that were fired during a test execution.
	 *
	 * @author Oliver Drotbohm
	 * @param <T> the event type
	 */
	interface TypedPublishedEvents<T> extends Iterable<T> {

		/**
		 * Further constrain the event type for downstream assertions.
		 *
		 * @param <S> the event sub type
		 * @param subType the sub type
		 * @return
		 */
		<S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType);

		/**
		 * Returns all {@link TypedPublishedEvents} that match the given predicate.
		 *
		 * @param predicate must not be {@literal null}.
		 * @return
		 */
		TypedPublishedEvents<T> matching(Predicate<? super T> predicate);
	}
}
