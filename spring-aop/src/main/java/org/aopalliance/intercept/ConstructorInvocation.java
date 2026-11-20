/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.aopalliance.intercept;

import java.lang.reflect.Constructor;

/**
 * Description of an invocation to a constructor, given to an
 * interceptor upon constructor-call.
 *
 * <p>A constructor invocation is a joinpoint and can be intercepted
 * by a constructor interceptor.
 *
 * @author Rod Johnson
 * @see ConstructorInterceptor
 */
public interface ConstructorInvocation extends Invocation {

	/**
	 * Get the constructor being called.
	 * <p>This method is a friendly implementation of the
	 * {@link Joinpoint#getStaticPart()} method (same result).
	 * @return the constructor being called
	 */
	Constructor<?> getConstructor();

}
