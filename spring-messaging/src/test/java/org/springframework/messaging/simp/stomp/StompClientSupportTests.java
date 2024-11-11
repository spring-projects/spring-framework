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
