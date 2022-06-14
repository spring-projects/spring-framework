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

package org.springframework.aot.hint.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindingReflectionHintsRegistrar}.
 *
 * @author Sebastien Deleuze
 */
public class BindingReflectionHintsRegistrarTests {

	private final BindingReflectionHintsRegistrar bindingRegistrar = new BindingReflectionHintsRegistrar();

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void registerTypeForSerializationWithEmptyClass() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleEmptyClass.class);
		assertThat(this.hints.reflection().typeHints()).singleElement()
				.satisfies(typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleEmptyClass.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				});
	}

	@Test
	void registerTypeForSerializationWithNoProperty() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithNoProperty.class);
		assertThat(this.hints.reflection().typeHints()).singleElement()
				.satisfies(typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassWithNoProperty.class)));
	}

	@Test
	void registerTypeForSerializationWithGetter() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithGetter.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassWithGetter.class));
					assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
						assertThat(methodHint.getName()).isEqualTo("getName");
						assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
					});
				});
	}

	@Test
	void registerTypeForSerializationWithSetter() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithSetter.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassWithSetter.class));
					assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
						assertThat(methodHint.getName()).isEqualTo("setName");
						assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
					});
				});
	}

	@Test
	void registerTypeForSerializationWithListProperty() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithListProperty.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(List.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassWithListProperty.class));
					assertThat(typeHint.methods()).satisfiesExactlyInAnyOrder(
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("setNames");
								assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
							},
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("getNames");
								assertThat(methodHint.getModes()).containsOnly(ExecutableMode.INVOKE);
							});
				});
	}

	static class SampleEmptyClass {
	}

	static class SampleClassWithNoProperty {

		String name() {
			return null;
		}
	}

	static class SampleClassWithGetter {

		public String getName() {
			return null;
		}

		public SampleEmptyClass unmanaged() {
			return null;
		}
	}

	static class SampleClassWithSetter {

		public void setName(String name) {
		}

		public SampleEmptyClass unmanaged() {
			return null;
		}
	}

	static class SampleClassWithListProperty {

		public List<String> getNames() {
			return null;
		}

		public void setNames(List<String> names) {
		}
	}

}
