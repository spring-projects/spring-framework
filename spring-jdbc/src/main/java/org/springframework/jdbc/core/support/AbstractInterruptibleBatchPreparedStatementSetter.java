/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.jdbc.core.InterruptibleBatchPreparedStatementSetter;

/**
 * Abstract implementation of the {@link InterruptibleBatchPreparedStatementSetter}
 * interface, combining the check for available values and setting of those
 * into a single callback method {@link #setValuesIfAvailable}.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setValuesIfAvailable
 */
public abstract class AbstractInterruptibleBatchPreparedStatementSetter
		implements InterruptibleBatchPreparedStatementSetter {

	private boolean exhausted;


	/**
	 * This implementation calls {@link #setValuesIfAvailable}
	 * and sets this instance's exhaustion flag accordingly.
	 */
	@Override
	public final void setValues(PreparedStatement ps, int i) throws SQLException {
		this.exhausted = !setValuesIfAvailable(ps, i);
	}

	/**
	 * This implementation return this instance's current exhaustion flag.
	 */
	@Override
	public final boolean isBatchExhausted(int i) {
		return this.exhausted;
	}

	/**
	 * Check for available values and set them on the given PreparedStatement.
	 * If no values are available anymore, return {@code false}.
	 * @param ps PreparedStatement we'll invoke setter methods on
	 * @param i index of the statement we're issuing in the batch, starting from 0
	 * @return whether there were values to apply (that is, whether the applied
	 * parameters should be added to the batch and this method should be called
	 * for a further iteration)
	 * @throws SQLException if a SQLException is encountered
	 * (i.e. there is no need to catch SQLException)
	 */
	protected abstract boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException;

}
