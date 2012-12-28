/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Unit tests for the {@link Assert} class.
 *
 * @author Keith Donald
 * @author Erwin Vervaet
 * @author Rick Evans
 * @author Arjen Poutsma
 */
public class AssertTests {

	@Test(expected = IllegalArgumentException.class)
	public void instanceOf() {
		final Set<?> set = new HashSet<Object>();
		Assert.isInstanceOf(HashSet.class, set);
		Assert.isInstanceOf(HashMap.class, set);
	}

	@Test
	public void isNullDoesNotThrowExceptionIfArgumentIsNullWithMessage() {
		Assert.isNull(null, "Bla");
	}

	@Test
	public void isNullDoesNotThrowExceptionIfArgumentIsNull() {
		Assert.isNull(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void isNullThrowsExceptionIfArgumentIsNotNull() {
		Assert.isNull(new Object());
	}

	@Test(expected = IllegalArgumentException.class)
	public void isTrueWithFalseExpressionThrowsException() throws Exception {
		Assert.isTrue(false);
	}

	@Test
	public void isTrueWithTrueExpressionSunnyDay() throws Exception {
		Assert.isTrue(true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHasLengthWithNullStringThrowsException() throws Exception {
		Assert.hasLength(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void hasLengthWithEmptyStringThrowsException() throws Exception {
		Assert.hasLength("");
	}

	@Test
	public void hasLengthWithWhitespaceOnlyStringDoesNotThrowException() throws Exception {
		Assert.hasLength("\t  ");
	}

	@Test
	public void hasLengthSunnyDay() throws Exception {
		Assert.hasLength("I Heart ...");
	}

	@Test
	public void doesNotContainWithNullSearchStringDoesNotThrowException() throws Exception {
		Assert.doesNotContain(null, "rod");
	}

	@Test
	public void doesNotContainWithNullSubstringDoesNotThrowException() throws Exception {
		Assert.doesNotContain("A cool chick's name is Brod. ", null);
	}

	@Test
	public void doesNotContainWithEmptySubstringDoesNotThrowException() throws Exception {
		Assert.doesNotContain("A cool chick's name is Brod. ", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertNotEmptyWithNullCollectionThrowsException() throws Exception {
		Assert.notEmpty((Collection<?>) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertNotEmptyWithEmptyCollectionThrowsException() throws Exception {
		Assert.notEmpty(new ArrayList<Object>());
	}

	@Test
	public void assertNotEmptyWithCollectionSunnyDay() throws Exception {
		List<String> collection = new ArrayList<String>();
		collection.add("");
		Assert.notEmpty(collection);
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertNotEmptyWithNullMapThrowsException() throws Exception {
		Assert.notEmpty((Map<?, ?>) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertNotEmptyWithEmptyMapThrowsException() throws Exception {
		Assert.notEmpty(new HashMap<Object, Object>());
	}

	@Test
	public void assertNotEmptyWithMapSunnyDay() throws Exception {
		Map<String, String> map = new HashMap<String, String>();
		map.put("", "");
		Assert.notEmpty(map);
	}

	@Test(expected = IllegalArgumentException.class)
	public void isInstanceofClassWithNullInstanceThrowsException() throws Exception {
		Assert.isInstanceOf(String.class, null);
	}

	@Test(expected = IllegalStateException.class)
	public void stateWithFalseExpressionThrowsException() throws Exception {
		Assert.state(false);
	}

	@Test
	public void stateWithTrueExpressionSunnyDay() throws Exception {
		Assert.state(true);
	}

}
