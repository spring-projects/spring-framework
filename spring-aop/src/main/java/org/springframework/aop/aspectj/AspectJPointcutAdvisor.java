/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

/**
 * {@code AspectJPointcutAdvisor} adapts an {@link AbstractAspectJAdvice} to the
 * {@link PointcutAdvisor} interface.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectJPointcutAdvisor implements PointcutAdvisor, Ordered {

	private final AbstractAspectJAdvice advice;

	private final Pointcut pointcut;

	private @Nullable Integer order;


	/**
	 * Create a new AspectJPointcutAdvisor for the given advice.
	 * @param advice the AbstractAspectJAdvice to wrap
	 */
	public AspectJPointcutAdvisor(AbstractAspectJAdvice advice) {
		Assert.notNull(advice, "Advice must not be null");
		this.advice = advice;
		this.pointcut = advice.buildSafePointcut();
	}


	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		if (this.order != null) {
			return this.order;
		}
		else {
			return this.advice.getOrder();
		}
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

	/**
	 * Return the name of the aspect (bean) in which the advice was declared.
	 * @since 4.3.15
	 * @see AbstractAspectJAdvice#getAspectName()
	 */
	public String getAspectName() {
		return this.advice.getAspectName();
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AspectJPointcutAdvisor that &&
				this.advice.equals(that.advice)));
	}

	@Override
	public int hashCode() {
		return AspectJPointcutAdvisor.class.hashCode() * 29 + this.advice.hashCode();
	}

}
