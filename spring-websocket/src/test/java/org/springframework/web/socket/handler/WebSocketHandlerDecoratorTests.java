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
