/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel.ast;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.expression.TargetedAccessor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AccessorUtils}.
 *
 * @author Sam Brannen
 * @since 6.1.15
 */
class AccessorUtilsTests {

	private final TargetedAccessor animal1Accessor = createAccessor("Animal1", Animal.class);

	private final TargetedAccessor animal2Accessor = createAccessor("Animal2", Animal.class);

	private final TargetedAccessor cat1Accessor = createAccessor("Cat1", Cat.class);

	private final TargetedAccessor cat2Accessor = createAccessor("Cat2", Cat.class);

	private final TargetedAccessor generic1Accessor = createAccessor("Generic1", null);

	private final TargetedAccessor generic2Accessor = createAccessor("Generic2", null);

	private final List<TargetedAccessor> accessors = List.of(
			generic1Accessor,
			cat1Accessor,
			animal1Accessor,
			animal2Accessor,
			cat2Accessor,
			generic2Accessor
		);


	@Test
	void emptyAccessorsList() {
		List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Cat(), List.of());
		assertThat(accessorsToTry).isEmpty();
	}

	@Test
	void noMatch() {
		List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Dog(), List.of(cat1Accessor));
		assertThat(accessorsToTry).isEmpty();
	}

	@Test
	void singleExactTypeMatch() {
		List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Cat(), List.of(cat1Accessor));
		assertThat(accessorsToTry).containsExactly(cat1Accessor);
	}

	@Test
	void exactTypeSupertypeAndGenericMatches() {
		List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Cat(), accessors);
		assertThat(accessorsToTry).containsExactly(
				cat1Accessor, cat2Accessor, animal1Accessor, animal2Accessor, generic1Accessor, generic2Accessor);
	}

	@Test
	void supertypeAndGenericMatches() {
		List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry(new Dog(), accessors);
		assertThat(accessorsToTry).containsExactly(
				animal1Accessor, animal2Accessor, generic1Accessor, generic2Accessor);
	}

	@Test
	void genericMatches() {
		List<TargetedAccessor> accessorsToTry = AccessorUtils.getAccessorsToTry("not an Animal", accessors);
		assertThat(accessorsToTry).containsExactly(generic1Accessor, generic2Accessor);
	}


	private static TargetedAccessor createAccessor(String name, Class<?> type) {
		return new DemoAccessor(name, (type != null ? new Class<?>[] { type } : null));
	}


	private record DemoAccessor(String name, Class<?>[] types) implements TargetedAccessor {

		@Override
		public Class<?> @Nullable [] getSpecificTargetClasses() {
			return this.types;
		}

		@Override
		public final String toString() {
			return this.name;
		}
	}

	sealed interface Animal permits Cat, Dog {
	}

	static final class Cat implements Animal {
	}

	static final class Dog implements Animal {
	}

}
