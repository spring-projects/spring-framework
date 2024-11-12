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

package org.springframework.expression.spel.ast;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AstUtils}.
 *
 * @author Sam Brannen
 * @since 6.1.15
 */
class AstUtilsTests {

	private final PropertyAccessor animal1Accessor = createAccessor("Animal1", Animal.class);

	private final PropertyAccessor animal2Accessor = createAccessor("Animal2", Animal.class);

	private final PropertyAccessor cat1Accessor = createAccessor("Cat1", Cat.class);

	private final PropertyAccessor cat2Accessor = createAccessor("Cat2", Cat.class);

	private final PropertyAccessor generic1Accessor = createAccessor("Generic1", null);

	private final PropertyAccessor generic2Accessor = createAccessor("Generic2", null);

	private final List<PropertyAccessor> accessors = List.of(
			generic1Accessor,
			cat1Accessor,
			animal1Accessor,
			animal2Accessor,
			cat2Accessor,
			generic2Accessor
		);


	@Test
	void emptyAccessorsList() {
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(new Cat(), List.of());
		assertThat(accessorsToTry).isEmpty();
	}

	@Test
	void noMatch() {
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(new Dog(), List.of(cat1Accessor));
		assertThat(accessorsToTry).isEmpty();
	}

	@Test
	void singleExactTypeMatch() {
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(new Cat(), List.of(cat1Accessor));
		assertThat(accessorsToTry).containsExactly(cat1Accessor);
	}

	@Test
	void exactTypeMatches() {
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(new Cat(), accessors);
		// We would actually expect the following.
		// assertThat(accessorsToTry).containsExactly(
		// 		cat1Accessor, cat2Accessor, animal1Accessor, animal2Accessor, generic1Accessor, generic2Accessor);
		// However, prior to Spring Framework 6.2, the supertype and generic accessors are not
		// ordered properly. So we test that the exact matches come first and in the expected order.
		assertThat(accessorsToTry)
				.hasSize(accessors.size())
				.startsWith(cat1Accessor, cat2Accessor);
	}

	@Disabled("PropertyAccessor ordering for supertype and generic matches is broken prior to Spring Framework 6.2")
	@Test
	void supertypeMatches() {
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry(new Dog(), accessors);
		assertThat(accessorsToTry).containsExactly(
				animal1Accessor, animal2Accessor, generic1Accessor, generic2Accessor);
	}

	@Test
	void genericMatches() {
		List<PropertyAccessor> accessorsToTry = getPropertyAccessorsToTry("not an Animal", accessors);
		assertThat(accessorsToTry).containsExactly(generic1Accessor, generic2Accessor);
	}


	private static PropertyAccessor createAccessor(String name, Class<?> type) {
		return new DemoAccessor(name, type);
	}

	private static List<PropertyAccessor> getPropertyAccessorsToTry(Object target, List<PropertyAccessor> propertyAccessors) {
		return AstUtils.getPropertyAccessorsToTry(target.getClass(), propertyAccessors);
	}


	private static class DemoAccessor implements PropertyAccessor {

		private final String name;
		private final Class<?>[] types;

		DemoAccessor(String name, Class<?> type) {
			this.name = name;
			this.types = (type != null ? new Class<?>[] {type} : null);
		}

		@Override
		@Nullable
		public Class<?>[] getSpecificTargetClasses() {
			return this.types;
		}

		@Override
		public String toString() {
			return this.name;
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) {
			return true;
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) {
			throw new UnsupportedOperationException("Auto-generated method stub");
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			return false;
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
			/* no-op */
		}
	}

	sealed interface Animal permits Bat, Cat, Dog {
	}

	static final class Bat implements Animal {
	}

	static final class Cat implements Animal {
	}

	static final class Dog implements Animal {
	}

}
