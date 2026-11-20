/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

/**
 * Extension of {@link DataBuffer} that allows for buffers that can be given
 * hints for debugging purposes.
 *
 * @author Arjen Poutsma
 * @since 6.0
 */
public interface TouchableDataBuffer extends DataBuffer {

	/**
	 * Associate the given hint with the data buffer for debugging purposes.
	 * @return this buffer
	 */
	TouchableDataBuffer touch(Object hint);

}
