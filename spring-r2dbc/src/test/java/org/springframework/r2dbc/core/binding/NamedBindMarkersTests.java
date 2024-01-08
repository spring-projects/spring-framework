/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.r2dbc.core.binding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NamedBindMarkers}.
 *
 * @author Mark Paluch
 */
class NamedBindMarkersTests {

	@Test
	void shouldCreateNewBindMarkers() {
		BindMarkersFactory factory = BindMarkersFactory.named("@", "p", 32);

		BindMarkers bindMarkers1 = factory.create();
		BindMarkers bindMarkers2 = factory.create();

		assertThat(bindMarkers1.next().getPlaceholder()).isEqualTo("@p0");
		assertThat(bindMarkers2.next().getPlaceholder()).isEqualTo("@p0");
	}

	@ParameterizedTest
	@ValueSource(strings = {"$", "?"})
	void nextShouldIncrementBindMarker(String prefix) {
		BindMarkers bindMarkers = BindMarkersFactory.named(prefix, "p", 32).create();

		BindMarker marker1 = bindMarkers.next();
		BindMarker marker2 = bindMarkers.next();

		assertThat(marker1.getPlaceholder()).isEqualTo(prefix + "p0");
		assertThat(marker2.getPlaceholder()).isEqualTo(prefix + "p1");
	}

	@Test
	void nextShouldConsiderNameHint() {
		BindMarkers bindMarkers = BindMarkersFactory.named("@", "x", 32).create();

		BindMarker marker1 = bindMarkers.next("foo1bar");
		BindMarker marker2 = bindMarkers.next();

		assertThat(marker1.getPlaceholder()).isEqualTo("@x0foo1bar");
		assertThat(marker2.getPlaceholder()).isEqualTo("@x1");
	}

	@Test
	void nextShouldConsiderFilteredNameHint() {
		BindMarkers bindMarkers = BindMarkersFactory.named("@", "p", 32,
				s -> s.chars().filter(Character::isAlphabetic).collect(StringBuilder::new,
				StringBuilder::appendCodePoint, StringBuilder::append).toString()).create();

		BindMarker marker1 = bindMarkers.next("foo1.bar?");
		BindMarker marker2 = bindMarkers.next();

		assertThat(marker1.getPlaceholder()).isEqualTo("@p0foobar");
		assertThat(marker2.getPlaceholder()).isEqualTo("@p1");
	}

	@Test
	void nextShouldConsiderNameLimit() {
		BindMarkers bindMarkers = BindMarkersFactory.named("@", "p", 10).create();

		BindMarker marker1 = bindMarkers.next("123456789");

		assertThat(marker1.getPlaceholder()).isEqualTo("@p012345678");
	}

	@Test
	void bindValueShouldBindByName() {
		BindTarget bindTarget = mock();

		BindMarkers bindMarkers = BindMarkersFactory.named("@", "p", 32).create();

		bindMarkers.next().bind(bindTarget, "foo");
		bindMarkers.next().bind(bindTarget, "bar");

		verify(bindTarget).bind("p0", "foo");
		verify(bindTarget).bind("p1", "bar");
	}

	@Test
	void bindNullShouldBindByName() {
		BindTarget bindTarget = mock();

		BindMarkers bindMarkers = BindMarkersFactory.named("@", "p", 32).create();

		bindMarkers.next(); // ignore
		bindMarkers.next().bindNull(bindTarget, Integer.class);

		verify(bindTarget).bindNull("p1", Integer.class);
	}

}
