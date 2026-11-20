/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.handler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture for {@link WebSocketHandlerDecorator}.
 *
 * @author Rossen Stoyanchev
 */
class WebSocketHandlerDecoratorTests {

	@Test
	void getLastHandler() {
		AbstractWebSocketHandler h1 = new AbstractWebSocketHandler() {
		};
		WebSocketHandlerDecorator h2 = new WebSocketHandlerDecorator(h1);
		WebSocketHandlerDecorator h3 = new WebSocketHandlerDecorator(h2);

		assertThat(h3.getLastHandler()).isSameAs(h1);
	}

}
