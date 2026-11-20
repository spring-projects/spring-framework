/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;

/**
 * Simple implementation of {@link SourceExtractor} that returns {@code null}
 * as the source metadata.
 *
 * <p>This is the default implementation and prevents too much metadata from being
 * held in memory during normal (non-tooled) runtime usage.
 *
 * @author Rob Harrop
 * @since 2.0
 */
public class NullSourceExtractor implements SourceExtractor {

	/**
	 * This implementation simply returns {@code null} for any input.
	 */
	@Override
	public @Nullable Object extractSource(Object sourceCandidate, @Nullable Resource definitionResource) {
		return null;
	}

}
