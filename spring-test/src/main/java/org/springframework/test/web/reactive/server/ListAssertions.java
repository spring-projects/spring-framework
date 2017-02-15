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
package org.springframework.test.web.reactive.server;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

/**
 * Assertions on a List of values.
 *
 * @param <E> the type of element values in the collection
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ListAssertions<E> extends ObjectAssertions<List<E>, ListAssertions<E>> {


	protected ListAssertions(ExchangeActions actions, List<E> collection, String errorPrefix) {
		super(actions, collection, errorPrefix);
	}


	/**
	 * Assert the size of the list.
	 */
	public ListAssertions<E> hasSize(int size) {
		assertEquals(getErrorPrefix() + " count", size, getValue().size());
		return this;
	}

	/**
	 * Assert that the list contains all of the given values.
	 */
	@SuppressWarnings("unchecked")
	public ListAssertions<E> contains(E... entities) {
		Arrays.stream(entities).forEach(entity -> {
			boolean result = getValue().contains(entity);
			assertTrue(getErrorPrefix() + " do not contain " + entity, result);
		});
		return this;
	}

	/**
	 * Assert the list does not contain any of the given values.
	 */
	@SuppressWarnings("unchecked")
	public ListAssertions<E> doesNotContain(E... entities) {
		Arrays.stream(entities).forEach(entity -> {
			boolean result = !getValue().contains(entity);
			assertTrue(getErrorPrefix() + " should not contain " + entity, result);
		});
		return this;
	}

}
