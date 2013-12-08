/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.frame;

import java.nio.charset.Charset;

import org.springframework.util.Assert;

/**
 * Represents a SockJS frame and provides factory methods for creating SockJS frames.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsFrame {

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private static final SockJsFrame openFrame = new SockJsFrame("o");

	private static final SockJsFrame heartbeatFrame = new SockJsFrame("h");

	private static final SockJsFrame closeGoAwayFrame = closeFrame(3000, "Go away!");

	private static final SockJsFrame closeAnotherConnectionOpenFrame = closeFrame(2010, "Another connection still open");


	public static SockJsFrame openFrame() {
		return openFrame;
	}

	public static SockJsFrame heartbeatFrame() {
		return heartbeatFrame;
	}

	public static SockJsFrame messageFrame(SockJsMessageCodec codec, String... messages) {
		String encoded = codec.encode(messages);
		return new SockJsFrame(encoded);
	}

	public static SockJsFrame closeFrameGoAway() {
		return closeGoAwayFrame;
	}

	public static SockJsFrame closeFrameAnotherConnectionOpen() {
		return closeAnotherConnectionOpenFrame;
	}

	public static SockJsFrame closeFrame(int code, String reason) {
		return new SockJsFrame("c[" + code + ",\"" + reason + "\"]");
	}


	private final String content;


	public SockJsFrame(String content) {
		Assert.notNull("Content must not be null");
		this.content = content;
	}


	public String getContent() {
		return this.content;
	}

	public byte[] getContentBytes() {
		return this.content.getBytes(UTF8_CHARSET);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof SockJsFrame)) {
			return false;
		}
		return this.content.equals(((SockJsFrame) other).content);
	}

	@Override
	public int hashCode() {
		return this.content.hashCode();
	}

	@Override
	public String toString() {
		String result = this.content;
		if (result.length() > 80) {
			result = result.substring(0, 80) + "...(truncated)";
		}
		return "SockJsFrame content='" + result.replace("\n", "\\n").replace("\r", "\\r") + "'";
	}

}
