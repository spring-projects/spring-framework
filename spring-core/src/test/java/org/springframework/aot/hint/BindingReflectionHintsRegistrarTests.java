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

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BindingReflectionHintsRegistrar}.
 *
 * @author Sebastien Deleuze
 */
class BindingReflectionHintsRegistrarTests {

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
						assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
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
						assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
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
								assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
							},
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("getNames");
								assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
							});
				});
	}

	@Test
	void registerTypeForSerializationWithCycles() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithCycles.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassWithCycles.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(List.class)));
	}

	@Test
	void registerTypeForSerializationWithResolvableType() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithResolvableType.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(ResolvableType[].class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Type.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Class.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(ResolvableType.class));
					assertThat(typeHint.getMemberCategories()).containsExactlyInAnyOrder(
							MemberCategory.DECLARED_FIELDS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).hasSizeGreaterThan(1);
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassWithResolvableType.class));
					assertThat(typeHint.methods()).singleElement().satisfies(
							methodHint -> {
								assertThat(methodHint.getName()).isEqualTo("getResolvableType");
								assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
							});
				});
	}

	@Test
	void registerTypeForSerializationWithMultipleLevelsAndCollection() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassA.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassA.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassB.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleClassC.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class)),
				typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(Set.class)));
	}

	@Test
	void registerTypeForSerializationWithEnum() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleEnum.class);
		assertThat(this.hints.reflection().typeHints()).singleElement()
				.satisfies(typeHint -> assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleEnum.class)));
	}

	@Test
	void registerTypeForSerializationWithRecord() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleRecord.class);
		assertThat(this.hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(String.class));
					assertThat(typeHint.getMemberCategories()).isEmpty();
					assertThat(typeHint.constructors()).isEmpty();
					assertThat(typeHint.fields()).isEmpty();
					assertThat(typeHint.methods()).isEmpty();
				},
				typeHint -> {
					assertThat(typeHint.getType()).isEqualTo(TypeReference.of(SampleRecord.class));
					assertThat(typeHint.methods()).singleElement().satisfies(methodHint -> {
						assertThat(methodHint.getName()).isEqualTo("name");
						assertThat(methodHint.getMode()).isEqualTo(ExecutableMode.INVOKE);
					});
				});
	}

	@Test
	void registerTypeForSerializationWithRecordWithProperty() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleRecordWithProperty.class);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleRecordWithProperty.class, "getNameProperty"))
				.accepts(this.hints);
	}

	@Test
	void registerTypeForSerializationWithAnonymousClass() {
		Runnable anonymousRunnable = () -> { };
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), anonymousRunnable.getClass());
	}

	@Test
	void registerTypeForJacksonAnnotations() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithJsonProperty.class);
		assertThat(RuntimeHintsPredicates.reflection().onField(SampleClassWithJsonProperty.class, "privateField"))
				.accepts(this.hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithJsonProperty.class, "packagePrivateMethod").invoke())
				.accepts(this.hints);
	}

	@Test
	void registerTypeForInheritedJacksonAnnotations() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithInheritedJsonProperty.class);
		assertThat(RuntimeHintsPredicates.reflection().onField(SampleClassWithJsonProperty.class, "privateField"))
				.accepts(this.hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithJsonProperty.class, "packagePrivateMethod").invoke())
				.accepts(this.hints);
	}

	@Test
	void registerTypeForJacksonCustomStrategy() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleRecordWithJacksonCustomStrategy.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(PropertyNamingStrategies.UpperSnakeCaseStrategy.class).withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
				.accepts(this.hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleRecordWithJacksonCustomStrategy.Builder.class).withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
				.accepts(this.hints);
	}

	@Test
	void registerTypeForAnnotationOnMethodAndField() {
		bindingRegistrar.registerReflectionHints(this.hints.reflection(), SampleClassWithJsonProperty.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(CustomDeserializer1.class).withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
				.accepts(this.hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(CustomDeserializer2.class).withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS))
				.accepts(this.hints);
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

	static class SampleClassWithCycles {

		public SampleClassWithCycles getSampleClassWithCycles() {
			return null;
		}

		public List<SampleClassWithCycles> getSampleClassWithCyclesList() {
			return null;
		}
	}

	static class SampleClassWithResolvableType {

		public ResolvableType getResolvableType() {
			return null;
		}
	}

	static class SampleClassA {
		public Set<SampleClassB> getB() {
			return null;
		}
	}

	static class SampleClassB {
		public SampleClassC getC() {
			return null;
		}
	}

	static class SampleClassC {
		public String getString() {
			return "";
		}
	}

	enum SampleEnum {
		value1, value2
	}

	record SampleRecord(String name) {}

	record SampleRecordWithProperty(String name) {

		public String getNameProperty() {
			return "";
		}
	}

	static class SampleClassWithJsonProperty {

		@JsonProperty
		@JsonDeserialize(using = CustomDeserializer1.class)
		private String privateField = "";

		@JsonProperty
		@JsonDeserialize(using = CustomDeserializer2.class)
		String packagePrivateMethod() {
			return "";
		}
	}

	static class SampleClassWithInheritedJsonProperty extends SampleClassWithJsonProperty {}

	@JsonNaming(PropertyNamingStrategies.UpperSnakeCaseStrategy.class)
	@JsonDeserialize(builder = SampleRecordWithJacksonCustomStrategy.Builder.class)
	record SampleRecordWithJacksonCustomStrategy(String name) {

		@JsonPOJOBuilder(withPrefix = "")
		public static class Builder {
			private String name;

			public static Builder newInstance() {
				return new Builder();
			}

			public Builder name(String name) {
				this.name = name;
				return this;
			}

			public SampleRecordWithJacksonCustomStrategy build() {
				return new SampleRecordWithJacksonCustomStrategy(name);
			}
		}

	}

	@SuppressWarnings("serial")
	static class CustomDeserializer1 extends StdDeserializer<LocalDate> {

		public CustomDeserializer1() {
			super(CustomDeserializer1.class);
		}

		@Override
		public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
			return null;
		}
	}

	@SuppressWarnings("serial")
	static class CustomDeserializer2 extends StdDeserializer<LocalDate> {

		public CustomDeserializer2() {
			super(CustomDeserializer2.class);
		}

		@Override
		public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
			return null;
		}
	}

}
