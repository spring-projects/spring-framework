/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.EvaluationException;

/**
 * Root exception for Spring EL related exceptions.
 *
 * <p>Rather than holding a hard-coded string indicating the problem, it records
 * a message key and the inserts for the message.
 *
 * <p>See {@link SpelMessage} for the list of all possible messages that can occur.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("serial")
public class SpelEvaluationException extends EvaluationException {

	private final SpelMessage message;

	private final @Nullable Object[] inserts;


	public SpelEvaluationException(SpelMessage message, @Nullable Object... inserts) {
		super(message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(int position, SpelMessage message, @Nullable Object... inserts) {
		super(position, message.formatMessage(inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(int position, @Nullable Throwable cause, SpelMessage message, @Nullable Object... inserts) {
		super(position, message.formatMessage(inserts), cause);
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(@Nullable Throwable cause, SpelMessage message, @Nullable Object... inserts) {
		super(message.formatMessage(inserts), cause);
		this.message = message;
		this.inserts = inserts;
	}


	/**
	 * Set the position in the related expression which gave rise to this exception.
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * Return the message code.
	 */
	public SpelMessage getMessageCode() {
		return this.message;
	}

	/**
	 * Return the message inserts.
	 */
	public @Nullable Object @Nullable [] getInserts() {
		return this.inserts;
	}

}
