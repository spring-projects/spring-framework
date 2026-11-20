/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.env;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertySource} implementations.
 *
 * @author Chris Beams
 * @since 3.1
 */
class PropertySourceTests {

	@Test
	@SuppressWarnings("serial")
	void equals() {
		Map<String, Object> map1 = Map.of("a", "b");
		Map<String, Object> map2 = Map.of("c", "d");
		Properties props1 = new Properties() {{
			setProperty("a", "b");
		}};
		Properties props2 = new Properties() {{
			setProperty("c", "d");
		}};

		MapPropertySource mps = new MapPropertySource("mps", map1);
		assertThat(mps).isEqualTo(mps);

		assertThat(new MapPropertySource("x", map1)).isEqualTo(new MapPropertySource("x", map1));
		assertThat(new MapPropertySource("x", map1)).isEqualTo(new MapPropertySource("x", map2));
		assertThat(new MapPropertySource("x", map1)).isEqualTo(new PropertiesPropertySource("x", props1));
		assertThat(new MapPropertySource("x", map1)).isEqualTo(new PropertiesPropertySource("x", props2));

		assertThat(new MapPropertySource("x", map1)).isNotEqualTo(new Object());
		assertThat(new MapPropertySource("x", map1)).isNotEqualTo("x");
		assertThat(new MapPropertySource("x", map1)).isNotEqualTo(new MapPropertySource("y", map1));
		assertThat(new MapPropertySource("x", map1)).isNotEqualTo(new MapPropertySource("y", map2));
		assertThat(new MapPropertySource("x", map1)).isNotEqualTo(new PropertiesPropertySource("y", props1));
		assertThat(new MapPropertySource("x", map1)).isNotEqualTo(new PropertiesPropertySource("y", props2));
	}

	@Test
	void collectionsOperations() {
		Map<String, Object> map1 = Map.of("a", "b");
		Map<String, Object> map2 = Map.of("c", "d");

		PropertySource<?> ps1 = new MapPropertySource("ps1", map1);
		ps1.getSource();
		List<PropertySource<?>> propertySources = new ArrayList<>();
		assertThat(propertySources.add(ps1)).isTrue();
		assertThat(propertySources).contains(ps1);
		assertThat(propertySources).contains(PropertySource.named("ps1"));

		PropertySource<?> ps1replacement = new MapPropertySource("ps1", map2); // notice - different map
		assertThat(propertySources.add(ps1replacement)).isTrue(); // true because linkedlist allows duplicates
		assertThat(propertySources).hasSize(2);
		assertThat(propertySources.remove(PropertySource.named("ps1"))).isTrue();
		assertThat(propertySources).hasSize(1);
		assertThat(propertySources.remove(PropertySource.named("ps1"))).isTrue();
		assertThat(propertySources).isEmpty();

		PropertySource<?> ps2 = new MapPropertySource("ps2", map2);
		propertySources.add(ps1);
		propertySources.add(ps2);
		assertThat(propertySources.indexOf(PropertySource.named("ps1"))).isEqualTo(0);
		assertThat(propertySources.indexOf(PropertySource.named("ps2"))).isEqualTo(1);
		propertySources.clear();
	}

}
