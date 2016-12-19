/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.web.reactive.socket;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

/**
 * Handler for a WebSocket session.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSocketHandler {

	/**
	 * Return the list of sub-protocols supported by this handler.
	 * <p>By default an empty list is returned.
	 */
	default List<String> getSubProtocols() {
		return Collections.emptyList();
	}

	/**
	 * Handle the WebSocket session.
	 * @param session the session to handle
	 * @return completion {@code Mono<Void>} to indicate the outcome of the
	 * WebSocket session handling.
	 */
	Mono<Void> handle(WebSocketSession session);

}
