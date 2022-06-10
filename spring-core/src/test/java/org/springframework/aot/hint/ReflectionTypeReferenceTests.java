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

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReflectionTypeReference}.
 *
 * @author Stephane Nicoll
 */
class ReflectionTypeReferenceTests {

	@ParameterizedTest
	@MethodSource("reflectionTargetNames")
	void typeReferenceFromClasHasSuitableReflectionTargetName(TypeReference typeReference, String binaryName) {
		assertThat(typeReference.getName()).isEqualTo(binaryName);
	}

	static Stream<Arguments> reflectionTargetNames() {
		return Stream.of(Arguments.of(ReflectionTypeReference.of(int.class), "int"),
				Arguments.of(ReflectionTypeReference.of(int[].class), "int[]"),
				Arguments.of(ReflectionTypeReference.of(Integer[].class), "java.lang.Integer[]"),
				Arguments.of(ReflectionTypeReference.of(Object[].class), "java.lang.Object[]"));
	}

}
