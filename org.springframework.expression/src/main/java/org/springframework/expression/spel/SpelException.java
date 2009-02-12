/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel;

import org.springframework.expression.EvaluationException;

/**
 * Root exception for Spring EL related exceptions. Rather than holding a hard coded string indicating the problem, it
 * records a message key and the inserts for the message. See {@link SpelMessages} for the list of all possible messages
 * that can occur.
 * 
 * @author Andy Clement
 * @since 3.0
 */
public class SpelException extends EvaluationException {

	private SpelMessages message;
	private int position = -1;
	private Object[] inserts;

	public SpelException(int position, Throwable cause, SpelMessages message, Object... inserts) {
		super(cause);
		this.position = position;
		this.message = message;
		this.inserts = inserts;
	}

	public SpelException(Throwable cause, SpelMessages message, Object... inserts) {
		super(cause);
		this.message = message;
		this.inserts = inserts;
	}

	public SpelException(int position, SpelMessages message, Object... inserts) {
		super((Throwable)null);
		this.position = position;
		this.message = message;
		this.inserts = inserts;
	}

	public SpelException(SpelMessages message, Object... inserts) {
		super((Throwable)null);
		this.message = message;
		this.inserts = inserts;
	}

	public SpelException(String expressionString, int position, Throwable cause, SpelMessages message, Object... inserts) {
		super(expressionString, cause);
		this.position = position;
		this.message = message;
		this.inserts = inserts;
	}

	/**
	 * @return a formatted message with inserts applied
	 */
	@Override
	public String getMessage() {
		if (message != null)
			return message.formatMessage(position, inserts);
		else
			return super.getMessage();
	}

	/**
	 * @return the position within the expression that gave rise to the exception (or -1 if unknown)
	 */
	public int getPosition() {
		return this.position;
	}

	/**
	 * @return the unformatted message
	 */
	public SpelMessages getMessageUnformatted() {
		return this.message;
	}

	/**
	 * Set the position in the related expression which gave rise to this exception.
	 * 
	 * @param position the position in the expression that gave rise to the exception
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * @return the message inserts
	 */
	public Object[] getInserts() {
		return inserts;
	}

}
