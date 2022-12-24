package org.springframework.http.codec.csv;

import java.io.Reader;
import java.util.LinkedList;

import org.springframework.util.Assert;

import static java.lang.Math.min;

/**
 * A reader that reads multiple row at a time. NOT thread-safe.
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
		return rows.size();
	}

	/**
	 * Add a new row to read.
	 */
	void addRow(String row) {
		Assert.notNull(row, "row must not be null");
		Assert.isTrue(!closed, "reader must not be closed.");

		if (row.isEmpty()) {
			// Ignore empty (last) line that cause trouble because read() may not return 0.
			return;
		}

		if (rows.isEmpty()) {
			rowOffset = 0;
			rowRemaining = row.length();
		}
		this.rows.addLast(row);
	}

	/**
	 * Read the (one and only) row.
	 *
	 * @param destination
	 * 		Destination buffer.
	 * @param offset
	 * 		Offset at which to start storing characters.
	 * @param length
	 * 		Maximum number of characters to read.
	 * @return Number of characters read.
	 * @throws IllegalArgumentException
	 * 		If this reader runs out of rows but has not been closed yet.
	 */
	@Override
	public int read(char[] destination, int offset, int length) {
		Assert.isTrue(length > 0, "length must be positive");

		if (rows.isEmpty()) {
			Assert.isTrue(closed, "reader must be closed. " +
								  "Increment the lookahead if the reader runs out of rows.");
			return -1;
		}

		var count = min(rowRemaining, length);
		rows.get(0).getChars(rowOffset, rowOffset + count, destination, offset);
		rowOffset += count;
		rowRemaining -= count;
		nextRow();
		return count;
	}

	/**
	 * Advance to the next row if the current one has been read fully.
	 */
	private void nextRow() {
		if (rowRemaining > 0) {
			return;
		}

		rows.removeFirst();
		if (rows.isEmpty()) {
			Assert.isTrue(closed, "reader must be closed. " +
								  "Increment the lookahead if the reader runs out of rows.");
			return;
		}

		rowOffset = 0;
		rowRemaining = rows.get(0).length();
	}

	/**
	 * Close this reader.
	 */
	@Override
	public void close() {
		closed = true;
	}
}
