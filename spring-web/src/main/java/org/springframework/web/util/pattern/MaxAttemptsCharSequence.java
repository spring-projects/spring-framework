/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
