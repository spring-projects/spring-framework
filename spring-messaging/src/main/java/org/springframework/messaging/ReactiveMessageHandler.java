/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging;

import reactor.core.publisher.Mono;

/**
 * Reactive contract for handling a {@link Message}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see MessageHandler
 */
@FunctionalInterface
public interface ReactiveMessageHandler {

	/**
	 * Handle the given message.
	 * @param message the message to be handled
	 * @return a completion {@link Mono} for the result of the message handling
	 */
	Mono<Void> handleMessage(Message<?> message);

}
