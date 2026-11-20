/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests for {@link MergedAnnotations} and {@link MergedAnnotation} in Kotlin.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Lorenz Simon
 * @since 5.3.16
 */
class KotlinMergedAnnotationsTests {

	@Test  // gh-28012
	fun recursiveAnnotationWithAlias() {
		val method = javaClass.getMethod("personWithAliasMethod")

		// MergedAnnotations
		val mergedAnnotations = MergedAnnotations.from(method)
		assertThat(mergedAnnotations.isPresent(PersonWithAlias::class.java)).isTrue();

		// MergedAnnotation
		val mergedAnnotation = MergedAnnotation.from(method.getAnnotation(PersonWithAlias::class.java))
		assertThat(mergedAnnotation).isNotNull();

		// Synthesized Annotations
		val jane = mergedAnnotation.synthesize()
		assertThat(jane.value).isEqualTo("jane")
		assertThat(jane.name).isEqualTo("jane")
		val synthesizedFriends = jane.friends
		assertThat(synthesizedFriends).hasSize(2)

		val john = synthesizedFriends[0]
		assertThat(john.value).isEqualTo("john")
		assertThat(john.name).isEqualTo("john")

		val sally = synthesizedFriends[1]
		assertThat(sally.value).isEqualTo("sally")
		assertThat(sally.name).isEqualTo("sally")
	}

	@Test  // gh-31400
	fun recursiveAnnotationWithoutAlias() {
		val method = javaClass.getMethod("personWithoutAliasMethod")

		// MergedAnnotations
		val mergedAnnotations = MergedAnnotations.from(method)
		assertThat(mergedAnnotations.isPresent(PersonWithoutAlias::class.java)).isTrue();

		// MergedAnnotation
		val mergedAnnotation = MergedAnnotation.from(method.getAnnotation(PersonWithoutAlias::class.java))
		assertThat(mergedAnnotation).isNotNull();

		// Synthesized Annotations
		val jane = mergedAnnotation.synthesize()
		val synthesizedFriends = jane.friends
		assertThat(synthesizedFriends).hasSize(2)
	}

	@Test  // gh-28012
	fun recursiveNestedAnnotationWithAlias() {
		val method = javaClass.getMethod("filterWithAliasMethod")

		// MergedAnnotations
		val mergedAnnotations = MergedAnnotations.from(method)
		assertThat(mergedAnnotations.isPresent(FilterWithAlias::class.java)).isTrue();

		// MergedAnnotation
		val mergedAnnotation = MergedAnnotation.from(method.getAnnotation(FilterWithAlias::class.java))
		assertThat(mergedAnnotation).isNotNull();

		// Synthesized Annotations
		val fooFilter = mergedAnnotation.synthesize()
		assertThat(fooFilter.value).isEqualTo("foo")
		assertThat(fooFilter.name).isEqualTo("foo")
		val filters = fooFilter.and
		assertThat(filters.value).hasSize(2)

		val barFilter = filters.value[0]
		assertThat(barFilter.value).isEqualTo("bar")
		assertThat(barFilter.name).isEqualTo("bar")
		assertThat(barFilter.and.value).isEmpty()

		val bazFilter = filters.value[1]
		assertThat(bazFilter.value).isEqualTo("baz")
		assertThat(bazFilter.name).isEqualTo("baz")
		assertThat(bazFilter.and.value).isEmpty()
	}

	@PersonWithAlias("jane", friends = [PersonWithAlias("john"), PersonWithAlias("sally")])
	fun personWithAliasMethod() {
	}

	@PersonWithoutAlias("jane", friends = [PersonWithoutAlias("john"), PersonWithoutAlias("sally")])
	fun personWithoutAliasMethod() {
	}

	@FilterWithAlias("foo", and = FiltersWithoutAlias(FilterWithAlias("bar"), FilterWithAlias("baz")))
	fun filterWithAliasMethod() {
	}

}
