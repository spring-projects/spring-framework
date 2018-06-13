/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.util.function.Predicate;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Denys Ivano
 */
public class StatusCodePredicatesTests {

	@Test
	public void is1xxInformational() {
		Predicate<Integer> predicate = StatusCodePredicates.is1xxInformational();

		assertTrue(predicate.test(100));
		assertTrue(predicate.test(101));
		assertTrue(predicate.test(199));
		assertFalse(predicate.test(200));
	}

	@Test
	public void is2xxSuccessful() {
		Predicate<Integer> predicate = StatusCodePredicates.is2xxSuccessful();

		assertTrue(predicate.test(200));
		assertTrue(predicate.test(204));
		assertTrue(predicate.test(299));
		assertFalse(predicate.test(300));
	}

	@Test
	public void is3xxRedirection() {
		Predicate<Integer> predicate = StatusCodePredicates.is3xxRedirection();

		assertTrue(predicate.test(300));
		assertTrue(predicate.test(302));
		assertTrue(predicate.test(399));
		assertFalse(predicate.test(400));
	}

	@Test
	public void is4xxClientError() {
		Predicate<Integer> predicate = StatusCodePredicates.is4xxClientError();

		assertTrue(predicate.test(400));
		assertTrue(predicate.test(404));
		assertTrue(predicate.test(499));
		assertFalse(predicate.test(500));
	}

	@Test
	public void is5xxServerError() {
		Predicate<Integer> predicate = StatusCodePredicates.is5xxServerError();

		assertTrue(predicate.test(500));
		assertTrue(predicate.test(502));
		assertTrue(predicate.test(599));
		assertFalse(predicate.test(600));
	}

	@Test
	public void isError() {
		Predicate<Integer> predicate = StatusCodePredicates.isError();

		assertTrue(predicate.test(400));
		assertTrue(predicate.test(404));
		assertTrue(predicate.test(502));
		assertFalse(predicate.test(200));
		assertFalse(predicate.test(999));
	}

	@Test
	public void between() {
		assertTrue(StatusCodePredicates.between(100, 200).test(100));
		assertTrue(StatusCodePredicates.between(100, 200).test(200));
		assertTrue(StatusCodePredicates.between(400, 499).test(404));
		assertFalse(StatusCodePredicates.between(400, 400).test(401));
		assertFalse(StatusCodePredicates.between(400, 499).test(500));
		try {
			StatusCodePredicates.between(400, 399);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			// do nothing
		}
	}

	@Test
	public void is() {
		assertTrue(StatusCodePredicates.is(400).test(400));
		assertFalse(StatusCodePredicates.is(400).test(500));
	}

	@Test
	public void not() {
		assertTrue(StatusCodePredicates.not(200).test(400));
		assertFalse(StatusCodePredicates.not(200).test(200));
	}

	@Test
	public void anyOf() {
		assertTrue(StatusCodePredicates.anyOf(400, 401, 403).test(401));
		assertTrue(StatusCodePredicates.anyOf(500).test(500));
		assertFalse(StatusCodePredicates.anyOf(new int[0]).test(500));
		assertFalse(StatusCodePredicates.anyOf(400, 500).test(404));
	}

	@Test
	public void noneOf() {
		assertTrue(StatusCodePredicates.noneOf(new int[0]).test(500));
		assertTrue(StatusCodePredicates.noneOf(200, 204).test(400));
		assertFalse(StatusCodePredicates.noneOf(500).test(500));
		assertFalse(StatusCodePredicates.noneOf(200, 204, 302).test(302));
	}

}
