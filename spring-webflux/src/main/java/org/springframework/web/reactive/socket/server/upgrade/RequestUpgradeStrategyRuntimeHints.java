/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.socket.server.upgrade;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} for {@link RequestUpgradeStrategy} implementations.
 *
 * @author Andy Wilkinson
 */
class RequestUpgradeStrategyRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerType(JettyRequestUpgradeStrategy.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS).onReachableType(
						TypeReference.of("org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer")));
		hints.reflection().registerType(ReactorNettyRequestUpgradeStrategy.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
						.onReachableType(TypeReference.of("reactor.netty.http.server.HttpServerResponse")));
		hints.reflection().registerType(TomcatRequestUpgradeStrategy.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
						.onReachableType(TypeReference.of("org.apache.tomcat.websocket.server.WsHttpUpgradeHandler")));
		hints.reflection().registerType(UndertowRequestUpgradeStrategy.class,
				hint -> hint.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
						.onReachableType(TypeReference.of("io.undertow.websockets.WebSocketProtocolHandshakeHandler")));
	}

}
