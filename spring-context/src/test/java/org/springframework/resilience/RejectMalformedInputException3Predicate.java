/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.resilience;

import java.lang.reflect.Method;
import java.nio.charset.MalformedInputException;

import org.springframework.resilience.retry.MethodRetryPredicate;

class RejectMalformedInputException3Predicate implements MethodRetryPredicate {

	@Override
	public boolean shouldRetry(Method method, Throwable throwable) {
		return !(throwable.getClass() == MalformedInputException.class && throwable.getMessage().contains("3"));
	}

}
