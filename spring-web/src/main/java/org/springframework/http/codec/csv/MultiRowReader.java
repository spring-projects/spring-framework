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

package org.springframework.http.codec.csv;

import java.io.Reader;
import java.util.LinkedList;

import org.springframework.util.Assert;

/**
 * A reader that reads multiple row at a time. Not thread-safe.
 *
 * @author Markus Heiden
 */
final class MultiRowReader extends Reader {

	/**
	 * The row.
	 */
	private final LinkedList<String> rows = new LinkedList<>();

	/**
	 * The row offset to read from.
	 */
	private int rowOffset = Integer.MIN_VALUE;

	/**
	 * The remaining chars in the row.
	 */
	private int rowRemaining = Integer.MIN_VALUE;

	/**
	 * Has this reader already been closed?.
	 */
	private boolean closed = false;

	/**
	 * Number of rows.
	 */
	public int size() {
		return this.rows.size();
	}

	/**
	 * Add a new row to read.
	 */
	void addRow(String row) {
		Assert.notNull(row, "row must not be null");
		Assert.isTrue(!this.closed, "reader must not be closed");

		if (row.isEmpty()) {
			// Ignore empty (last) line that cause trouble because read() may not return 0.
			return;
		}

		if (this.rows.isEmpty()) {
			this.rowOffset = 0;
			this.rowRemaining = row.length();
		}
		this.rows.addLast(row);
	}

	/**
	 * Read the (one and only) row.
	 * @param destination destination buffer.
	 * @param offset      offset at which to start storing characters.
	 * @param length      maximum number of characters to read.
	 * @return number of characters read.
	 * @throws IllegalArgumentException if this reader runs out of rows but has not been closed yet.
	 */
	@Override
	public int read(char[] destination, int offset, int length) {
		Assert.isTrue(length > 0, "length must be positive");

		if (this.rows.isEmpty()) {
			Assert.isTrue(this.closed, "reader must be closed. " +
					"Increment the lookahead if the reader runs out of rows.");
			return -1;
		}

		var count = Math.min(this.rowRemaining, length);
		this.rows.get(0).getChars(this.rowOffset, this.rowOffset + count, destination, offset);
		this.rowOffset += count;
		this.rowRemaining -= count;
		nextRow();
		return count;
	}

	/**
	 * Advance to the next row if the current one has been read fully.
	 */
	private void nextRow() {
		if (this.rowRemaining > 0) {
			return;
		}

		this.rows.removeFirst();
		if (this.rows.isEmpty()) {
			Assert.isTrue(this.closed, "reader must be closed. " +
					"Increment the lookahead if the reader runs out of rows.");
			return;
		}

		this.rowOffset = 0;
		this.rowRemaining = this.rows.getFirst().length();
	}

	/**
	 * Close this reader.
	 */
	@Override
	public void close() {
		this.closed = true;
	}

}
