/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.BatchUpdateException;

/**
 * A {@link BatchUpdateException} that provides additional information about
 * batches that were successful prior to one failing.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
@SuppressWarnings("serial")
public class AggregatedBatchUpdateException extends BatchUpdateException {

	private final int[][] successfulUpdateCounts;

	private final BatchUpdateException originalException;

	/**
	 * Create an aggregated exception with the batches that have completed prior
	 * to the given {@code cause}.
	 * @param successfulUpdateCounts the counts of the batches that run successfully
	 * @param original the exception this instance aggregates
	 */
	public AggregatedBatchUpdateException(int[][] successfulUpdateCounts, BatchUpdateException original) {
		super(original.getMessage(), original.getSQLState(), original.getErrorCode(),
				original.getUpdateCounts(), original.getCause());
		this.successfulUpdateCounts = successfulUpdateCounts;
		this.originalException = original;
		// Copy state of the original exception
		setNextException(original.getNextException());
		for (Throwable suppressed : original.getSuppressed()) {
			addSuppressed(suppressed);
		}
	}

	/**
	 * Return the batches that have completed successfully, prior to this exception.
	 * <p>Information about the batch that failed is available via
	 * {@link #getUpdateCounts()}.
	 * @return an array containing for each batch another array containing the numbers of
	 * rows affected by each update in the batch
	 * @see #getUpdateCounts()
	 */
	public int[][] getSuccessfulUpdateCounts() {
		return this.successfulUpdateCounts;
	}

	/**
	 * Return the original {@link BatchUpdateException} that this exception aggregates.
	 * @return the original exception
	 */
	public BatchUpdateException getOriginalException() {
		return this.originalException;
	}

}
