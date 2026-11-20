/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.support;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection hints
 * related to {@link HandshakeWebSocketService}.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class HandshakeWebSocketServiceRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerType(HandshakeWebSocketService.initUpgradeStrategy().getClass(),
				MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
	}

}
