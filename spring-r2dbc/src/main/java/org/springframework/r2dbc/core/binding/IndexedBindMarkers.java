/*
 * Copyright 2002-2020 the original author or authors.
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
 * Index-based bind marker. This implementation creates indexed bind
 * markers using a numeric index and an optional prefix for bind markers
 * to be represented within the query string.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @since 5.3
 */
class IndexedBindMarkers implements BindMarkers {

	private static final AtomicIntegerFieldUpdater<IndexedBindMarkers> COUNTER_INCREMENTER =
			AtomicIntegerFieldUpdater.newUpdater(IndexedBindMarkers.class, "counter");


	private final int offset;

	private final String prefix;

	// access via COUNTER_INCREMENTER
	@SuppressWarnings("unused")
	private volatile int counter;


	/**
	 * Create a new {@link IndexedBindMarker} instance given {@code prefix} and {@code beginWith}.
	 * @param prefix bind parameter prefix
	 * @param beginWith the first index to use
	 */
	IndexedBindMarkers(String prefix, int beginWith) {
		this.counter = 0;
		this.prefix = prefix;
		this.offset = beginWith;
	}


	@Override
	public BindMarker next() {
		int index = COUNTER_INCREMENTER.getAndIncrement(this);
		return new IndexedBindMarker(this.prefix + "" + (index + this.offset), index);
	}


	/**
	 * A single indexed bind marker.
	 * @author Mark Paluch
	 */
	static class IndexedBindMarker implements BindMarker {

		private final String placeholder;

		private final int index;

		IndexedBindMarker(String placeholder, int index) {
			this.placeholder = placeholder;
			this.index = index;
		}

		@Override
		public String getPlaceholder() {
			return this.placeholder;
		}

		@Override
		public void bind(BindTarget target, Object value) {
			target.bind(this.index, value);
		}

		@Override
		public void bindNull(BindTarget target, Class<?> valueType) {
			target.bindNull(this.index, valueType);
		}

		public int getIndex() {
			return this.index;
		}
	}

}
