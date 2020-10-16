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

package org.springframework.messaging.tcp;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * Allows specifying some configuration to setup the TCP connection.
 *
 * @author Ruben Perez
 * @see TcpConnectionHandler
 */
public interface TcpConnectionConfiguration {
	/**
	 * Specifies the attributes to pass on to the TCP client for setting up the TCP connection.
	 * @param settings a @{@link Map} of attributes
	 */
	void attributes(@Nonnull Map<String, String> settings);
}
