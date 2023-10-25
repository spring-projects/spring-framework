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

package org.springframework.core.env;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompositePropertySource}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class CompositePropertySourceTests {

	@Test
	void addFirst() {
		PropertySource<?> p1 = new MapPropertySource("p1", Map.of());
		PropertySource<?> p2 = new MapPropertySource("p2", Map.of());
		PropertySource<?> p3 = new MapPropertySource("p3", Map.of());
		CompositePropertySource composite = new CompositePropertySource("c");
		composite.addPropertySource(p2);
		composite.addPropertySource(p3);
		composite.addPropertySource(p1);
		composite.addFirstPropertySource(p1);

		assertThat(composite.getPropertySources()).extracting(PropertySource::getName).containsExactly("p1", "p2", "p3");
		assertThat(composite).asString().containsSubsequence("name='p1'", "name='p2'", "name='p3'");
	}

	@Test
	void getPropertyNamesRemovesDuplicates() {
		CompositePropertySource composite = new CompositePropertySource("c");
		composite.addPropertySource(new MapPropertySource("p1", Map.of("p1.property", "value")));
		composite.addPropertySource(new MapPropertySource("p2",
				Map.of("p2.property1", "value", "p1.property", "value", "p2.property2", "value")));
		assertThat(composite.getPropertyNames()).containsOnly("p1.property", "p2.property1", "p2.property2");
	}

}
