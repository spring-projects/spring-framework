/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
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

	@Nullable
	private Integer order;


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
