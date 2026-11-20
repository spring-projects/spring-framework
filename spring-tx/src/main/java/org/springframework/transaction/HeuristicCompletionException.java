/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
