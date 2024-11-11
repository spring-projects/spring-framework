/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.expression.spel;

import org.springframework.expression.EvaluationException;
import org.springframework.lang.Nullable;

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

	@Nullable
	private final Object[] inserts;


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
	@Nullable
	public Object[] getInserts() {
		return this.inserts;
	}

}
