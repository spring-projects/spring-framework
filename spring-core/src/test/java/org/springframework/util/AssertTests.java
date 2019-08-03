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

import org.junit.Test;

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
public class AssertTests {


	@Test
	public void stateWithMessage() {
		Assert.state(true, "enigma");
	}

	@Test
	public void stateWithFalseExpressionAndMessage() {
		assertThatIllegalStateException().isThrownBy(() ->
		Assert.state(false, "enigma")).withMessageContaining("enigma");
	}

	@Test
	public void stateWithMessageSupplier() {
		Assert.state(true, () -> "enigma");
	}

	@Test
	public void stateWithFalseExpressionAndMessageSupplier() {
		assertThatIllegalStateException().isThrownBy(() ->
				Assert.state(false, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void stateWithFalseExpressionAndNullMessageSupplier() {
		assertThatIllegalStateException().isThrownBy(() ->
				Assert.state(false, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void isTrueWithMessage() {
		Assert.isTrue(true, "enigma");
	}

	@Test
	public void isTrueWithFalse() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isTrue(false, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void isTrueWithMessageSupplier() {
		Assert.isTrue(true, () -> "enigma");
	}

	@Test
	public void isTrueWithFalseAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isTrue(false, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void isTrueWithFalseAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isTrue(false, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void isNullWithMessage() {
		Assert.isNull(null, "Bla");
	}

	@Test
	public void isNullWithMessageSupplier() {
		Assert.isNull(null, () -> "enigma");
	}

	@Test
	public void isNullWithNonNullObjectAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isNull("foo", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void isNullWithNonNullObjectAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isNull("foo", (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void notNullWithMessage() {
		Assert.notNull("foo", "enigma");
	}

	@Test
	public void notNullWithMessageSupplier() {
		Assert.notNull("foo", () -> "enigma");
	}

	@Test
	public void notNullWithNullAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notNull(null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notNullWithNullAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notNull(null, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void hasLength() {
		Assert.hasLength("I Heart ...", "enigma");
	}

	@Test
	public void hasLengthWithWhitespaceOnly() {
		Assert.hasLength("\t  ", "enigma");
	}

	@Test
	public void hasLengthWithEmptyString() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength("", "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasLengthWithNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength(null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasLengthWithMessageSupplier() {
		Assert.hasLength("foo", () -> "enigma");
	}

	@Test
	public void hasLengthWithWhitespaceOnlyAndMessageSupplier() {
		Assert.hasLength("\t", () -> "enigma");
	}

	@Test
	public void hasLengthWithEmptyStringAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength("", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasLengthWithNullAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength(null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasLengthWithNullAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasLength(null, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void hasText() {
		Assert.hasText("foo", "enigma");
	}

	@Test
	public void hasTextWithWhitespaceOnly() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText("\t ", "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasTextWithEmptyString() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText("", "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasTextWithNull() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText(null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasTextWithMessageSupplier() {
		Assert.hasText("foo", () -> "enigma");
	}

	@Test
	public void hasTextWithWhitespaceOnlyAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText("\t ", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasTextWithEmptyStringAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
		Assert.hasText("", () -> "enigma")).withMessageContaining("enigma");
	}

	@Test
	public void hasTextWithNullAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText(null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void hasTextWithNullAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.hasText(null, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void doesNotContainWithNullSearchString() {
		Assert.doesNotContain(null, "rod", "enigma");
	}

	@Test
	public void doesNotContainWithNullSubstring() {
		Assert.doesNotContain("A cool chick's name is Brod.", null, "enigma");
	}

	@Test
	public void doesNotContainWithEmptySubstring() {
		Assert.doesNotContain("A cool chick's name is Brod.", "", "enigma");
	}

	@Test
	public void doesNotContainWithNullSearchStringAndNullSubstring() {
		Assert.doesNotContain(null, null, "enigma");
	}

	@Test
	public void doesNotContainWithMessageSupplier() {
		Assert.doesNotContain("foo", "bar", () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullSearchStringAndMessageSupplier() {
		Assert.doesNotContain(null, "bar", () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullSubstringAndMessageSupplier() {
		Assert.doesNotContain("foo", null, () -> "enigma");
	}

	@Test
	public void doesNotContainWithNullSearchStringAndNullSubstringAndMessageSupplier() {
		Assert.doesNotContain(null, null, () -> "enigma");
	}

	@Test
	public void doesNotContainWithSubstringPresentInSearchStringAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.doesNotContain("1234", "23", () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void doesNotContainWithNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.doesNotContain("1234", "23", (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void notEmptyArray() {
		Assert.notEmpty(new String[] {"1234"}, "enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArray() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(new String[] {}, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyArrayWithNullArray() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Object[]) null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyArrayWithMessageSupplier() {
		Assert.notEmpty(new String[] {"1234"}, () -> "enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArrayAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(new String[] {}, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyArrayWithNullArrayAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Object[]) null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArrayAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(new String[] {}, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void noNullElements() {
		Assert.noNullElements(new String[] { "1234" }, "enigma");
	}

	@Test
	public void noNullElementsWithEmptyArray() {
		Assert.noNullElements(new String[] {}, "enigma");
	}

	@Test
	public void noNullElementsWithMessageSupplier() {
		Assert.noNullElements(new String[] { "1234" }, () -> "enigma");
	}

	@Test
	public void noNullElementsWithEmptyArrayAndMessageSupplier() {
		Assert.noNullElements(new String[] {}, () -> "enigma");
	}

	@Test
	public void noNullElementsWithNullArrayAndMessageSupplier() {
		Assert.noNullElements((Object[]) null, () -> "enigma");
	}

	@Test
	public void noNullElementsWithNullElementsAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.noNullElements(new String[] { "foo", null, "bar" }, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void noNullElementsWithNullElementsAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.noNullElements(new String[] { "foo", null, "bar" }, (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void notEmptyCollection() {
		Assert.notEmpty(singletonList("foo"), "enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollection() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyList(), "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyCollectionWithNullCollection() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Collection<?>) null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyCollectionWithMessageSupplier() {
		Assert.notEmpty(singletonList("foo"), () -> "enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollectionAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyList(), () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyCollectionWithNullCollectionAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Collection<?>) null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollectionAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyList(), (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void notEmptyMap() {
		Assert.notEmpty(singletonMap("foo", "bar"), "enigma");
	}

	@Test
	public void notEmptyMapWithNullMap() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Map<?, ?>) null, "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMap() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyMap(), "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyMapWithMessageSupplier() {
		Assert.notEmpty(singletonMap("foo", "bar"), () -> "enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMapAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyMap(), () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyMapWithNullMapAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty((Map<?, ?>) null, () -> "enigma"))
			.withMessageContaining("enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMapAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.notEmpty(emptyMap(), (Supplier<String>) null))
			.withMessage(null);
	}

	@Test
	public void isInstanceOf() {
		Assert.isInstanceOf(String.class, "foo", "enigma");
	}

	@Test
	public void isInstanceOfWithNullType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(null, "foo", "enigma"))
			.withMessageContaining("Type to check against must not be null");
	}

	@Test
	public void isInstanceOfWithNullInstance() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, null, "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndNullMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, (String) null))
			.withMessageContaining("Object of class [java.lang.Long] must be an instance of class java.lang.String");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, "Custom message"))
			.withMessageContaining("Custom message: java.lang.Long");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessageWithSeparator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, "Custom message:"))
			.withMessageContaining("Custom message: Object of class [java.lang.Long] must be an instance of class java.lang.String");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessageWithSpace() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, "Custom message for "))
			.withMessageContaining("Custom message for java.lang.Long");
	}

	@Test
	public void isInstanceOfWithMessageSupplier() {
		Assert.isInstanceOf(String.class, "foo", () -> "enigma");
	}

	@Test
	public void isInstanceOfWithNullTypeAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(null, "foo", () -> "enigma"))
			.withMessageContaining("Type to check against must not be null");
	}

	@Test
	public void isInstanceOfWithNullInstanceAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, null, () -> "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, (Supplier<String>) null))
			.withMessageContaining("Object of class [java.lang.Long] must be an instance of class java.lang.String");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isInstanceOf(String.class, 42L, () -> "enigma"))
			.withMessageContaining("enigma: java.lang.Long");
	}

	@Test
	public void isAssignable() {
		Assert.isAssignable(Number.class, Integer.class, "enigma");
	}

	@Test
	public void isAssignableWithNullSupertype() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(null, Integer.class, "enigma"))
			.withMessageContaining("Super type to check against must not be null");
	}

	@Test
	public void isAssignableWithNullSubtype() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(Integer.class, null, "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	public void isAssignableWithTypeMismatchAndNullMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, (String) null))
			.withMessageContaining("class java.lang.Integer is not assignable to class java.lang.String");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessage() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, "Custom message"))
			.withMessageContaining("Custom message: class java.lang.Integer");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessageWithSeparator() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, "Custom message:"))
			.withMessageContaining("Custom message: class java.lang.Integer is not assignable to class java.lang.String");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessageWithSpace() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, "Custom message for "))
			.withMessageContaining("Custom message for class java.lang.Integer");
	}

	@Test
	public void isAssignableWithMessageSupplier() {
		Assert.isAssignable(Number.class, Integer.class, () -> "enigma");
	}

	@Test
	public void isAssignableWithNullSupertypeAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(null, Integer.class, () -> "enigma"))
			.withMessageContaining("Super type to check against must not be null");
	}

	@Test
	public void isAssignableWithNullSubtypeAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(Integer.class, null, () -> "enigma"))
			.withMessageContaining("enigma: null");
	}

	@Test
	public void isAssignableWithTypeMismatchAndNullMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, (Supplier<String>) null))
			.withMessageContaining("class java.lang.Integer is not assignable to class java.lang.String");
	}

	@Test
	public void isAssignableWithTypeMismatchAndMessageSupplier() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				Assert.isAssignable(String.class, Integer.class, () -> "enigma"))
			.withMessageContaining("enigma: class java.lang.Integer");
	}

	@Test
	public void state() {
		Assert.state(true, "enigma");
	}

	@Test
	public void stateWithFalseExpression() {
		assertThatIllegalStateException().isThrownBy(() ->
				Assert.state(false, "enigma"))
			.withMessageContaining("enigma");
	}

}
