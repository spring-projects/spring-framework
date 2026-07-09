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

package org.springframework.util.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

import org.springframework.core.io.Resource;

/**
 * Common functional interface for I/O content consumption, for example
 * consuming an {@link java.io.InputStream} or a {@link java.io.Reader}.
 *
 * @author Juergen Hoeller
 * @since 7.1
 * @param <C> the type of stream/reader
 * @see Resource#consumeContent
 * @see org.springframework.core.io.support.EncodedResource#consumeContent
 * @see ThrowingConsumer
 */
@FunctionalInterface
public interface IOConsumer<C> extends Consumer<C> {

	/**
	 * Performs this operation on the given argument, possibly throwing
	 * an {@link IOException}.
	 * @param content the stream/reader
	 * @throws IOException on error
	 */
	void acceptWithException(C content) throws IOException;

	/**
	 * Default {@link Consumer#accept(Object)} that wraps any thrown
	 * {@link IOException} in an {@link UncheckedIOException}.
	 * @see java.util.function.Consumer#accept(Object)
	 */
	default void accept(C input) {
		try {
			acceptWithException(input);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
