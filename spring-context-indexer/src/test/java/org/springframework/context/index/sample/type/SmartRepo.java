/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.index.sample.type;

import org.springframework.stereotype.Indexed;

/**
 * A {@link Repo} that requires an extra stereotype.
 *
 * @author Stephane Nicoll
 */
@Indexed
public interface SmartRepo<T, I> extends Repo<T, I> {
}
