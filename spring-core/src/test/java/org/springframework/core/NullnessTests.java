/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.core.testfixture.nullness.ClassMarkedJSpecifyProcessor;
import org.springframework.core.testfixture.nullness.CustomNullableProcessor;
import org.springframework.core.testfixture.nullness.JSpecifyProcessor;
import org.springframework.core.testfixture.nullness.NullnessFields;
import org.springframework.core.testfixture.nullness.marked.PackageMarkedJSpecifyProcessor;
import org.springframework.core.testfixture.nullness.marked.unmarked.PackageUnmarkedJSpecifyProcessor;

/**
 * Tests for {@link Nullness}.
 *
 * @author Sebastien Deleuze
 */
public class NullnessTests {

	// JSpecify without @NullMarked and @NullUnmarked

	@Test
	void jspecifyUnspecifiedReturnType() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyNullableReturnType() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("nullableProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyNonNullReturnType() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("nonNullProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyUnspecifiedParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyNullableParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[1]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyNonNullParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[2]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// JSpecify with MethodParameter without @NullMarked and @NullUnmarked

	@Test
	void jspecifyUnspecifiedReturnTypeWithMethodParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var methodParameter = MethodParameter.forExecutable(method, -1);
		var nullness = Nullness.forMethodParameter(methodParameter);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyNullableReturnTypeWithMethodParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("nullableProcess");
		var methodParameter = MethodParameter.forExecutable(method, -1);
		var nullness = Nullness.forMethodParameter(methodParameter);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyNonNullReturnTypeWithMethodParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("nonNullProcess");
		var methodParameter = MethodParameter.forExecutable(method, -1);
		var nullness = Nullness.forMethodParameter(methodParameter);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyUnspecifiedParameterWithMethodParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var methodParameter = MethodParameter.forExecutable(method, 0);
		var nullness = Nullness.forMethodParameter(methodParameter);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyNullableParameterWithMethodParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var methodParameter = MethodParameter.forExecutable(method, 1);
		var nullness = Nullness.forMethodParameter(methodParameter);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyNonNullParameterWithMethodParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var methodParameter = MethodParameter.forExecutable(method, 2);
		var nullness = Nullness.forMethodParameter(methodParameter);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// JSpecify with Field without @NullMarked and @NullUnmarked

	@Test
	void jspecifyUnspecifiedWithField() throws NoSuchFieldException {
		var field = NullnessFields.class.getDeclaredField("unannotatedField");
		var nullness = Nullness.forField(field);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyNullableWithField() throws NoSuchFieldException {
		var field = NullnessFields.class.getDeclaredField("jspecifyNullableField");
		var nullness = Nullness.forField(field);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyNonNullWithField() throws NoSuchFieldException {
		var field = NullnessFields.class.getDeclaredField("jspecifyNonNullField");
		var nullness = Nullness.forField(field);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// JSpecify with method-level @NullMarked

	@Test
	void jspecifyMethodMarkedUnspecifiedReturnType() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("markedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyMethodMarkedNullableReturnType() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("nullableMarkedProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyMethodMarkedNonNullReturnType() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("nonNullMarkedProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyMethodMarkedUnspecifiedParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("markedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyMethodMarkedNullableParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("markedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[1]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyMethodMarkedNonNullParameter() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("markedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[2]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// JSpecify with class-level @NullMarked

	@Test
	void jspecifyClassMarkedUnspecifiedReturnType() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyClassMarkedNullableReturnType() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("nullableProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyClassMarkedNonNullReturnType() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("nonNullProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyClassMarkedUnspecifiedParameter() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyClassMarkedNullableParameter() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[1]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyClassMarkedNonNullParameter() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[2]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyClassMarkedMethodUnmarkedUnspecifiedReturnType() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("unmarkedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyClassMarkedMethodUnmarkedUnspecifiedParameter() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("unmarkedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyClassMarkedMethodUnmarkedNullableParameter() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("unmarkedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[1]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyClassMarkedMethodUnmarkedNonNullParameter() throws NoSuchMethodException {
		var method = ClassMarkedJSpecifyProcessor.class.getMethod("unmarkedProcess", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[2]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// JSpecify with package-level @NullMarked

	@Test
	void jspecifyPackageMarkedUnspecifiedReturnType() throws NoSuchMethodException {
		var method = PackageMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyPackageMarkedNullableReturnType() throws NoSuchMethodException {
		var method = PackageMarkedJSpecifyProcessor.class.getMethod("nullableProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyPackageMarkedNonNullReturnType() throws NoSuchMethodException {
		var method = PackageMarkedJSpecifyProcessor.class.getMethod("nonNullProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyPackageMarkedUnspecifiedParameter() throws NoSuchMethodException {
		var method = PackageMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyPackageMarkedNullableParameter() throws NoSuchMethodException {
		var method = PackageMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[1]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyPackageMarkedNonNullParameter() throws NoSuchMethodException {
		var method = PackageMarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[2]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// JSpecify with package-level @NullUnmarked

	@Test
	void jspecifyPackageUnmarkedUnspecifiedReturnType() throws NoSuchMethodException {
		var method = PackageUnmarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyPackageUnmarkedNullableReturnType() throws NoSuchMethodException {
		var method = PackageUnmarkedJSpecifyProcessor.class.getMethod("nullableProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyPackageUnmarkedNonNullReturnType() throws NoSuchMethodException {
		var method = PackageUnmarkedJSpecifyProcessor.class.getMethod("nonNullProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void jspecifyPackageUnmarkedUnspecifiedParameter() throws NoSuchMethodException {
		var method = PackageUnmarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

	@Test
	void jspecifyPackageUnmarkedNullableParameter() throws NoSuchMethodException {
		var method = PackageUnmarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[1]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void jspecifyPackageUnmarkedNonNullParameter() throws NoSuchMethodException {
		var method = PackageUnmarkedJSpecifyProcessor.class.getMethod("process", String.class, String.class, String.class);
		var nullness = Nullness.forParameter(method.getParameters()[2]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	// Custom @Nullable

	@Test
	void customNullableReturnType() throws NoSuchMethodException {
		var method = CustomNullableProcessor.class.getMethod("process", String.class);
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void customNullableParameter() throws NoSuchMethodException {
		var method = CustomNullableProcessor.class.getMethod("process", String.class);
		var nullness = Nullness.forParameter(method.getParameters()[0]);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	@Test
	void customNullableField() throws NoSuchFieldException {
		var field = NullnessFields.class.getDeclaredField("customNullableField");
		var nullness = Nullness.forField(field);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NULLABLE);
	}

	// Primitive types

	@Test
	void primitiveField() throws NoSuchFieldException {
		var field = NullnessFields.class.getDeclaredField("primitiveField");
		var nullness = Nullness.forField(field);
		Assertions.assertThat(nullness).isEqualTo(Nullness.NON_NULL);
	}

	@Test
	void voidMethod() throws NoSuchMethodException {
		var method = JSpecifyProcessor.class.getMethod("voidProcess");
		var nullness = Nullness.forMethodReturnType(method);
		Assertions.assertThat(nullness).isEqualTo(Nullness.UNSPECIFIED);
	}

}
