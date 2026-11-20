/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutableMode}.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("removal")
class ExecutableModeTests {

	@Test
	void invokeIncludesNullMode() {
		assertThat(ExecutableMode.INVOKE.includes(null)).isTrue();
	}

	@Test
	void invokeIncludesIntrospect() {
		assertThat(ExecutableMode.INVOKE.includes(ExecutableMode.INTROSPECT)).isTrue();
	}

	@Test
	void invokeIncludesIncludes() {
		assertThat(ExecutableMode.INVOKE.includes(ExecutableMode.INVOKE)).isTrue();
	}

	@Test
	void introspectIncludesNullMode() {
		assertThat(ExecutableMode.INTROSPECT.includes(null)).isTrue();
	}

	@Test
	void introspectIncludesIntrospect() {
		assertThat(ExecutableMode.INTROSPECT.includes(ExecutableMode.INTROSPECT)).isTrue();
	}

	@Test
	void introspectDoesNotIncludeInvoke() {
		assertThat(ExecutableMode.INTROSPECT.includes(ExecutableMode.INVOKE)).isFalse();
	}

}
