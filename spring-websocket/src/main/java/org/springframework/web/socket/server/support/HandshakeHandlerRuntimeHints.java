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

package org.springframework.web.socket.server.support;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection hints
 * for {@link AbstractHandshakeHandler}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class HandshakeHandlerRuntimeHints implements RuntimeHintsRegistrar {

	private static final boolean tomcatWsPresent;

	private static final boolean jettyWsPresent;

	private static final boolean undertowWsPresent;

	private static final boolean glassfishWsPresent;

	private static final boolean weblogicWsPresent;

	private static final boolean websphereWsPresent;

	static {
		ClassLoader classLoader = AbstractHandshakeHandler.class.getClassLoader();
		tomcatWsPresent = ClassUtils.isPresent(
				"org.apache.tomcat.websocket.server.WsHttpUpgradeHandler", classLoader);
		jettyWsPresent = ClassUtils.isPresent(
				"org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServerContainer", classLoader);
		undertowWsPresent = ClassUtils.isPresent(
				"io.undertow.websockets.jsr.ServerWebSocketContainer", classLoader);
		glassfishWsPresent = ClassUtils.isPresent(
				"org.glassfish.tyrus.servlet.TyrusHttpUpgradeHandler", classLoader);
		weblogicWsPresent = ClassUtils.isPresent(
				"weblogic.websocket.tyrus.TyrusServletWriter", classLoader);
		websphereWsPresent = ClassUtils.isPresent(
				"com.ibm.websphere.wsoc.WsWsocServerContainer", classLoader);
	}


	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();
		if (tomcatWsPresent) {
			registerType(reflectionHints, "org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy");
		}
		else if (jettyWsPresent) {
			registerType(reflectionHints, "org.springframework.web.socket.server.jetty.JettyRequestUpgradeStrategy");
		}
		else if (undertowWsPresent) {
			registerType(reflectionHints, "org.springframework.web.socket.server.standard.UndertowRequestUpgradeStrategy");
		}
		else if (glassfishWsPresent) {
			registerType(reflectionHints, "org.springframework.web.socket.server.standard.GlassFishRequestUpgradeStrategy");
		}
		else if (weblogicWsPresent) {
			registerType(reflectionHints, "org.springframework.web.socket.server.standard.WebLogicRequestUpgradeStrategy");
		}
		else if (websphereWsPresent) {
			registerType(reflectionHints, "org.springframework.web.socket.server.standard.WebSphereRequestUpgradeStrategy");
		}
	}

	private void registerType(ReflectionHints reflectionHints, String className) {
		reflectionHints.registerType(TypeReference.of(className),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS));
	}

}
