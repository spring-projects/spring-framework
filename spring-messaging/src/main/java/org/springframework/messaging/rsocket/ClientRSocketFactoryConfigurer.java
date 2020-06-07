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

/**
 * Strategy to apply configuration to a client side {@code RSocketFactory}.
 * that's being prepared by {@link RSocketRequester.Builder} to connect
 * to a server.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @deprecated as of 5.2.6 following the deprecation of
 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory RSocketFactory.ClientRSocketFactory}
 * in RSocket 1.0 RC7. Please, use {@link RSocketConnectorConfigurer}.
 */
@FunctionalInterface
@Deprecated
public interface ClientRSocketFactoryConfigurer {

	/**
	 * Apply configuration to the given {@code ClientRSocketFactory}.
	 */
	void configure(io.rsocket.RSocketFactory.ClientRSocketFactory rsocketFactory);

}
