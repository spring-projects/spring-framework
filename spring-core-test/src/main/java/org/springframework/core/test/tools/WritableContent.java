/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import java.io.IOException;

/**
 * Callback interface used to write file content. Designed to align with
 * JavaPoet's {@code JavaFile.writeTo} method.
 *
 * @author Phillip Webb
 * @since 6.0
 */
@FunctionalInterface
public interface WritableContent {

	/**
	 * Callback method that should write the content to the given
	 * {@link Appendable}.
	 * @param out the {@link Appendable} used to receive the content
	 * @throws IOException on IO error
	 */
	void writeTo(Appendable out) throws IOException;

}
