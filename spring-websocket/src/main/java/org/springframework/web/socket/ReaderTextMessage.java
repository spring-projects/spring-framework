/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket;

/**
 * A {@link WebSocketMessage} that contains a textual {@link String} payload.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class ReaderTextMessage extends WebSocketMessage<String> {

	/**
	 * Create a new {@link ReaderTextMessage} instance.
	 * @param payload the non-null payload
	 */
	public ReaderTextMessage(CharSequence payload) {
		super(payload.toString(), true);
	}

	/**
	 * Create a new {@link ReaderTextMessage} instance.
	 * @param payload the non-null payload
	 * @param isLast whether this the last part of a message received or transmitted in parts
	 */
	public ReaderTextMessage(CharSequence payload, boolean isLast) {
		super(payload.toString(), isLast);
	}


	@Override
	protected int getPayloadSize() {
		return getPayload().length();
	}

	@Override
	protected String toStringPayload() {
		return (getPayloadSize() > 10) ? getPayload().substring(0, 10) + ".." : getPayload();
	}

}
