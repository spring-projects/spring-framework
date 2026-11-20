/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.transport;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
class TransportTypeTests {

	@Test
	void fromValue() {
		assertThat(TransportType.fromValue("websocket")).isEqualTo(TransportType.WEBSOCKET);
		assertThat(TransportType.fromValue("xhr")).isEqualTo(TransportType.XHR);
		assertThat(TransportType.fromValue("xhr_send")).isEqualTo(TransportType.XHR_SEND);
		assertThat(TransportType.fromValue("xhr_streaming")).isEqualTo(TransportType.XHR_STREAMING);
		assertThat(TransportType.fromValue("eventsource")).isEqualTo(TransportType.EVENT_SOURCE);
		assertThat(TransportType.fromValue("htmlfile")).isEqualTo(TransportType.HTML_FILE);
	}

}
