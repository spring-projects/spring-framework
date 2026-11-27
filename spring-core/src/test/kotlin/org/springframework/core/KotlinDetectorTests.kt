/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

/**
 * Kotlin tests for [KotlinDetector].
 *
 * @author Sebastien Deleuze
 */
class KotlinDetectorTests {

	@Test
	fun isKotlinType() {
		Assertions.assertThat(KotlinDetector.isKotlinType(KotlinDetectorTests::class.java)).isTrue()
	}

	@Test
	fun isNotKotlinType() {
		Assertions.assertThat(KotlinDetector.isKotlinType(KotlinDetector::class.java)).isFalse()
	}

	@Test
	fun isInlineClass() {
		Assertions.assertThat(KotlinDetector.isInlineClass(ValueClass::class.java)).isTrue()
	}

	@Test
	fun isNotInlineClass() {
		Assertions.assertThat(KotlinDetector.isInlineClass(KotlinDetector::class.java)).isFalse()
		Assertions.assertThat(KotlinDetector.isInlineClass(KotlinDetectorTests::class.java)).isFalse()
	}

	@Test
	fun hasSerializableAnnotation() {
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.forClass(WithoutSerializable::class.java))).isFalse()
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.forClass(WithSerializable::class.java))).isTrue()

		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.forClassWithGenerics(List::class.java, WithoutSerializable::class.java))).isFalse()
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.forClassWithGenerics(List::class.java, WithSerializable::class.java))).isTrue()

		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.forClassWithGenerics(Map::class.java, String::class.java, WithoutSerializable::class.java))).isFalse()
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.forClassWithGenerics(Map::class.java, String::class.java, WithSerializable::class.java))).isTrue()
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(
			ResolvableType.forClassWithGenerics(Map::class.java,
				ResolvableType.forClass(String::class.java),
				ResolvableType.forClassWithGenerics(List::class.java, WithSerializable::class.java)))).isTrue()
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(
			ResolvableType.forClassWithGenerics(List::class.java,
			ResolvableType.forClassWithGenerics(List::class.java, WithSerializable::class.java)))).isTrue()

		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(ResolvableType.NONE)).isFalse()
	}

	@Test
	fun hasSerializableAnnotationWithRecursiveGeneric() {
		Assertions.assertThat(KotlinDetector.hasSerializableAnnotation(
			ResolvableType.forClass(RecursiveGeneric::class.java))).isFalse()
	}

	// Real life example: Guava's `ImmutableEnumSet`.
	class RecursiveGeneric<T : RecursiveGeneric<T>>

	@JvmInline
	value class ValueClass(val value: String)

	data class WithoutSerializable(val value: String)

	@Serializable
	data class WithSerializable(val value: String)

}
