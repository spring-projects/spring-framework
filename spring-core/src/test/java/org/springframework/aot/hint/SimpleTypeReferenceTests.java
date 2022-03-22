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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SimpleTypeReference}.
 *
 * @author Stephane Nicoll
 */
class SimpleTypeReferenceTests {

	@Test
	void typeReferenceInRootPackage() {
		TypeReference type = SimpleTypeReference.of("MyRootClass");
		assertThat(type.getCanonicalName()).isEqualTo("MyRootClass");
		assertThat(type.getPackageName()).isEqualTo("");
	}

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "com.example.Tes(t", "com.example..Test" })
	void typeReferenceWithInvalidClassName(String invalidClassName) {
		assertThatIllegalStateException().isThrownBy(() -> SimpleTypeReference.of(invalidClassName))
				.withMessageContaining("Invalid class name");
	}

}
