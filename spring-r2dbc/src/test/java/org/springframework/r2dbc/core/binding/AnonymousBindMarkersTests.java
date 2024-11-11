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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AnonymousBindMarkers}.
 *
 * @author Mark Paluch
 */
class AnonymousBindMarkersTests {

	@Test
	void shouldCreateNewBindMarkers() {
		BindMarkersFactory factory = BindMarkersFactory.anonymous("?");

		BindMarkers bindMarkers1 = factory.create();
		BindMarkers bindMarkers2 = factory.create();

		assertThat(bindMarkers1.next().getPlaceholder()).isEqualTo("?");
		assertThat(bindMarkers2.next().getPlaceholder()).isEqualTo("?");
	}

	@Test
	void shouldBindByIndex() {
		BindTarget bindTarget = mock();

		BindMarkers bindMarkers = BindMarkersFactory.anonymous("?").create();

		BindMarker first = bindMarkers.next();
		BindMarker second = bindMarkers.next();

		second.bind(bindTarget, "foo");
		first.bindNull(bindTarget, Object.class);

		verify(bindTarget).bindNull(0, Object.class);
		verify(bindTarget).bind(1, "foo");
	}

}
