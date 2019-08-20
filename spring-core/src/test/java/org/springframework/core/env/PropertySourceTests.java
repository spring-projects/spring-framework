/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PropertySource} implementations.
 *
 * @author Chris Beams
 * @since 3.1
 */
class PropertySourceTests {

	@Test
	@SuppressWarnings("serial")
	void equals() {
		Map<String, Object> map1 = new HashMap<String, Object>() {{
			put("a", "b");
		}};
		Map<String, Object> map2 = new HashMap<String, Object>() {{
			put("c", "d");
		}};
		Properties props1 = new Properties() {{
			setProperty("a", "b");
		}};
		Properties props2 = new Properties() {{
			setProperty("c", "d");
		}};

		MapPropertySource mps = new MapPropertySource("mps", map1);
		assertThat(mps).isEqualTo(mps);

		assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("x", map1))).isTrue();
		assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("x", map2))).isTrue();
		assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("x", props1))).isTrue();
		assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("x", props2))).isTrue();

		assertThat(new MapPropertySource("x", map1).equals(new Object())).isFalse();
		assertThat(new MapPropertySource("x", map1).equals("x")).isFalse();
		assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("y", map1))).isFalse();
		assertThat(new MapPropertySource("x", map1).equals(new MapPropertySource("y", map2))).isFalse();
		assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("y", props1))).isFalse();
		assertThat(new MapPropertySource("x", map1).equals(new PropertiesPropertySource("y", props2))).isFalse();
	}

	@Test
	@SuppressWarnings("serial")
	void collectionsOperations() {
		Map<String, Object> map1 = new HashMap<String, Object>() {{
			put("a", "b");
		}};
		Map<String, Object> map2 = new HashMap<String, Object>() {{
			put("c", "d");
		}};

		PropertySource<?> ps1 = new MapPropertySource("ps1", map1);
		ps1.getSource();
		List<PropertySource<?>> propertySources = new ArrayList<>();
		assertThat(propertySources.add(ps1)).isEqualTo(true);
		assertThat(propertySources.contains(ps1)).isTrue();
		assertThat(propertySources.contains(PropertySource.named("ps1"))).isTrue();

		PropertySource<?> ps1replacement = new MapPropertySource("ps1", map2); // notice - different map
		assertThat(propertySources.add(ps1replacement)).isTrue(); // true because linkedlist allows duplicates
		assertThat(propertySources).hasSize(2);
		assertThat(propertySources.remove(PropertySource.named("ps1"))).isTrue();
		assertThat(propertySources).hasSize(1);
		assertThat(propertySources.remove(PropertySource.named("ps1"))).isTrue();
		assertThat(propertySources).hasSize(0);

		PropertySource<?> ps2 = new MapPropertySource("ps2", map2);
		propertySources.add(ps1);
		propertySources.add(ps2);
		assertThat(propertySources.indexOf(PropertySource.named("ps1"))).isEqualTo(0);
		assertThat(propertySources.indexOf(PropertySource.named("ps2"))).isEqualTo(1);
		propertySources.clear();
	}

}
