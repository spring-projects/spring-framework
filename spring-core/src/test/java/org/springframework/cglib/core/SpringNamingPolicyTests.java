/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
