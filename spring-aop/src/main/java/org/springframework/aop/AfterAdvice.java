/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * Common marker interface for after advice,
 * such as {@link AfterReturningAdvice} and {@link ThrowsAdvice}.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see BeforeAdvice
 */
public interface AfterAdvice extends Advice {

}
