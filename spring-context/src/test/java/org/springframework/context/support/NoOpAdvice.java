/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.support;

import org.springframework.aop.ThrowsAdvice;

/**
 * Advice object that implements <i>multiple</i> Advice interfaces.
 *
 * @author Chris Beams
 */
public class NoOpAdvice implements ThrowsAdvice {

	public void afterThrowing(Exception ex) {
		// no-op
	}

}
