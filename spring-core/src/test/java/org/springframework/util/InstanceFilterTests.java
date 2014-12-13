/*
 * Copyright 2002-2014 the original author or authors.
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

import org.junit.Test;

import static java.util.Arrays.*;
import static org.junit.Assert.*;

/**
 * @author Stephane Nicoll
 */
public class InstanceFilterTests {

	@Test
	public void emptyFilterApplyMatchIfEmpty() {
		InstanceFilter<String> filter = new InstanceFilter<String>(null, null, true);
		match(filter, "foo");
		match(filter, "bar");
	}

	@Test
	public void includesFilter() {
		InstanceFilter<String> filter = new InstanceFilter<String>(
				asList("First", "Second"), null, true);
		match(filter, "Second");
		doNotMatch(filter, "foo");
	}

	@Test
	public void excludesFilter() {
		InstanceFilter<String> filter = new InstanceFilter<String>(
				null, asList("First", "Second"), true);
		doNotMatch(filter, "Second");
		match(filter, "foo");
	}

	@Test
	public void includesAndExcludesFilters() {
		InstanceFilter<String> filter = new InstanceFilter<String>(
				asList("foo", "Bar"), asList("First", "Second"), true);
		doNotMatch(filter, "Second");
		match(filter, "foo");
	}

	@Test
	public void includesAndExcludesFiltersConflict() {
		InstanceFilter<String> filter = new InstanceFilter<String>(
				asList("First"), asList("First"), true);
		doNotMatch(filter, "First");
	}

	private <T> void match(InstanceFilter<T> filter, T candidate) {
		assertTrue("filter '" + filter + "' should match " + candidate, filter.match(candidate));
	}

	private <T> void doNotMatch(InstanceFilter<T> filter, T candidate) {
		assertFalse("filter '" + filter + "' should not match " + candidate, filter.match(candidate));
	}

}
