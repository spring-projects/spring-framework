/*
 * Copyright 2002-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link IndexedBindMarkers}.
 *
 * @author Mark Paluch
 */
class IndexedBindMarkersUnitTests {

	@Test
	void shouldCreateNewBindMarkers() {
		BindMarkersFactory factory = BindMarkersFactory.indexed("$", 0);
		BindMarkers bindMarkers1 = factory.create();
		BindMarkers bindMarkers2 = factory.create();

		assertThat(bindMarkers1.next().getPlaceholder()).isEqualTo("$0");
		assertThat(bindMarkers2.next().getPlaceholder()).isEqualTo("$0");
	}

	@Test
	void shouldCreateNewBindMarkersWithOffset() {
		BindTarget bindTarget = mock(BindTarget.class);
		BindMarkers bindMarkers = BindMarkersFactory.indexed("$", 1).create();

		BindMarker first = bindMarkers.next();
		first.bind(bindTarget, "foo");

		BindMarker second = bindMarkers.next();
		second.bind(bindTarget, "bar");

		assertThat(first.getPlaceholder()).isEqualTo("$1");
		assertThat(second.getPlaceholder()).isEqualTo("$2");
		verify(bindTarget).bind(0, "foo");
		verify(bindTarget).bind(1, "bar");
	}

	@Test
	void nextShouldIncrementBindMarker() {
		String[] prefixes = {"$", "?"};

		for (String prefix : prefixes) {
			BindMarkers bindMarkers = BindMarkersFactory.indexed(prefix, 0).create();

			BindMarker marker1 = bindMarkers.next();
			BindMarker marker2 = bindMarkers.next();

			assertThat(marker1.getPlaceholder()).isEqualTo(prefix + "0");
			assertThat(marker2.getPlaceholder()).isEqualTo(prefix + "1");
		}
	}

	@Test
	void bindValueShouldBindByIndex() {
		BindTarget bindTarget = mock(BindTarget.class);
		BindMarkers bindMarkers = BindMarkersFactory.indexed("$", 0).create();

		bindMarkers.next().bind(bindTarget, "foo");
		bindMarkers.next().bind(bindTarget, "bar");

		verify(bindTarget).bind(0, "foo");
		verify(bindTarget).bind(1, "bar");
	}

	@Test
	void bindNullShouldBindByIndex() {
		BindTarget bindTarget = mock(BindTarget.class);
		BindMarkers bindMarkers = BindMarkersFactory.indexed("$", 0).create();

		bindMarkers.next(); // ignore
		bindMarkers.next().bindNull(bindTarget, Integer.class);

		verify(bindTarget).bindNull(1, Integer.class);
	}

}
