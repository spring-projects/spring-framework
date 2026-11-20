/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

	@Test  // gh-37540
	void toStringUsesConciseFormatForNestedPropertySources() {
		CompositePropertySource composite = new CompositePropertySource("c");
		composite.addPropertySource(new MapPropertySource("p1", Map.of("p1.property", "value")));
		composite.addPropertySource(new MapPropertySource("p2", Map.of("p2.property", "value")));

		assertThat(composite).hasToString("CompositePropertySource {name='c', propertySources=" +
				"[MapPropertySource {name='p1'}, MapPropertySource {name='p2'}]}");
	}

}
