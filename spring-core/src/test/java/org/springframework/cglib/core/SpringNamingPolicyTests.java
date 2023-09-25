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

package org.springframework.cglib.core;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.cglib.reflect.FastClass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringNamingPolicy}.
 *
 * @author Sam Brannen
 * @since 6.0.13
 */
class SpringNamingPolicyTests {

	private final Set<String> reservedClassNames = new HashSet<>();


	@Test
	void nullPrefix() {
		assertThat(getClassName(null)).isEqualTo("org.springframework.cglib.empty.Object$$SpringCGLIB$$0");
		assertThat(getClassName(null)).isEqualTo("org.springframework.cglib.empty.Object$$SpringCGLIB$$1");
	}

	@Test
	void javaPrefix() {
		assertThat(getClassName("java.util.ArrayList")).isEqualTo("_java.util.ArrayList$$SpringCGLIB$$0");
		assertThat(getClassName("java.util.ArrayList")).isEqualTo("_java.util.ArrayList$$SpringCGLIB$$1");
	}

	@Test
	void javaxPrefix() {
		assertThat(getClassName("javax.sql.RowSet")).isEqualTo("_javax.sql.RowSet$$SpringCGLIB$$0");
		assertThat(getClassName("javax.sql.RowSet")).isEqualTo("_javax.sql.RowSet$$SpringCGLIB$$1");
	}

	@Test
	void examplePrefix() {
		assertThat(getClassName("example.MyComponent")).isEqualTo("example.MyComponent$$SpringCGLIB$$0");
		assertThat(getClassName("example.MyComponent")).isEqualTo("example.MyComponent$$SpringCGLIB$$1");
	}

	@Test
	void prefixContainingSpringLabel() {
		String generated1 = "example.MyComponent$$SpringCGLIB$$0";
		String generated2 = "example.MyComponent$$SpringCGLIB$$1";

		assertThat(getClassName(generated1)).isEqualTo(generated1);
		assertThat(getClassName(generated1)).isEqualTo(generated2);
	}

	@Test
	void fastClass() {
		String prefix = "example.MyComponent";
		String source = FastClass.class.getName();
		assertThat(getClassName(prefix, "a.b.c", null)).isEqualTo("example.MyComponent$$SpringCGLIB$$0");
		assertThat(getClassName(prefix, source, null)).isEqualTo("example.MyComponent$$SpringCGLIB$$FastClass$$0");
		assertThat(getClassName(prefix, source, null)).isEqualTo("example.MyComponent$$SpringCGLIB$$FastClass$$1");
	}

	private String getClassName(String prefix) {
		return getClassName(prefix, null, null);
	}

	private String getClassName(String prefix, String source, Object key) {
		String className = SpringNamingPolicy.INSTANCE.getClassName(prefix, source, key, reservedClassNames::contains);
		reservedClassNames.add(className);
		return className;
	}

}
