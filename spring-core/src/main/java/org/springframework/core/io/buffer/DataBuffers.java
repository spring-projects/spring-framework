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

package org.springframework.core.io.buffer;

import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Utility methods for working with {@link DataBuffer DataBuffers} that do not
 * require Reactor or Reactive Streams on the classpath, and can therefore be
 * used in any application.
 *
 * <p>{@link DataBufferUtils} extends this class and adds the methods that
 * are based on Reactive Streams. This class must not depend on it.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 7.1
 */
public abstract class DataBuffers {

	// Same category as DataBufferUtils, without loading that class (and Reactive Streams)
	private static final Log logger = LogFactory.getLog("org.springframework.core.io.buffer.DataBufferUtils");

	private static final Consumer<DataBuffer> RELEASE_CONSUMER = DataBuffers::release;


	/**
	 * Retain the given data buffer, if it is a {@link PooledDataBuffer}.
	 * @param dataBuffer the data buffer to retain
	 * @return the retained buffer
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T retain(T dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
			return (T) pooledDataBuffer.retain();
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * Release the given data buffer. If it is a {@link PooledDataBuffer} and
	 * has been {@linkplain PooledDataBuffer#isAllocated() allocated}, this
	 * method will call {@link PooledDataBuffer#release()}. If it is a
	 * {@link CloseableDataBuffer}, this method will call
	 * {@link CloseableDataBuffer#close()}.
	 * @param dataBuffer the data buffer to release
	 * @return {@code true} if the buffer was released; {@code false} otherwise.
	 */
	public static boolean release(@Nullable DataBuffer dataBuffer) {
		if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer) {
			if (pooledDataBuffer.isAllocated()) {
				try {
					return pooledDataBuffer.release();
				}
				catch (IllegalStateException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to release PooledDataBuffer: " + dataBuffer, ex);
					}
					return false;
				}
			}
		}
		else if (dataBuffer instanceof CloseableDataBuffer closeableDataBuffer) {
			try {
				closeableDataBuffer.close();
				return true;
			}
			catch (IllegalStateException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to release CloseableDataBuffer " + dataBuffer, ex);
				}
				return false;
			}
		}
		return false;
	}

	/**
	 * Associate the given hint with the data buffer if it is a pooled buffer
	 * and supports leak tracking.
	 * @param dataBuffer the data buffer to attach the hint to
	 * @param hint the hint to attach
	 * @return the input buffer
	 * @since 5.3.2
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DataBuffer> T touch(T dataBuffer, Object hint) {
		if (dataBuffer instanceof TouchableDataBuffer touchableDataBuffer) {
			return (T) touchableDataBuffer.touch(hint);
		}
		else {
			return dataBuffer;
		}
	}

	/**
	 * Return a consumer that calls {@link #release(DataBuffer)} on all
	 * passed data buffers.
	 */
	public static Consumer<DataBuffer> releaseConsumer() {
		return RELEASE_CONSUMER;
	}

	/**
	 * Return a {@link DataBufferMatcher} for the given delimiter.
	 * The matcher can be used to find the delimiters in a stream of data buffers.
	 * @param delimiter the delimiter bytes to find
	 * @return the matcher
	 * @since 5.2
	 */
	public static DataBufferMatcher matcher(byte[] delimiter) {
		return matcher(new byte[][] {delimiter});
	}

	/**
	 * Return a {@link DataBufferMatcher} for the given delimiters.
	 * The matcher can be used to find the delimiters in a stream of data buffers.
	 * @param delimiters the delimiters bytes to find
	 * @return the matcher
	 * @since 5.2
	 */
	public static DataBufferMatcher matcher(byte[]... delimiters) {
		Assert.isTrue(delimiters.length > 0, "Delimiters must not be empty");
		return (delimiters.length == 1 ? createMatcher(delimiters[0]) : new CompositeMatcher(delimiters));
	}

	private static NestedMatcher createMatcher(byte[] delimiter) {
		// extract length due to Eclipse IDE compiler error in switch expression
		int length = delimiter.length;
		Assert.isTrue(length > 0, "Delimiter must not be empty");
		return switch (length) {
			case 1 -> (delimiter[0] == 10 ? SingleByteMatcher.NEWLINE_MATCHER : new SingleByteMatcher(delimiter));
			case 2 -> new TwoByteMatcher(delimiter);
			default -> new KnuthMorrisPrattMatcher(delimiter);
		};
	}


	/**
	 * Matcher that supports searching for multiple delimiters.
	 */
	private static class CompositeMatcher implements DataBufferMatcher {

		private static final byte[] NO_DELIMITER = new byte[0];


		private final NestedMatcher[] matchers;

		byte[] longestDelimiter = NO_DELIMITER;

		CompositeMatcher(byte[][] delimiters) {
			this.matchers = initMatchers(delimiters);
		}

		private static NestedMatcher[] initMatchers(byte[][] delimiters) {
			NestedMatcher[] matchers = new NestedMatcher[delimiters.length];
			for (int i = 0; i < delimiters.length; i++) {
				matchers[i] = createMatcher(delimiters[i]);
			}
			return matchers;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			this.longestDelimiter = NO_DELIMITER;

			for (int pos = dataBuffer.readPosition(); pos < dataBuffer.writePosition(); pos++) {
				byte b = dataBuffer.getByte(pos);

				for (NestedMatcher matcher : this.matchers) {
					if (matcher.match(b) && matcher.delimiter().length > this.longestDelimiter.length) {
						this.longestDelimiter = matcher.delimiter();
					}
				}

				if (this.longestDelimiter != NO_DELIMITER) {
					reset();
					return pos;
				}
			}
			return -1;
		}

		@Override
		public byte[] delimiter() {
			Assert.state(this.longestDelimiter != NO_DELIMITER, "'delimiter' not set");
			return this.longestDelimiter;
		}

		@Override
		public void reset() {
			for (NestedMatcher matcher : this.matchers) {
				matcher.reset();
			}
		}
	}


	/**
	 * Matcher that can be nested within {@link CompositeMatcher} where multiple
	 * matchers advance together using the same index, one byte at a time.
	 */
	private interface NestedMatcher extends DataBufferMatcher {

		/**
		 * Perform a match against the next byte of the stream and return true
		 * if the delimiter is fully matched.
		 */
		boolean match(byte b);

	}


	/**
	 * Matcher for a single byte delimiter.
	 */
	private static class SingleByteMatcher implements NestedMatcher {

		static final SingleByteMatcher NEWLINE_MATCHER = new SingleByteMatcher(new byte[] {10});

		private final byte[] delimiter;

		SingleByteMatcher(byte[] delimiter) {
			Assert.isTrue(delimiter.length == 1, "Expected a 1 byte delimiter");
			this.delimiter = delimiter;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			int start = dataBuffer.readPosition();
			int end = dataBuffer.writePosition();
			return dataBuffer.forEachByte(start, end - start, b -> !this.match(b));
		}

		@Override
		public boolean match(byte b) {
			return this.delimiter[0] == b;
		}

		@Override
		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public void reset() {
		}
	}


	/**
	 * Base class for a {@link NestedMatcher}.
	 */
	private abstract static class AbstractNestedMatcher implements NestedMatcher {

		private final byte[] delimiter;

		private int matches = 0;


		protected AbstractNestedMatcher(byte[] delimiter) {
			this.delimiter = delimiter;
		}

		protected void setMatches(int index) {
			this.matches = index;
		}

		protected int getMatches() {
			return this.matches;
		}

		@Override
		public int match(DataBuffer dataBuffer) {
			int start = dataBuffer.readPosition();
			int end = dataBuffer.writePosition();
			int matchPosition = dataBuffer.forEachByte(start, end - start, b -> !this.match(b));
			if (matchPosition != -1) {
				reset();
			}
			return matchPosition;
		}

		@Override
		public boolean match(byte b) {
			if (b == this.delimiter[this.matches]) {
				this.matches++;
				return (this.matches == delimiter().length);
			}
			return false;
		}

		@Override
		public byte[] delimiter() {
			return this.delimiter;
		}

		@Override
		public void reset() {
			this.matches = 0;
		}
	}


	/**
	 * Matcher with a 2 byte delimiter that does not benefit from a
	 * Knuth-Morris-Pratt suffix-prefix table.
	 */
	private static class TwoByteMatcher extends AbstractNestedMatcher {

		protected TwoByteMatcher(byte[] delimiter) {
			super(delimiter);
			Assert.isTrue(delimiter.length == 2, "Expected a 2-byte delimiter");
		}

		@Override
		public boolean match(byte b) {
			if (getMatches() > 0 && b != delimiter()[getMatches()]) {
				setMatches(0);
			}
			return super.match(b);
		}
	}


	/**
	 * Implementation of {@link DataBufferMatcher} that uses the Knuth-Morris-Pratt algorithm.
	 * @see <a href="https://www.nayuki.io/page/knuth-morris-pratt-string-matching">Knuth-Morris-Pratt string matching</a>
	 */
	private static class KnuthMorrisPrattMatcher extends AbstractNestedMatcher {

		private final int[] table;

		public KnuthMorrisPrattMatcher(byte[] delimiter) {
			super(delimiter);
			this.table = longestSuffixPrefixTable(delimiter);
		}

		private static int[] longestSuffixPrefixTable(byte[] delimiter) {
			int[] result = new int[delimiter.length];
			result[0] = 0;
			for (int i = 1; i < delimiter.length; i++) {
				int j = result[i - 1];
				while (j > 0 && delimiter[i] != delimiter[j]) {
					j = result[j - 1];
				}
				if (delimiter[i] == delimiter[j]) {
					j++;
				}
				result[i] = j;
			}
			return result;
		}

		@Override
		public boolean match(byte b) {
			while (getMatches() > 0 && b != delimiter()[getMatches()]) {
				setMatches(this.table[getMatches() - 1]);
			}
			return super.match(b);
		}
	}

}
