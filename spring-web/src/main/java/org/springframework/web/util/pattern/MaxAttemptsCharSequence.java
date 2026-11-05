/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.util.pattern;

/**
 * {@link CharSequence} decorator that limits the number of character access
 * attempts, guarding against excessive backtracking when matching a regular
 * expression against the given text.
 *
 * @author Brian Clozel
 * @see org.springframework.util.AntPathMatcher
 */
final class MaxAttemptsCharSequence implements CharSequence {

	private static final int MAX_ATTEMPTS = 1_000_000;


	private final String text;

	private final Counter counter;


	MaxAttemptsCharSequence(String text) {
		this(text, new Counter());
	}

	private MaxAttemptsCharSequence(String text, Counter counter) {
		this.text = text;
		this.counter = counter;
	}


	@Override
	public int length() {
		return this.text.length();
	}

	@Override
	public char charAt(int index) {
		if (this.counter.value++ >= MAX_ATTEMPTS) {
			throw new IllegalStateException(
					"Too many character access attempts encountered during pattern matching");
		}
		return this.text.charAt(index);
	}

	@Override
	public boolean isEmpty() {
		return this.text.isEmpty();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new MaxAttemptsCharSequence(this.text.substring(start, end), this.counter);
	}

	@Override
	public String toString() {
		return this.text;
	}


	private static class Counter {

		private int value;
	}

}
