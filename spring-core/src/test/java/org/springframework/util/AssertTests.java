/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for the {@link Assert} class.
 *
 * @author Keith Donald
 * @author Erwin Vervaet
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Juergen Hoeller
 */
class AssertTests {

	@Test
	void stateWithMessage() {
		Assert.state(true, "enigma");
	}

	@Test
	void stateWithFalseExpressionAndMessage() {
		assertThatIllegalStateException().isThrownBy(() ->
		Assert.state(false, "enigma")).withMessageContaining("enigma");
	}

	@Test
	void stateWithMessageSupplier() {
		Assert.state(true, () -> "enigma");
	}

	@Test
	void stateWithFalseExpressionAndMessageSupplier() {
		assertThatIllegalStateException().isThrownBy(() ->
				Assert.state(false, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void stateWithFalseExpressionAndNullMessageSupplier() {
		assertThatIllegalStateException().isThrownBy(() ->
				Assert.state(false, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void isTrueWithMessage() {
		Assert.isTrue(true, "enigma");
	}

	@Test
	void isTrueWithFalse() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isTrue(false, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void isTrueWithMessageSupplier() {
		Assert.isTrue(true, () -> "enigma");
	}

	@Test
	void isTrueWithFalseAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isTrue(false, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void isTrueWithFalseAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isTrue(false, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void isNullWithMessage() {
		Assert.isNull(null, "Bla");
	}

	@Test
	void isNullWithMessageSupplier() {
		Assert.isNull(null, () -> "enigma");
	}

	@Test
	void isNullWithNonNullObjectAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isNull("foo", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void isNullWithNonNullObjectAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isNull("foo", (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void notNullWithMessage() {
		Assert.notNull("foo", "enigma");
	}

	@Test
	void notNullWithMessageSupplier() {
		Assert.notNull("foo", () -> "enigma");
	}

	@Test
	void notNullWithNullAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notNull(null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notNullWithNullAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notNull(null, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void hasLength() {
		Assert.hasLength("I Heart ...", "enigma");
	}

	@Test
	void hasLengthWithWhitespaceOnly() {
		Assert.hasLength("\t  ", "enigma");
	}

	@Test
	void hasLengthWithEmptyString() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength("", "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasLengthWithNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength(null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasLengthWithMessageSupplier() {
		Assert.hasLength("foo", () -> "enigma");
	}

	@Test
	void hasLengthWithWhitespaceOnlyAndMessageSupplier() {
		Assert.hasLength("\t", () -> "enigma");
	}

	@Test
	void hasLengthWithEmptyStringAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength("", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasLengthWithNullAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength(null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasLengthWithNullAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength(null, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void hasText() {
		Assert.hasText("foo", "enigma");
	}

	@Test
	void hasTextWithWhitespaceOnly() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText("\t ", "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasTextWithEmptyString() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText("", "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasTextWithNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText(null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasTextWithMessageSupplier() {
		Assert.hasText("foo", () -> "enigma");
	}

	@Test
	void hasTextWithWhitespaceOnlyAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText("\t ", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasTextWithEmptyStringAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
		Assert.hasText("", () -> "enigma")).withMessageContaining("enigma");
	}

	@Test
	void hasTextWithNullAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText(null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void hasTextWithNullAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText(null, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void doesNotContainWithNullSearchString() {
		Assert.doesNotContain(null, "rod", "enigma");
	}

	@Test
	void doesNotContainWithNullSubstring() {
		Assert.doesNotContain("A cool chick's name is Brod.", null, "enigma");
	}

	@Test
	void doesNotContainWithEmptySubstring() {
		Assert.doesNotContain("A cool chick's name is Brod.", "", "enigma");
	}

	@Test
	void doesNotContainWithNullSearchStringAndNullSubstring() {
		Assert.doesNotContain(null, null, "enigma");
	}

	@Test
	void doesNotContainWithMessageSupplier() {
		Assert.doesNotContain("foo", "bar", () -> "enigma");
	}

	@Test
	void doesNotContainWithNullSearchStringAndMessageSupplier() {
		Assert.doesNotContain(null, "bar", () -> "enigma");
	}

	@Test
	void doesNotContainWithNullSubstringAndMessageSupplier() {
		Assert.doesNotContain("foo", null, () -> "enigma");
	}

	@Test
	void doesNotContainWithNullSearchStringAndNullSubstringAndMessageSupplier() {
		Assert.doesNotContain(null, null, () -> "enigma");
	}

	@Test
	void doesNotContainWithSubstringPresentInSearchStringAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.doesNotContain("1234", "23", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void doesNotContainWithNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.doesNotContain("1234", "23", (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void notEmptyArray() {
		Assert.notEmpty(new String[] {"1234"}, "enigma");
	}

	@Test
	void notEmptyArrayWithEmptyArray() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(new String[] {}, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyArrayWithNullArray() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Object[]) null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyArrayWithMessageSupplier() {
		Assert.notEmpty(new String[] {"1234"}, () -> "enigma");
	}

	@Test
	void notEmptyArrayWithEmptyArrayAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(new String[] {}, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyArrayWithNullArrayAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Object[]) null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyArrayWithEmptyArrayAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(new String[] {}, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void noNullElements() {
		Assert.noNullElements(new String[] { "1234" }, "enigma");
	}

	@Test
	void noNullElementsWithEmptyArray() {
		Assert.noNullElements(new String[] {}, "enigma");
	}

	@Test
	void noNullElementsWithMessageSupplier() {
		Assert.noNullElements(new String[] { "1234" }, () -> "enigma");
	}

	@Test
	void noNullElementsWithEmptyArrayAndMessageSupplier() {
		Assert.noNullElements(new String[] {}, () -> "enigma");
	}

	@Test
	void noNullElementsWithNullArrayAndMessageSupplier() {
		Assert.noNullElements((Object[]) null, () -> "enigma");
	}

	@Test
	void noNullElementsWithNullElementsAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.noNullElements(new String[] { "foo", null, "bar" }, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void noNullElementsWithNullElementsAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.noNullElements(new String[] { "foo", null, "bar" }, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void notEmptyCollection() {
		Assert.notEmpty(singletonList("foo"), "enigma");
	}

	@Test
	void notEmptyCollectionWithEmptyCollection() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyList(), "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyCollectionWithNullCollection() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Collection<?>) null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyCollectionWithMessageSupplier() {
		Assert.notEmpty(singletonList("foo"), () -> "enigma");
	}

	@Test
	void notEmptyCollectionWithEmptyCollectionAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyList(), () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyCollectionWithNullCollectionAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Collection<?>) null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyCollectionWithEmptyCollectionAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyList(), (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void notEmptyMap() {
		Assert.notEmpty(singletonMap("foo", "bar"), "enigma");
	}

	@Test
	void notEmptyMapWithNullMap() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Map<?, ?>) null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyMapWithEmptyMap() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyMap(), "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyMapWithMessageSupplier() {
		Assert.notEmpty(singletonMap("foo", "bar"), () -> "enigma");
	}

	@Test
	void notEmptyMapWithEmptyMapAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyMap(), () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyMapWithNullMapAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Map<?, ?>) null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	void notEmptyMapWithEmptyMapAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyMap(), (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	void isInstanceOf() {
		Assert.isInstanceOf(String.class, "foo", "enigma");
	}

	@Test
	void isInstanceOfWithNullType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(null, "foo", "enigma"))
			.withMessageContaining("Type to check against must not be null");
	}

	@Test
	void isInstanceOfWithNullInstance() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, null, "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	void isInstanceOfWithTypeMismatchAndNullMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, (String) null))
			.withMessageContaining("Object of class [java.lang.Long] must be an instance of class java.lang.String");
	}

	@Test
	void isInstanceOfWithTypeMismatchAndCustomMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, "Custom message"))
			.withMessageContaining("Custom message: java.lang.Long");
	}

	@Test
	void isInstanceOfWithTypeMismatchAndCustomMessageWithSeparator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, "Custom message:"))
			.withMessageContaining("Custom message: Object of class [java.lang.Long] must be an instance of class java.lang.String");
	}

	@Test
	void isInstanceOfWithTypeMismatchAndCustomMessageWithSpace() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, "Custom message for "))
			.withMessageContaining("Custom message for java.lang.Long");
	}

	@Test
	void isInstanceOfWithMessageSupplier() {
		Assert.isInstanceOf(String.class, "foo", () -> "enigma");
	}

	@Test
	void isInstanceOfWithNullTypeAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(null, "foo", () -> "enigma"))
			.withMessageContaining("Type to check against must not be null");
	}

	@Test
	void isInstanceOfWithNullInstanceAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, null, () -> "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	void isInstanceOfWithTypeMismatchAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, (Supplier<String>) null))
			.withMessageContaining("Object of class [java.lang.Long] must be an instance of class java.lang.String");
	}

	@Test
	void isInstanceOfWithTypeMismatchAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, () -> "enigma"))
			.withMessageContaining("enigma: java.lang.Long");
	}

	@Test
	void isAssignable() {
		Assert.isAssignable(Number.class, Integer.class, "enigma");
	}

	@Test
	void isAssignableWithNullSupertype() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(null, Integer.class, "enigma"))
			.withMessageContaining("Super type to check against must not be null");
	}

	@Test
	void isAssignableWithNullSubtype() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(Integer.class, null, "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	void isAssignableWithTypeMismatchAndNullMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, (String) null))
			.withMessageContaining("class java.lang.Integer is not assignable to class java.lang.String");
	}

	@Test
	void isAssignableWithTypeMismatchAndCustomMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, "Custom message"))
			.withMessageContaining("Custom message: class java.lang.Integer");
	}

