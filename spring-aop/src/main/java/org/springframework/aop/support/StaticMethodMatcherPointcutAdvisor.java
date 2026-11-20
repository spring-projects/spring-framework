/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * Convenient base class for Advisors that are also static pointcuts.
 * Serializable if Advice and subclass are.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public abstract class StaticMethodMatcherPointcutAdvisor extends StaticMethodMatcherPointcut
		implements PointcutAdvisor, Ordered, Serializable {

	private Advice advice = EMPTY_ADVICE;

	private int order = Ordered.LOWEST_PRECEDENCE;


	/**
	 * Create a new StaticMethodMatcherPointcutAdvisor,
	 * expecting bean-style configuration.
	 * @see #setAdvice
	 */
	public StaticMethodMatcherPointcutAdvisor() {
	}

	/**
	 * Create a new StaticMethodMatcherPointcutAdvisor for the given advice.
	 * @param advice the Advice to use
	 */
	public StaticMethodMatcherPointcutAdvisor(Advice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setAdvice(Advice advice) {
		this.advice = advice;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this;
	}

}
