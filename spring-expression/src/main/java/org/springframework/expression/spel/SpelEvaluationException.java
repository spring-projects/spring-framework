/*
 * Copyright 2004-2009 the original author or authors.
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
 * records a message key and the inserts for the message. See {@link SpelMessage} for the list of all possible messages
 * that can occur.
 *
 * @author Andy Clement
 * @since 3.0
 */
@SuppressWarnings("serial")
public class SpelEvaluationException extends EvaluationException {

	private SpelMessage message;
	private Object[] inserts;

	public SpelEvaluationException(SpelMessage message, Object... inserts) {
		super(message.formatMessage(0, inserts)); // TODO poor position information, can the callers not really supply something?
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(int position, SpelMessage message, Object... inserts) {
		super(position, message.formatMessage(position, inserts));
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(int position, Throwable cause,
			SpelMessage message, Object... inserts) {
		super(position,message.formatMessage(position,inserts),cause);
		this.message = message;
		this.inserts = inserts;
	}

	public SpelEvaluationException(Throwable cause, SpelMessage message, Object... inserts) {
		super(message.formatMessage(0,inserts),cause);
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
	 * @return the message code
	 */
	public SpelMessage getMessageCode() {
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
