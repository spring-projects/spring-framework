/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.retry;

import java.util.function.Predicate;

/**
 * Predicate that matches {@link NumberFormatException}.
 *
 * @author Sam Brannen
 * @since 7.0
 */
class NumberFormatExceptionMatcher implements Predicate<Throwable> {

	@Override
	public boolean test(Throwable throwable) {
		return (throwable instanceof NumberFormatException);
	}

}
