/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.adapter.jetty;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} for Jetty WebSocket support.
 *
 * @author Eymen Onar
 */
class JettyWebSocketRuntimeHints implements RuntimeHintsRegistrar {

	private static final String JETTY_LISTENER_CLASS_NAME = "org.eclipse.jetty.websocket.api.Session$Listener";

	private static final String HANDLER_ADAPTER_CLASS_NAME =
			"org.springframework.web.socket.adapter.jetty.JettyWebSocketHandlerAdapter";


	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		if (ClassUtils.isPresent(JETTY_LISTENER_CLASS_NAME, classLoader)) {
			hints.reflection().registerType(TypeReference.of(HANDLER_ADAPTER_CLASS_NAME),
					MemberCategory.INVOKE_PUBLIC_METHODS);
		}
	}

}
