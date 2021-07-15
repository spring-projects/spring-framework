/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.socket.messaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.web.socket.TextMessage;

/**
 * A builder for creating WebSocket messages with STOMP frame content.
 *
 * @author Rossen Stoyanchev
 */
public class StompTextMessageBuilder {

	private StompCommand command;

	private final List<String> headerLines = new ArrayList<>();

	private String body;


	private StompTextMessageBuilder(StompCommand command) {
		this.command = command;
	}

	public static StompTextMessageBuilder create(StompCommand command) {
		return new StompTextMessageBuilder(command);
	}

	public StompTextMessageBuilder headers(String... headerLines) {
		this.headerLines.addAll(Arrays.asList(headerLines));
		return this;
	}

	public StompTextMessageBuilder body(String body) {
		this.body = body;
		return this;
	}

	public TextMessage build() {
		StringBuilder sb = new StringBuilder(this.command.name()).append('\n');
		for (String line : this.headerLines) {
			sb.append(line).append('\n');
		}
		sb.append('\n');
		if (this.body != null) {
			sb.append(this.body);
		}
		sb.append('\u0000');
		return new TextMessage(sb.toString());
	}

}
