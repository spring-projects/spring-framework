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

package org.springframework.aot.hint.annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SimpleReflectiveProcessor}.
 *
 * @author Stephane Nicoll
 */
class SimpleReflectiveProcessorTests {

	private final SimpleReflectiveProcessor processor = new SimpleReflectiveProcessor();

	private final ReflectionHints hints = new ReflectionHints();

	@Test
	void registerReflectiveHintsForClass() {
		processor.registerReflectionHints(hints, SampleBean.class);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReflectiveHintsForConstructor() {
		Constructor<?> constructor = SampleBean.class.getDeclaredConstructors()[0];
		processor.registerReflectionHints(hints, constructor);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).singleElement().satisfies(constructorHint -> {
				assertThat(constructorHint.getName()).isEqualTo("<init>");
				assertThat(constructorHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
				assertThat(constructorHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
			});
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReflectiveHintsForField() throws NoSuchFieldException {
		Field field = SampleBean.class.getDeclaredField("name");
		processor.registerReflectionHints(hints, field);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).singleElement().satisfies(fieldHint ->
					assertThat(fieldHint.getName()).isEqualTo("name"));
			assertThat(typeHint.methods()).isEmpty();
		});
	}

	@Test
	void registerReflectiveHintsForMethod() throws NoSuchMethodException {
		Method method = SampleBean.class.getDeclaredMethod("setName", String.class);
		processor.registerReflectionHints(hints, method);
		assertThat(hints.typeHints()).singleElement().satisfies(typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleBean.class));
			assertThat(typeHint.getMemberCategories()).isEmpty();
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
				assertThat(methodHint.getName()).isEqualTo("setName");
				assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
				assertThat(methodHint.getParameterTypes()).containsExactly(TypeReference.of(String.class));
			});
		});
	}

	static class SampleBean {

		@SuppressWarnings("unused")
		private String name;

		SampleBean(String name) {
			this.name = name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
