/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutableMode}.
 *
 * @author Stephane Nicoll
 */
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