	@Test
	void isAssignableWithTypeMismatchAndCustomMessageWithSeparator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, "Custom message:"))
			.withMessageContaining("Custom message: class java.lang.Integer is not assignable to class java.lang.String");
	}

	@Test
	void isAssignableWithTypeMismatchAndCustomMessageWithSpace() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, "Custom message for "))
			.withMessageContaining("Custom message for class java.lang.Integer");
	}

	@Test
	void isAssignableWithMessageSupplier() {
		Assert.isAssignable(Number.class, Integer.class, () -> "enigma");
	}

	@Test
	void isAssignableWithNullSupertypeAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(null, Integer.class, () -> "enigma"))
			.withMessageContaining("Super type to check against must not be null");
	}

	@Test
	void isAssignableWithNullSubtypeAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(Integer.class, null, () -> "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	void isAssignableWithTypeMismatchAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, (Supplier<String>) null))
			.withMessageContaining("class java.lang.Integer is not assignable to class java.lang.String");
	}

	@Test
	void isAssignableWithTypeMismatchAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, () -> "enigma"))
			.withMessageContaining("enigma: class java.lang.Integer");
	}

	@Test
	void state() {
		Assert.state(true, "enigma");
	}

	@Test
	void stateWithFalseExpression() {
		assertThatIllegalStateException().isThrownBy(() ->
				Assert.state(false, "enigma"))
			.withMessageContaining("enigma");
	}

}
