package org.springframework.transaction;


/**
 * Exception when reactive transaction manager fails on commit
 *
 * @author Vladyslav Zolotarov
 */
@SuppressWarnings("serial")
public class TransactionCommitException extends TransactionException {

	public TransactionCommitException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
