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

package org.springframework.core.annotation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.annotation.MergedAnnotation

/**
 * Tests for {@link MergedAnnotations} and {@link MergedAnnotation} in Kotlin.
 *
 * @author Sam Brannen
 * @since 5.3.16
 */
class KotlinMergedAnnotationsTests {

	@Test  // gh-28012
	fun recursiveAnnotation() {
		val method = javaClass.getMethod("personMethod")

		// MergedAnnotations
		val mergedAnnotations = MergedAnnotations.from(method)
		assertThat(mergedAnnotations.isPresent(Person::class.java)).isTrue();

		// MergedAnnotation
		val mergedAnnotation = MergedAnnotation.from(method.getAnnotation(Person::class.java))
		assertThat(mergedAnnotation).isNotNull();

		// Synthesized Annotations
		val jane = mergedAnnotation.synthesize()
		assertThat(jane).isInstanceOf(SynthesizedAnnotation::class.java)
		assertThat(jane.value).isEqualTo("jane")
		assertThat(jane.name).isEqualTo("jane")
		val synthesizedFriends = jane.friends
		assertThat(synthesizedFriends).hasSize(2)

		val john = synthesizedFriends[0]
		assertThat(john).isInstanceOf(SynthesizedAnnotation::class.java)
		assertThat(john.value).isEqualTo("john")
		assertThat(john.name).isEqualTo("john")

		val sally = synthesizedFriends[1]
		assertThat(sally).isInstanceOf(SynthesizedAnnotation::class.java)
		assertThat(sally.value).isEqualTo("sally")
		assertThat(sally.name).isEqualTo("sally")
	}

	@Test  // gh-28012
	fun recursiveNestedAnnotation() {
		val method = javaClass.getMethod("filterMethod")

		// MergedAnnotations
		val mergedAnnotations = MergedAnnotations.from(method)
		assertThat(mergedAnnotations.isPresent(Filter::class.java)).isTrue();

		// MergedAnnotation
		val mergedAnnotation = MergedAnnotation.from(method.getAnnotation(Filter::class.java))
		assertThat(mergedAnnotation).isNotNull();

		// Synthesized Annotations
		val fooFilter = mergedAnnotation.synthesize()
		assertThat(fooFilter).isInstanceOf(SynthesizedAnnotation::class.java)
		assertThat(fooFilter.value).isEqualTo("foo")
		assertThat(fooFilter.name).isEqualTo("foo")
		val filters = fooFilter.and
		assertThat(filters.value).hasSize(2)

		val barFilter = filters.value[0]
		assertThat(barFilter).isInstanceOf(SynthesizedAnnotation::class.java)
		assertThat(barFilter.value).isEqualTo("bar")
		assertThat(barFilter.name).isEqualTo("bar")
		assertThat(barFilter.and.value).isEmpty()

		val bazFilter = filters.value[1]
		assertThat(bazFilter).isInstanceOf(SynthesizedAnnotation::class.java)
		assertThat(bazFilter.value).isEqualTo("baz")
		assertThat(bazFilter.name).isEqualTo("baz")
		assertThat(bazFilter.and.value).isEmpty()
	}


	@Person("jane", friends = [Person("john"), Person("sally")])
	fun personMethod() {
	}

	@Filter("foo", and = Filters(Filter("bar"), Filter("baz")))
	fun filterMethod() {
	}

}
