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
		SockJsFrame frame = SockJsFrame.messageFrame(new Jackson2SockJsMessageCodec(), "m1", "m2");

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

}
