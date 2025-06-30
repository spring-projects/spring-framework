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

package org.springframework.transaction;

/**
 * Exception that represents a transaction failure caused by a heuristic
 * decision on the side of the transaction coordinator.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 17.03.2003
 */
@SuppressWarnings("serial")
public class HeuristicCompletionException extends TransactionException {

	/**
	 * Unknown outcome state.
	 */
	public static final int STATE_UNKNOWN = 0;

	/**
	 * Committed outcome state.
	 */
	public static final int STATE_COMMITTED = 1;

	/**
	 * Rolledback outcome state.
	 */
	public static final int STATE_ROLLED_BACK = 2;

	/**
	 * Mixed outcome state.
	 */
	public static final int STATE_MIXED = 3;


	public static String getStateString(int state) {
		return switch (state) {
			case STATE_COMMITTED -> "committed";
			case STATE_ROLLED_BACK -> "rolled back";
			case STATE_MIXED -> "mixed";
			default -> "unknown";
		};
	}


	/**
	 * The outcome state of the transaction: have some or all resources been committed?
	 */
	private final int outcomeState;


	/**
	 * Constructor for HeuristicCompletionException.
	 * @param outcomeState the outcome state of the transaction
	 * @param cause the root cause from the transaction API in use
	 */
	public HeuristicCompletionException(int outcomeState, Throwable cause) {
		super("Heuristic completion: outcome state is " + getStateString(outcomeState), cause);
		this.outcomeState = outcomeState;
	}

	/**
	 * Return the outcome state of the transaction state,
	 * as one of the constants in this class.
	 * @see #STATE_UNKNOWN
	 * @see #STATE_COMMITTED
	 * @see #STATE_ROLLED_BACK
	 * @see #STATE_MIXED
	 */
	public int getOutcomeState() {
		return this.outcomeState;
	}

}
