/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.support;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;

/**
 * A {@link GenericMessage} with a {@link Throwable} payload.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 4.0
 * @see MessageBuilder
 */
public class ErrorMessage extends GenericMessage<Throwable> {

	private static final long serialVersionUID = -5470210965279837728L;


	/**
	 * Create a new message with the given payload.
	 * @param payload the message payload (never {@code null})
	 */
	public ErrorMessage(Throwable payload) {
		super(payload);
	}

	/**
	 * Create a new message with the given payload and headers.
	 * The content of the given header map is copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers to use for initialization
	 */
	public ErrorMessage(Throwable payload, Map<String, Object> headers) {
		super(payload, headers);
	}

	/**
	 * A constructor with the {@link MessageHeaders} instance to use.
	 * <p><strong>Note:</strong> the given {@code MessageHeaders} instance
	 * is used directly in the new message, i.e. it is not copied.
	 * @param payload the message payload (never {@code null})
	 * @param headers message headers
	 */
	public ErrorMessage(Throwable payload, MessageHeaders headers) {
		super(payload, headers);
	}

}
