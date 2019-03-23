/*
 * Copyright 2002-2015 the original author or authors.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link SystemEnvironmentPropertySource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class SystemEnvironmentPropertySourceTests {

	private Map<String, Object> envMap;

	private PropertySource<?> ps;


	@Before
	public void setUp() {
		envMap = new HashMap<String, Object>();
		ps = new SystemEnvironmentPropertySource("sysEnv", envMap);
	}


	@Test
	public void none() {
		assertThat(ps.containsProperty("a.key"), equalTo(false));
		assertThat(ps.getProperty("a.key"), equalTo(null));
	}

	@Test
	public void normalWithoutPeriod() {
		envMap.put("akey", "avalue");

		assertThat(ps.containsProperty("akey"), equalTo(true));
		assertThat(ps.getProperty("akey"), equalTo((Object)"avalue"));
	}

	@Test
	public void normalWithPeriod() {
		envMap.put("a.key", "a.value");

		assertThat(ps.containsProperty("a.key"), equalTo(true));
		assertThat(ps.getProperty("a.key"), equalTo((Object)"a.value"));
	}

	@Test
	public void withUnderscore() {
		envMap.put("a_key", "a_value");

		assertThat(ps.containsProperty("a_key"), equalTo(true));
		assertThat(ps.containsProperty("a.key"), equalTo(true));

		assertThat(ps.getProperty("a_key"), equalTo((Object)"a_value"));
		assertThat( ps.getProperty("a.key"), equalTo((Object)"a_value"));
	}

	@Test
	public void withBothPeriodAndUnderscore() {
		envMap.put("a_key", "a_value");
		envMap.put("a.key", "a.value");

		assertThat(ps.getProperty("a_key"), equalTo((Object)"a_value"));
		assertThat( ps.getProperty("a.key"), equalTo((Object)"a.value"));
	}

	@Test
	public void withUppercase() {
		envMap.put("A_KEY", "a_value");
		envMap.put("A_LONG_KEY", "a_long_value");
		envMap.put("A_DOT.KEY", "a_dot_value");
		envMap.put("A_HYPHEN-KEY", "a_hyphen_value");

		assertThat(ps.containsProperty("A_KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-KEY"), equalTo(true));
		assertThat(ps.containsProperty("a_key"), equalTo(true));
		assertThat(ps.containsProperty("a.key"), equalTo(true));
		assertThat(ps.containsProperty("a-key"), equalTo(true));
		assertThat(ps.containsProperty("A_LONG_KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.LONG.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-LONG-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.LONG-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-LONG.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A_long_KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.long.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-long-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.long-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-long.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A_DOT.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-DOT.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A_dot.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A-dot.KEY"), equalTo(true));
		assertThat(ps.containsProperty("A_HYPHEN-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.HYPHEN-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A_hyphen-KEY"), equalTo(true));
		assertThat(ps.containsProperty("A.hyphen-KEY"), equalTo(true));

		assertThat(ps.getProperty("A_KEY"), equalTo("a_value"));
		assertThat(ps.getProperty("A.KEY"), equalTo("a_value"));
		assertThat(ps.getProperty("A-KEY"), equalTo("a_value"));
		assertThat(ps.getProperty("a_key"), equalTo("a_value"));
		assertThat(ps.getProperty("a.key"), equalTo("a_value"));
		assertThat(ps.getProperty("a-key"), equalTo("a_value"));
		assertThat(ps.getProperty("A_LONG_KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A.LONG.KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A-LONG-KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A.LONG-KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A-LONG.KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A_long_KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A.long.KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A-long-KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A.long-KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A-long.KEY"), equalTo("a_long_value"));
		assertThat(ps.getProperty("A_DOT.KEY"), equalTo("a_dot_value"));
		assertThat(ps.getProperty("A-DOT.KEY"), equalTo("a_dot_value"));
		assertThat(ps.getProperty("A_dot.KEY"), equalTo("a_dot_value"));
		assertThat(ps.getProperty("A-dot.KEY"), equalTo("a_dot_value"));
		assertThat(ps.getProperty("A_HYPHEN-KEY"), equalTo("a_hyphen_value"));
		assertThat(ps.getProperty("A.HYPHEN-KEY"), equalTo("a_hyphen_value"));
		assertThat(ps.getProperty("A_hyphen-KEY"), equalTo("a_hyphen_value"));
		assertThat(ps.getProperty("A.hyphen-KEY"), equalTo("a_hyphen_value"));
	}

	@Test
	@SuppressWarnings("serial")
	public void withSecurityConstraints() throws Exception {
		envMap = new HashMap<String, Object>() {
			@Override
			public boolean containsKey(Object key) {
				throw new UnsupportedOperationException();
			}
			@Override
			public Set<String> keySet() {
				return new HashSet<String>(super.keySet());
			}
		};
		envMap.put("A_KEY", "a_value");

		ps = new SystemEnvironmentPropertySource("sysEnv", envMap) {
			@Override
			protected boolean isSecurityManagerPresent() {
				return true;
			}
		};

		assertThat(ps.containsProperty("A_KEY"), equalTo(true));
		assertThat(ps.getProperty("A_KEY"), equalTo((Object)"a_value"));
	}

}
