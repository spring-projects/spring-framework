/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aot.hint

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ThrowingConsumer
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import java.lang.reflect.Method

/**
 * Tests for Kotlin support in [BindingReflectionHintsRegistrar].
 *
 * @author Sebastien Deleuze
 */
class BindingReflectionHintsRegistrarKotlinTests {

	private val bindingRegistrar = BindingReflectionHintsRegistrar()

	private val hints = RuntimeHints()

	@Test
	fun `Register reflection hints for Kotlinx serialization`() {
		bindingRegistrar.registerReflectionHints(hints.reflection(), SampleSerializableClass::class.java)
		assertThat(hints.reflection().typeHints()).satisfiesExactlyInAnyOrder(
			ThrowingConsumer { typeHint: TypeHint ->
				assertThat(typeHint.type).isEqualTo(TypeReference.of(String::class.java))
				assertThat(typeHint.memberCategories).isEmpty()
				assertThat(typeHint.constructors()).isEmpty()
				assertThat(typeHint.fields()).isEmpty()
				assertThat(typeHint.methods()).isEmpty()
			},
			ThrowingConsumer { typeHint: TypeHint ->
				assertThat(typeHint.type).isEqualTo(TypeReference.of(SampleSerializableClass::class.java))
				assertThat(typeHint.methods()).singleElement()
					.satisfies(ThrowingConsumer { methodHint: ExecutableHint ->
						assertThat(methodHint.name).isEqualTo("getName")
						assertThat(methodHint.mode).isEqualTo(ExecutableMode.INVOKE)
					})
			},
			ThrowingConsumer { typeHint: TypeHint ->
				assertThat(typeHint.type).isEqualTo(TypeReference.of(SampleSerializableClass::class.qualifiedName + "\$Companion"))
				assertThat(typeHint.methods()).singleElement()
					.satisfies(ThrowingConsumer { methodHint: ExecutableHint ->
						assertThat(methodHint.name).isEqualTo("serializer")
						assertThat(methodHint.mode).isEqualTo(ExecutableMode.INVOKE)
					})
			})
	}

	@Test
	fun `Register reflection hints for Kotlin data class`() {
		bindingRegistrar.registerReflectionHints(hints.reflection(), SampleDataClass::class.java)
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleDataClass::class.java, "component1")).accepts(hints)
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleDataClass::class.java, "copy")).accepts(hints)
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleDataClass::class.java, "getName")).accepts(hints)
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleDataClass::class.java, "isNonNullable")).accepts(hints)
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleDataClass::class.java, "isNullable")).accepts(hints)
		val copyDefault: Method = SampleDataClass::class.java.getMethod("copy\$default", SampleDataClass::class.java,
			String::class.java, Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType, Int::class.java, Object::class.java)
		assertThat(RuntimeHintsPredicates.reflection().onMethod(copyDefault)).accepts(hints)
	}

	@Test
	fun `Register reflection hints on declared methods for Kotlin class`() {
		bindingRegistrar.registerReflectionHints(hints.reflection(), SampleClass::class.java)
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClass::class.java)
			.withMemberCategory(MemberCategory.INTROSPECT_DECLARED_METHODS)).accepts(hints)
	}
}

@kotlinx.serialization.Serializable
class SampleSerializableClass(val name: String)

data class SampleDataClass(val name: String, val isNonNullable: Boolean, val isNullable: Boolean?)

class SampleClass(val name: String)
