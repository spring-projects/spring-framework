/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.sample.type;

import org.springframework.stereotype.Indexed;

/**
 * A sample interface flagged with {@link Indexed} to indicate that a stereotype
 * for all implementations should be added to the index.
 *
 * @author Stephane Nicoll
 */
@Indexed
public interface Repo<T, I> {
}
