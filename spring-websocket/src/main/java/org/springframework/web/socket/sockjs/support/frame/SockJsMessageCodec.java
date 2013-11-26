/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs.support.frame;

import java.io.IOException;
import java.io.InputStream;

/**
 * Encode and decode messages to and from a SockJS message frame, essentially an array of
 * JSON-encoded messages. For example:
 *
 * <pre>
 * a["message1","message2"]
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsMessageCodec {

	/**
	 * Encode the given messages as a SockJS message frame. Aside from applying standard
	 * JSON quoting to each message, there are some additional JSON Unicode escaping
	 * rules. See the "JSON Unicode Encoding" section of SockJS protocol (i.e. the
	 * protocol test suite).
	 * @param messages the messages to encode
	 * @return the content for a SockJS message frame, never {@code null}
	 */
	String encode(String[] messages);

	/**
	 * Decode the given SockJS message frame.
	 * @param content the SockJS message frame
	 * @return an array of messages or {@code null}
	 * @throws IOException if the content could not be parsed
	 */
	String[] decode(String content) throws IOException;

	/**
	 * Decode the given SockJS message frame.
	 * @param content the SockJS message frame
	 * @return an array of messages or {@code null}
	 * @throws IOException if the content could not be parsed
	 */
	String[] decodeInputStream(InputStream content) throws IOException;

}
