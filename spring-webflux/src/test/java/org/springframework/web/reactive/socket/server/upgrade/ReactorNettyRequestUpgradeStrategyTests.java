/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.socket.server.upgrade;

import org.junit.jupiter.api.Test;
import reactor.netty.http.server.WebsocketServerSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactorNettyRequestUpgradeStrategy}.
 *
 * @author Rossen Stoyanchev
 */
class ReactorNettyRequestUpgradeStrategyTests {

	@Test // gh-25315
	void defaultWebSocketSpecBuilderIsUniquePerRequest() {
		ReactorNettyRequestUpgradeStrategy strategy = new ReactorNettyRequestUpgradeStrategy();
		WebsocketServerSpec spec1 = strategy.buildSpec("p1");
		WebsocketServerSpec spec2 = strategy.getWebsocketServerSpec();

		assertThat(spec1.protocols()).isEqualTo("p1");
		assertThat(spec2.protocols()).isNull();
	}

}
