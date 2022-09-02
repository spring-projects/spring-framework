package org.springframework.transaction;


/**
 * Exception when reactive transaction manager fails on commit
 *
 * @author Vladyslav Zolotarov
 * @since 12.05.2022
 */
@SuppressWarnings("serial")
public class TransactionCommitException extends TransactionException {

	public TransactionCommitException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
