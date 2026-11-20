/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * Common functional interface for I/O content consumption, for example
 * consuming an {@link java.io.InputStream} or a {@link java.io.Reader}.
 *
 * @author Juergen Hoeller
 * @since 7.1
 * @param <C> the type of stream/reader
 * @see org.springframework.core.io.Resource#consumeContent
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
