/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.rsocket;

import io.rsocket.core.RSocketConnector;

/**
 * Strategy to apply configuration to an {@code RSocketConnector}. For use with
 * {@link RSocketRequester.Builder#rsocketConnector RSocketRequester.Builder}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2.6
 */
@FunctionalInterface
public interface RSocketConnectorConfigurer {

	/**
	 * Apply configuration to the given {@code RSocketConnector}.
	 */
	void configure(RSocketConnector connector);

}
