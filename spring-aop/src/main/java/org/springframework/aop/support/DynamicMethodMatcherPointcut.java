/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * Convenient superclass when we want to force subclasses to
 * implement MethodMatcher interface, but subclasses
 * will want to be pointcuts. The getClassFilter() method can
 * be overridden to customize ClassFilter behavior as well.
 *
 * @author Rod Johnson
 */
public abstract class DynamicMethodMatcherPointcut extends DynamicMethodMatcher implements Pointcut {

	@Override
	public ClassFilter getClassFilter() {
		return ClassFilter.TRUE;
	}

	@Override
	public final MethodMatcher getMethodMatcher() {
		return this;
	}

}
