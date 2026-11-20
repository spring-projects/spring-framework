/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

/**
 * Interface to be implemented by types that can supply the information
 * needed to sort advice/advisors by AspectJ's precedence rules.
 *
 * @author Adrian Colyer
 * @since 2.0
 * @see org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator
 */
public interface AspectJPrecedenceInformation extends Ordered {

	// Implementation note:
	// We need the level of indirection this interface provides as otherwise the
	// AspectJPrecedenceComparator must ask an Advisor for its Advice in all cases
	// in order to sort advisors. This causes problems with the
	// InstantiationModelAwarePointcutAdvisor which needs to delay creating
	// its advice for aspects with non-singleton instantiation models.

	/**
	 * Return the name of the aspect (bean) in which the advice was declared.
	 */
	String getAspectName();

	/**
	 * Return the declaration order of the advice member within the aspect.
	 */
	int getDeclarationOrder();

	/**
	 * Return whether this is a before advice.
	 */
	boolean isBeforeAdvice();

	/**
	 * Return whether this is an after advice.
	 */
	boolean isAfterAdvice();

}
