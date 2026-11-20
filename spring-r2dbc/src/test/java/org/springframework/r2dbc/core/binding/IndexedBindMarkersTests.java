/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core.binding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link IndexedBindMarkers}.
 *
 * @author Mark Paluch
 */
class IndexedBindMarkersTests {

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
		BindTarget bindTarget = mock();
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
		BindTarget bindTarget = mock();
		BindMarkers bindMarkers = BindMarkersFactory.indexed("$", 0).create();

		bindMarkers.next().bind(bindTarget, "foo");
		bindMarkers.next().bind(bindTarget, "bar");

		verify(bindTarget).bind(0, "foo");
		verify(bindTarget).bind(1, "bar");
	}

	@Test
	void bindNullShouldBindByIndex() {
		BindTarget bindTarget = mock();
		BindMarkers bindMarkers = BindMarkersFactory.indexed("$", 0).create();

		bindMarkers.next(); // ignore
		bindMarkers.next().bindNull(bindTarget, Integer.class);

		verify(bindTarget).bindNull(1, Integer.class);
	}

}
