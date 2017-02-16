/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.*;

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

	@Rule
	public final ExpectedException thrown = ExpectedException.none();


	@Test
	public void state() {
		Assert.state(true, "enigma");
	}

	@Test
	public void stateWithFalseExpression() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("enigma");
		Assert.state(false, "enigma");
	}

	@Test
	public void isTrueWithMessage() {
		Assert.isTrue(true, "enigma");
	}

	@Test
	public void isTrueWithFalse() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.isTrue(false, "enigma");
	}

	@Test
	public void isNullWithMessage() {
		Assert.isNull(null, "Bla");
	}

	@Test
	public void notNullWithMessage() {
		Assert.notNull("foo", "enigma");
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
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.hasLength("", "enigma");
	}

	@Test
	public void hasLengthWithNull() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.hasLength(null, "enigma");
	}

	@Test
	public void hasText() {
		Assert.hasText("foo", "enigma");
	}

	@Test
	public void hasTextWithWhitespaceOnly() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.hasText("\t ", "enigma");
	}

	@Test
	public void hasTextWithEmptyString() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.hasText("", "enigma");
	}

	@Test
	public void hasTextWithNull() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.hasText(null, "enigma");
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
	public void notEmptyArray() {
		Assert.notEmpty(new String[] {"1234"}, "enigma");
	}

	@Test
	public void notEmptyArrayWithEmptyArray() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.notEmpty(new String[] {}, "enigma");
	}

	@Test
	public void notEmptyArrayWithNullArray() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.notEmpty((Object[]) null, "enigma");
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
	public void notEmptyCollection() {
		Assert.notEmpty(singletonList("foo"), "enigma");
	}

	@Test
	public void notEmptyCollectionWithEmptyCollection() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.notEmpty(emptyList(), "enigma");
	}

	@Test
	public void notEmptyCollectionWithNullCollection() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.notEmpty((Collection<?>) null, "enigma");
	}

	@Test
	public void notEmptyMap() {
		Assert.notEmpty(singletonMap("foo", "bar"), "enigma");
	}

	@Test
	public void notEmptyMapWithNullMap() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.notEmpty((Map<?, ?>) null, "enigma");
	}

	@Test
	public void notEmptyMapWithEmptyMap() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma");
		Assert.notEmpty(emptyMap(), "enigma");
	}

	@Test
	public void isInstanceOf() {
		Assert.isInstanceOf(String.class, "foo", "enigma");
	}

	@Test
	public void isInstanceOfWithNullType() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Type to check against must not be null");
		Assert.isInstanceOf(null, "foo", "enigma");
	}

	@Test
	public void isInstanceOfWithNullInstance() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma: null");
		Assert.isInstanceOf(String.class, null, "enigma");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndNullMessage() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Object of class [java.lang.Long] must be an instance of class java.lang.String");
		Assert.isInstanceOf(String.class, 42L, (String) null);
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessage() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Custom message: java.lang.Long");
		Assert.isInstanceOf(String.class, 42L, "Custom message");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessageWithSeparator() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(
				"Custom message: Object of class [java.lang.Long] must be an instance of class java.lang.String");
		Assert.isInstanceOf(String.class, 42L, "Custom message:");
	}

	@Test
	public void isInstanceOfWithTypeMismatchAndCustomMessageWithSpace() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Custom message for java.lang.Long");
		Assert.isInstanceOf(String.class, 42L, "Custom message for ");
	}

	@Test
	public void isAssignable() {
		Assert.isAssignable(Number.class, Integer.class, "enigma");
	}

	@Test
	public void isAssignableWithNullSupertype() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Super type to check against must not be null");
		Assert.isAssignable(null, Integer.class, "enigma");
	}

	@Test
	public void isAssignableWithNullSubtype() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("enigma: null");
		Assert.isAssignable(Integer.class, null, "enigma");
	}

	@Test
	public void isAssignableWithTypeMismatchAndNullMessage() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("class java.lang.Integer is not assignable to class java.lang.String");
		Assert.isAssignable(String.class, Integer.class, (String) null);
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessage() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Custom message: class java.lang.Integer");
		Assert.isAssignable(String.class, Integer.class, "Custom message");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessageWithSeparator() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Custom message: class java.lang.Integer is not assignable to class java.lang.String");
		Assert.isAssignable(String.class, Integer.class, "Custom message:");
	}

	@Test
	public void isAssignableWithTypeMismatchAndCustomMessageWithSpace() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Custom message for class java.lang.Integer");
		Assert.isAssignable(String.class, Integer.class, "Custom message for ");
	}

}
