/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
