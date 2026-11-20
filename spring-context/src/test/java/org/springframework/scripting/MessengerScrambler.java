/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Twee advice that 'scrambles' the return value
 * of a {@link Messenger} invocation.
 *
 * @author Rick Evans
 */
public class MessengerScrambler {

	public String scramble(ProceedingJoinPoint pjp) throws Throwable {
		String message = (String) pjp.proceed();
		return new StringBuilder(message).reverse().toString();
	}

}
