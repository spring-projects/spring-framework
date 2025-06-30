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
 * Represents an HTTP output message that allows for setting a streaming body.
 * Note that such messages typically do not support {@link #getBody()} access.
 *
 * @author Arjen Poutsma
 * @since 4.0
 * @see #setBody
 */
public interface StreamingHttpOutputMessage extends HttpOutputMessage {

	/**
	 * Set the streaming body callback for this message.
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
	 * Defines the contract for bodies that can be written directly to an
	 * {@link OutputStream}. Useful with HTTP client libraries that provide
	 * indirect access to an {@link OutputStream} via a callback mechanism.
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
