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

package org.springframework.aot.hint;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Tests for {@link ReflectionTypeReference}.
 *
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Sebastien Deleuze
 */
class ReflectionTypeReferenceTests {

	@Test
	void typeReferenceWithNullClass() {
		assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTypeReference.of(null));
	}

	@Test
	void typeReferenceWithLambda() {
		Runnable lambda = () -> { };
		assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTypeReference.of(lambda.getClass()));
	}

	@ParameterizedTest
	@MethodSource("reflectionTargetNames")
	void typeReferenceFromClassHasSuitableReflectionTargetName(Class<?> clazz, String binaryName) {
		assertThat(ReflectionTypeReference.of(clazz).getName()).isEqualTo(binaryName);
	}

	static Stream<Arguments> reflectionTargetNames() {
		return Stream.of(
				arguments(int.class, "int"),
				arguments(int[].class, "int[]"),
				arguments(Integer[].class, "java.lang.Integer[]"),
				arguments(Object[].class, "java.lang.Object[]"),
				arguments(StaticNested.class, "org.springframework.aot.hint.ReflectionTypeReferenceTests$StaticNested"),
				arguments(StaticNested[].class, "org.springframework.aot.hint.ReflectionTypeReferenceTests$StaticNested[]"),
				arguments(Inner.class, "org.springframework.aot.hint.ReflectionTypeReferenceTests$Inner"),
				arguments(Inner[].class, "org.springframework.aot.hint.ReflectionTypeReferenceTests$Inner[]")
		);
	}

	static class StaticNested {
	}

	class Inner {
	}

}
