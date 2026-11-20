/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.simp.stomp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StompClientSupport}.
 *
 * @author Rossen Stoyanchev
 */
class StompClientSupportTests {

	private final StompClientSupport stompClient = new StompClientSupport() {};


	@Test
	void defaultHeartbeatValidation() {
		trySetDefaultHeartbeat(new long[] {-1, 0});
		trySetDefaultHeartbeat(new long[] {0, -1});
	}

	private void trySetDefaultHeartbeat(long[] heartbeat) {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.stompClient.setDefaultHeartbeat(heartbeat));
	}

	@Test
	void defaultHeartbeatValue() {
		assertThat(this.stompClient.getDefaultHeartbeat()).isEqualTo(new long[] {10000, 10000});
	}

	@Test
	void isDefaultHeartbeatEnabled() {
		assertThat(this.stompClient.getDefaultHeartbeat()).isEqualTo(new long[] {10000, 10000});
		assertThat(this.stompClient.isDefaultHeartbeatEnabled()).isTrue();

		this.stompClient.setDefaultHeartbeat(new long[] {0, 0});
		assertThat(this.stompClient.isDefaultHeartbeatEnabled()).isFalse();
	}

	@Test
	void processConnectHeadersDefault() {
		StompHeaders connectHeaders = this.stompClient.processConnectHeaders(null);

		assertThat(connectHeaders).isNotNull();
		assertThat(connectHeaders.getHeartbeat()).isEqualTo(new long[] {10000, 10000});
	}

	@Test
	void processConnectHeadersWithExplicitHeartbeat() {

		StompHeaders connectHeaders = new StompHeaders();
		connectHeaders.setHeartbeat(new long[] {15000, 15000});
		connectHeaders = this.stompClient.processConnectHeaders(connectHeaders);

		assertThat(connectHeaders).isNotNull();
		assertThat(connectHeaders.getHeartbeat()).isEqualTo(new long[] {15000, 15000});
	}

}
