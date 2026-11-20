/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
