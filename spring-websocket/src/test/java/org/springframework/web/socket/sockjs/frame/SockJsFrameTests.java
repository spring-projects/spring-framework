/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.frame;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SockJsFrame}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class SockJsFrameTests {

	@Test
	void openFrame() {
		SockJsFrame frame = SockJsFrame.openFrame();

		assertThat(frame.getContent()).isEqualTo("o");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.OPEN);
		assertThat(frame.getFrameData()).isNull();
	}

	@Test
	void heartbeatFrame() {
		SockJsFrame frame = SockJsFrame.heartbeatFrame();

		assertThat(frame.getContent()).isEqualTo("h");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.HEARTBEAT);
		assertThat(frame.getFrameData()).isNull();
	}

	@Test
	void messageArrayFrame() {
		SockJsFrame frame = SockJsFrame.messageFrame(new JacksonJsonSockJsMessageCodec(), "m1", "m2");

		assertThat(frame.getContent()).isEqualTo("a[\"m1\",\"m2\"]");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.MESSAGE);
		assertThat(frame.getFrameData()).isEqualTo("[\"m1\",\"m2\"]");
	}

	@Test
	void messageArrayFrameEmpty() {
		SockJsFrame frame = new SockJsFrame("a");

		assertThat(frame.getContent()).isEqualTo("a[]");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.MESSAGE);
		assertThat(frame.getFrameData()).isEqualTo("[]");

		frame = new SockJsFrame("a[]");

		assertThat(frame.getContent()).isEqualTo("a[]");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.MESSAGE);
		assertThat(frame.getFrameData()).isEqualTo("[]");
	}

	@Test
	void closeFrame() {
		SockJsFrame frame = SockJsFrame.closeFrame(3000, "Go Away!");

		assertThat(frame.getContent()).isEqualTo("c[3000,\"Go Away!\"]");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.CLOSE);
		assertThat(frame.getFrameData()).isEqualTo("[3000,\"Go Away!\"]");
	}

	@Test
	void closeFrameEmpty() {
		SockJsFrame frame = new SockJsFrame("c");

		assertThat(frame.getContent()).isEqualTo("c[]");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.CLOSE);
		assertThat(frame.getFrameData()).isEqualTo("[]");

		frame = new SockJsFrame("c[]");

		assertThat(frame.getContent()).isEqualTo("c[]");
		assertThat(frame.getType()).isEqualTo(SockJsFrameType.CLOSE);
		assertThat(frame.getFrameData()).isEqualTo("[]");
	}

	@Test
	void toStringDoesNotSplitSurrogatePair() {
		String prefix = "a[\"" + "x".repeat(76);
		SockJsFrame frame = new SockJsFrame(prefix + "\uD83D\uDE00\"]");

		assertThat(frame.toString()).isEqualTo("SockJsFrame content='" + prefix + "...(truncated)'");
	}

}
