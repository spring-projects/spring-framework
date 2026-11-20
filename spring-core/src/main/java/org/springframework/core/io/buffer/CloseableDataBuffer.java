/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffers that can be used
 * in a {@code try}-with-resources statement.

 * @author Arjen Poutsma
 * @since 6.0
 */
public interface CloseableDataBuffer extends DataBuffer, AutoCloseable {

	/**
	 * Closes this data buffer, freeing any resources.
	 * @throws IllegalStateException if this buffer has already been closed
	 */
	@Override
	void close();

}
