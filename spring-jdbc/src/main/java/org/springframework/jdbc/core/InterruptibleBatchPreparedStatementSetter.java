/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

/**
 * Extension of the {@link BatchPreparedStatementSetter} interface,
 * adding a batch exhaustion check.
 *
 * <p>This interface allows you to signal the end of a batch rather than
 * having to determine the exact batch size upfront. Batch size is still
 * being honored, but it is now the maximum size of the batch.
 *
 * <p>The {@link #isBatchExhausted} method is called after each call to
 * {@link #setValues} to determine whether there were some values added,
 * or if the batch was determined to be complete and no additional values
 * were provided during the last call to {@code setValues}.
 *
 * <p>Consider extending the
 * {@link org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter}
 * base class instead of implementing this interface directly, using a single
 * {@code setValuesIfAvailable} callback method that checks for available
 * values and sets them, returning whether values have actually been provided.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.0
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 * @see org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter
 */
public interface InterruptibleBatchPreparedStatementSetter extends BatchPreparedStatementSetter {

	/**
	 * Return whether the batch is complete, that is, whether there were no
	 * additional values added during the last {@code setValues} call.
	 * <p><b>NOTE:</b> If this method returns {@code true}, any parameters
	 * that might have been set during the last {@code setValues} call will
	 * be ignored! Make sure that you set a corresponding internal flag if you
	 * detect exhaustion <i>at the beginning</i> of your {@code setValues}
	 * implementation, letting this method return {@code true} based on the flag.
	 * @param i index of the statement we're issuing in the batch, starting from 0
	 * @return whether the batch is already exhausted
	 * @see #setValues
	 * @see org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter#setValuesIfAvailable
	 */
	boolean isBatchExhausted(int i);

}
