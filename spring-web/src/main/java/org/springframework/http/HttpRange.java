/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Represents an HTTP (byte) range, as used in the {@code Range} header.
 *
 * @author Arjen Poutsma
 * @see <a href="http://tools.ietf.org/html/rfc7233">HTTP/1.1: Range Requests</a>
 * @see HttpHeaders#setRange(List)
 * @see HttpHeaders#getRange()
 * @since 4.2
 */
public abstract class HttpRange {

	private static final String BYTE_RANGE_PREFIX = "bytes=";


	/**
	 * Creates a {@code HttpRange} that ranges from the given position to the end of the
	 * representation.
	 * @param firstBytePos the first byte position
	 * @return a byte range that ranges from {@code firstBytePos} till the end
	 * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos) {
		return new ByteRange(firstBytePos, null);
	}

	/**
	 * Creates a {@code HttpRange} that ranges from the given fist position to the given
	 * last position.
	 * @param firstBytePos the first byte position
	 * @param lastBytePos the last byte position
	 * @return a byte range that ranges from {@code firstBytePos} till {@code lastBytePos}
	 * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
		Assert.isTrue(firstBytePos <= lastBytePos,
				"\"firstBytePost\" should be " + "less then or equal to \"lastBytePos\"");
		return new ByteRange(firstBytePos, lastBytePos);
	}

	/**
	 * Creates a {@code HttpRange} that ranges over the last given number of bytes.
	 * @param suffixLength the number of bytes
	 * @return a byte range that ranges over the last {@code suffixLength} number of bytes
	 * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createSuffixRange(long suffixLength) {
		return new SuffixByteRange(suffixLength);
	}


	/**
	 * Return the start of this range, given the total length of the representation.
	 * @param length the length of the representation.
	 * @return the start of this range
	 */
	public abstract long getRangeStart(long length);

	/**
	 * Return the end of this range (inclusive), given the total length of the
	 * representation.
	 * @param length the length of the representation.
	 * @return the end of this range
	 */
	public abstract long getRangeEnd(long length);


	/**
	 * Parse the given, comma-separated string into a list of {@code HttpRange} objects.
	 * <p>This method can be used to parse an {@code Range} header.
	 * @param ranges the string to parse
	 * @return the list of ranges
	 * @throws IllegalArgumentException if the string cannot be parsed
	 */
	public static List<HttpRange> parseRanges(String ranges) {
		if (!StringUtils.hasLength(ranges)) {
			return Collections.emptyList();
		}
		if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
			throw new IllegalArgumentException("Range \"" + ranges + "\" does not " +
					"start with \"" + BYTE_RANGE_PREFIX + "\"");
		}
		ranges = ranges.substring(BYTE_RANGE_PREFIX.length());

		String[] tokens = ranges.split(",\\s*");
		List<HttpRange> result = new ArrayList<HttpRange>(tokens.length);
		for (String token : tokens) {
			result.add(parseRange(token));
		}
		return result;
	}

	private static HttpRange parseRange(String range) {
		if (range == null) {
			return null;
		}
		int dashIdx = range.indexOf('-');
		if (dashIdx < 0) {
			throw new IllegalArgumentException("Range '\"" + range + "\" does not" +
					"contain \"-\"");
		}
		else if (dashIdx > 0) {
			// standard byte range, i.e. "bytes=0-500"
			long firstPos = Long.parseLong(range.substring(0, dashIdx));
			ByteRange byteRange;
			if (dashIdx < range.length() - 1) {
				long lastPos =
						Long.parseLong(range.substring(dashIdx + 1, range.length()));
				byteRange = new ByteRange(firstPos, lastPos);
			}
			else {
				byteRange = new ByteRange(firstPos, null);
			}
			if (!byteRange.validate()) {
				throw new IllegalArgumentException("Invalid Range \"" + range + "\"");
			}
			return byteRange;
		}
		else { // dashIdx == 0
			// suffix byte range, i.e. "bytes=-500"
			long suffixLength = Long.parseLong(range.substring(1));
			return new SuffixByteRange(suffixLength);
		}
	}

	/**
	 * Return a string representation of the given list of {@code HttpRange} objects.
	 * <p>This method can be used to for an {@code Range} header.
	 * @param ranges the ranges to create a string of
	 * @return the string representation
	 */
	public static String toString(Collection<HttpRange> ranges) {
		StringBuilder builder = new StringBuilder(BYTE_RANGE_PREFIX);
		for (Iterator<HttpRange> iterator = ranges.iterator(); iterator.hasNext(); ) {
			HttpRange range = iterator.next();
			range.appendTo(builder);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		appendTo(builder);
		return builder.toString();
	}

	abstract void appendTo(StringBuilder builder);

	/**
	 * Represents an HTTP/1.1 byte range, with a first and optional last position.
	 * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 * @see HttpRange#createByteRange(long)
	 * @see HttpRange#createByteRange(long, long)
	 */
	private static class ByteRange extends HttpRange {

		private final long firstPos;

		private final Long lastPos;

		private ByteRange(long firstPos, Long lastPos) {
			this.firstPos = firstPos;
			this.lastPos = lastPos;
		}

		@Override
		public long getRangeStart(long length) {
			return this.firstPos;
		}

		@Override
		public long getRangeEnd(long length) {
			if (this.lastPos != null && this.lastPos < length) {
				return this.lastPos;
			}
			else {
				return length - 1;

			}
		}

		@Override
		void appendTo(StringBuilder builder) {
			builder.append(this.firstPos);
			builder.append('-');
			if (this.lastPos != null) {
				builder.append(this.lastPos);
			}
		}

		boolean validate() {
			if (this.firstPos < 0) {
				return false;
			}
			if (this.lastPos == null) {
				return true;
			}
			else {
				return this.firstPos <= this.lastPos;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ByteRange)) {
				return false;
			}

			ByteRange other = (ByteRange) o;

			return this.firstPos == other.firstPos &&
					ObjectUtils.nullSafeEquals(this.lastPos, other.lastPos);
		}

		@Override
		public int hashCode() {
			int hashCode = ObjectUtils.nullSafeHashCode(this.firstPos);
			hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.lastPos);
			return hashCode;
		}

	}

	/**
	 * Represents an HTTP/1.1 suffix byte range, with a number of suffix bytes.
	 * @see <a href="http://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 * @see HttpRange#createSuffixRange(long)
	 */
	private static class SuffixByteRange extends HttpRange {

		private final long suffixLength;

		private SuffixByteRange(long suffixLength) {
			this.suffixLength = suffixLength;
		}

		@Override
		void appendTo(StringBuilder builder) {
			builder.append('-');
			builder.append(this.suffixLength);
		}

		@Override
		public long getRangeStart(long length) {
			if (this.suffixLength < length) {
				return length - this.suffixLength;
			}
			else {
				return 0;
			}
		}

		@Override
		public long getRangeEnd(long length) {
			return length - 1;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof SuffixByteRange)) {
				return false;
			}
			SuffixByteRange other = (SuffixByteRange) o;
			return this.suffixLength == other.suffixLength;
		}

		@Override
		public int hashCode() {
			return ObjectUtils.hashCode(this.suffixLength);
		}
	}
}
