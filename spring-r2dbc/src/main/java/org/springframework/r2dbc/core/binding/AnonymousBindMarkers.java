/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.r2dbc.core.binding;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Anonymous, index-based bind marker using a static placeholder.
 * Instances are bound by the ordinal position ordered by the appearance of
 * the placeholder. This implementation creates indexed bind markers using
 * an anonymous placeholder that correlates with an index.
 *
 * <p>Note: Anonymous bind markers are problematic because they have to appear
 * in generated SQL in the same order they get generated. This might cause
 * challenges in the future with complex generate statements. For example those
 * containing subselects which limit the freedom of arranging bind markers.
 *
 * @author Mark Paluch
 * @since 5.3
 */
class AnonymousBindMarkers implements BindMarkers {

	private static final AtomicIntegerFieldUpdater<AnonymousBindMarkers> COUNTER_INCREMENTER =
			AtomicIntegerFieldUpdater.newUpdater(AnonymousBindMarkers.class, "counter");


	private final String placeholder;

	// access via COUNTER_INCREMENTER
	@SuppressWarnings("unused")
	private volatile int counter;


	/**
	 * Create a new {@link AnonymousBindMarkers} instance given {@code placeholder}.
	 * @param placeholder parameter bind marker
	 */
	AnonymousBindMarkers(String placeholder) {
		this.placeholder = placeholder;
	}


	@Override
	public BindMarker next() {
		int index = COUNTER_INCREMENTER.getAndIncrement(this);
		return new IndexedBindMarkers.IndexedBindMarker(this.placeholder, index);
	}

}
