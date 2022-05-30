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

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.test.context.event.ApplicationEventsHolder;

/**
 * Default implementation of {@link PublishedEvents}.
 *
 * <p>Copied from the Moduliths project.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.3
 */
class DefaultPublishedEvents implements PublishedEvents {

	@Override
	public <T> TypedPublishedEvents<T> ofType(Class<T> type) {
		return SimpleTypedPublishedEvents.of(ApplicationEventsHolder.getRequiredApplicationEvents().stream(type));
	}


	private static class SimpleTypedPublishedEvents<T> implements TypedPublishedEvents<T> {

		private final List<T> events;

		private SimpleTypedPublishedEvents(List<T> events) {
			this.events = events;
		}

		static <T> SimpleTypedPublishedEvents<T> of(Stream<T> stream) {
			return new SimpleTypedPublishedEvents<>(stream.collect(Collectors.toList()));
		}

		@Override
		public <S extends T> TypedPublishedEvents<S> ofSubType(Class<S> subType) {
			return SimpleTypedPublishedEvents.of(getFilteredEvents(subType::isInstance)//
					.map(subType::cast));
		}

		@Override
		public TypedPublishedEvents<T> matching(Predicate<? super T> predicate) {
			return SimpleTypedPublishedEvents.of(getFilteredEvents(predicate));
		}

		@Override
		public <S> TypedPublishedEvents<T> matchingMapped(Function<T, S> mapper, Predicate<? super S> predicate) {
			return SimpleTypedPublishedEvents.of(this.events.stream().flatMap(it -> {
				S mapped = mapper.apply(it);
				return predicate.test(mapped) ? Stream.of(it) : Stream.empty();
			}));
		}

		private Stream<T> getFilteredEvents(Predicate<? super T> predicate) {
			return this.events.stream().filter(predicate);
		}

		@Override
		public Iterator<T> iterator() {
			return this.events.iterator();
		}

		@Override
		public String toString() {
			return this.events.toString();
		}
	}

}
