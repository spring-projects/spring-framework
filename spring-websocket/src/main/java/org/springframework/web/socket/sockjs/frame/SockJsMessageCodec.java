/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.sockjs.frame;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.Nullable;

/**
 * Encode and decode messages to and from a SockJS message frame,
 * essentially an array of JSON-encoded messages. For example:
 *
 * <pre class="code">
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
	 * @return the content for a SockJS message frame (never {@code null})
	 */
	String encode(String... messages);

	/**
	 * Decode the given SockJS message frame.
	 * @param content the SockJS message frame
	 * @return an array of messages, or {@code null} if none
	 * @throws IOException if the content could not be parsed
	 */
	String @Nullable [] decode(String content) throws IOException;

	/**
	 * Decode the given SockJS message frame.
	 * @param content the SockJS message frame
	 * @return an array of messages, or {@code null} if none
	 * @throws IOException if the content could not be parsed
	 */
	String @Nullable [] decodeInputStream(InputStream content) throws IOException;

}
