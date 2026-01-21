/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.util.StreamUtils;

/**
 * Contract for {@code HttpOutputMessage} implementations to expose the ability
 * to stream request body content by writing to an {@link OutputStream} from
 * a callback.
 *
 * <p>The {@link #setBody(Body)} method provides the option to stream, and is
 * mutually exclusive use of {@link #getBody()}, which instead returns an
 * {@code OutputStream} that aggregates the request body before sending it.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see #setBody
 */
public interface StreamingHttpOutputMessage extends HttpOutputMessage {

	/**
	 * Set the streaming body callback for this message.
	 * <p>Note that this is mutually exclusive with {@link #getBody()}, which
	 * may instead aggregate the request body before sending it.
	 * @param body the streaming body callback
	 */
	void setBody(Body body);

	/**
	 * Variant of {@link #setBody(Body)} for a non-streaming write.
	 * @param body the content to write
	 * @throws IOException if an I/O exception occurs
	 * @since 7.0
	 */
	default void setBody(byte[] body) throws IOException {
		setBody(new Body() {
			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				StreamUtils.copy(body, outputStream);
			}

			@Override
			public boolean repeatable() {
				return true;
			}
		});
	}



	/**
	 * Contract to stream request body content to an {@link OutputStream}.
	 * In some HTTP client libraries this is only possible indirectly through a
	 * callback mechanism.
	 */
	@FunctionalInterface
	interface Body {

		/**
		 * Write this body to the given {@link OutputStream}.
		 * @param outputStream the output stream to write to
		 * @throws IOException in case of I/O errors
		 */
		void writeTo(OutputStream outputStream) throws IOException;

		/**
		 * Indicates whether this body is capable of
		 * {@linkplain #writeTo(OutputStream) writing its data} more than
		 * once. The default implementation returns {@code false}.
		 * @return {@code true} if this body can be written repeatedly,
		 * {@code false} otherwise
		 * @since 6.1
		 */
		default boolean repeatable() {
			return false;
		}
	}

}
