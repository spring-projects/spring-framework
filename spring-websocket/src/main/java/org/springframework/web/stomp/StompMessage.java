/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.stomp;

import java.nio.charset.Charset;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompMessage {

	public static final Charset CHARSET = Charset.forName("UTF-8");

	private final StompCommand command;

	private final StompHeaders headers;

	private final byte[] payload;

	private String sessionId;


	public StompMessage(StompCommand command, StompHeaders headers, byte[] payload) {
		this.command = command;
		this.headers = (headers != null) ? headers : new StompHeaders();
		this.payload = payload;
	}

	/**
	 * Constructor for empty payload message.
	 */
	public StompMessage(StompCommand command, StompHeaders headers) {
		this(command, headers, new byte[0]);
	}

	public StompCommand getCommand() {
		return this.command;
	}

	public StompHeaders getHeaders() {
		return this.headers;
	}

	public byte[] getPayload() {
		return this.payload;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getStompSessionId() {
		return this.sessionId;
	}

	@Override
	public String toString() {
		return "StompMessage [" + command + ", headers=" + this.headers + ", payload=" + new String(this.payload) + "]";
	}

}
