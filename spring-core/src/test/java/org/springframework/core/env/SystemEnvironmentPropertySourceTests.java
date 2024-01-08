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

package org.springframework.core.env;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemEnvironmentPropertySource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
class SystemEnvironmentPropertySourceTests {

	private Map<String, Object> envMap;

	private PropertySource<?> ps;


	@BeforeEach
	void setUp() {
		envMap = new HashMap<>();
		ps = new SystemEnvironmentPropertySource("sysEnv", envMap);
	}


	@Test
	void none() {
		assertThat(ps.containsProperty("a.key")).isFalse();
		assertThat(ps.getProperty("a.key")).isNull();
	}

	@Test
	void normalWithoutPeriod() {
		envMap.put("akey", "avalue");

		assertThat(ps.containsProperty("akey")).isTrue();
		assertThat(ps.getProperty("akey")).isEqualTo("avalue");
	}

	@Test
	void normalWithPeriod() {
		envMap.put("a.key", "a.value");

		assertThat(ps.containsProperty("a.key")).isTrue();
		assertThat(ps.getProperty("a.key")).isEqualTo("a.value");
	}

	@Test
	void withUnderscore() {
		envMap.put("a_key", "a_value");

		assertThat(ps.containsProperty("a_key")).isTrue();
		assertThat(ps.containsProperty("a.key")).isTrue();

		assertThat(ps.getProperty("a_key")).isEqualTo("a_value");
		assertThat( ps.getProperty("a.key")).isEqualTo("a_value");
	}

	@Test
	void withBothPeriodAndUnderscore() {
		envMap.put("a_key", "a_value");
		envMap.put("a.key", "a.value");

		assertThat(ps.getProperty("a_key")).isEqualTo("a_value");
		assertThat( ps.getProperty("a.key")).isEqualTo("a.value");
	}

	@Test
	void withUppercase() {
		envMap.put("A_KEY", "a_value");
		envMap.put("A_LONG_KEY", "a_long_value");
		envMap.put("A_DOT.KEY", "a_dot_value");
		envMap.put("A_HYPHEN-KEY", "a_hyphen_value");

		assertThat(ps.containsProperty("A_KEY")).isTrue();
		assertThat(ps.containsProperty("A.KEY")).isTrue();
		assertThat(ps.containsProperty("A-KEY")).isTrue();
		assertThat(ps.containsProperty("a_key")).isTrue();
		assertThat(ps.containsProperty("a.key")).isTrue();
		assertThat(ps.containsProperty("a-key")).isTrue();
		assertThat(ps.containsProperty("A_LONG_KEY")).isTrue();
		assertThat(ps.containsProperty("A.LONG.KEY")).isTrue();
		assertThat(ps.containsProperty("A-LONG-KEY")).isTrue();
		assertThat(ps.containsProperty("A.LONG-KEY")).isTrue();
		assertThat(ps.containsProperty("A-LONG.KEY")).isTrue();
		assertThat(ps.containsProperty("A_long_KEY")).isTrue();
		assertThat(ps.containsProperty("A.long.KEY")).isTrue();
		assertThat(ps.containsProperty("A-long-KEY")).isTrue();
		assertThat(ps.containsProperty("A.long-KEY")).isTrue();
		assertThat(ps.containsProperty("A-long.KEY")).isTrue();
		assertThat(ps.containsProperty("A_DOT.KEY")).isTrue();
		assertThat(ps.containsProperty("A-DOT.KEY")).isTrue();
		assertThat(ps.containsProperty("A_dot.KEY")).isTrue();
		assertThat(ps.containsProperty("A-dot.KEY")).isTrue();
		assertThat(ps.containsProperty("A_HYPHEN-KEY")).isTrue();
		assertThat(ps.containsProperty("A.HYPHEN-KEY")).isTrue();
		assertThat(ps.containsProperty("A_hyphen-KEY")).isTrue();
		assertThat(ps.containsProperty("A.hyphen-KEY")).isTrue();

		assertThat(ps.getProperty("A_KEY")).isEqualTo("a_value");
		assertThat(ps.getProperty("A.KEY")).isEqualTo("a_value");
		assertThat(ps.getProperty("A-KEY")).isEqualTo("a_value");
		assertThat(ps.getProperty("a_key")).isEqualTo("a_value");
		assertThat(ps.getProperty("a.key")).isEqualTo("a_value");
		assertThat(ps.getProperty("a-key")).isEqualTo("a_value");
		assertThat(ps.getProperty("A_LONG_KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A.LONG.KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A-LONG-KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A.LONG-KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A-LONG.KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A_long_KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A.long.KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A-long-KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A.long-KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A-long.KEY")).isEqualTo("a_long_value");
		assertThat(ps.getProperty("A_DOT.KEY")).isEqualTo("a_dot_value");
		assertThat(ps.getProperty("A-DOT.KEY")).isEqualTo("a_dot_value");
		assertThat(ps.getProperty("A_dot.KEY")).isEqualTo("a_dot_value");
		assertThat(ps.getProperty("A-dot.KEY")).isEqualTo("a_dot_value");
		assertThat(ps.getProperty("A_HYPHEN-KEY")).isEqualTo("a_hyphen_value");
		assertThat(ps.getProperty("A.HYPHEN-KEY")).isEqualTo("a_hyphen_value");
		assertThat(ps.getProperty("A_hyphen-KEY")).isEqualTo("a_hyphen_value");
		assertThat(ps.getProperty("A.hyphen-KEY")).isEqualTo("a_hyphen_value");
	}

}
