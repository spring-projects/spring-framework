/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * AspectJPointcutAdvisor that adapts an {@link AbstractAspectJAdvice}
 * to the {@link org.springframework.aop.PointcutAdvisor} interface.
 *
 * @author Adrian Colyer
 * @author Juergen Hoeller
 * @since 2.0
 */
public class AspectJPointcutAdvisor implements PointcutAdvisor, Ordered {

	private final AbstractAspectJAdvice advice;

	private final Pointcut pointcut;

	private Integer order;


	/**
	 * Create a new AspectJPointcutAdvisor for the given advice
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


	public boolean isPerInstance() {
		return true;
	}

	public Advice getAdvice() {
		return this.advice;
	}

	public Pointcut getPointcut() {
		return this.pointcut;
	}

	public int getOrder() {
		if (this.order != null) {
			return this.order;
		}
		else {
			return this.advice.getOrder();
		}
	}


	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AspectJPointcutAdvisor)) {
			return false;
		}
		AspectJPointcutAdvisor otherAdvisor = (AspectJPointcutAdvisor) other;
		return (ObjectUtils.nullSafeEquals(this.advice, otherAdvisor.advice));
	}

	@Override
	public int hashCode() {
		return AspectJPointcutAdvisor.class.hashCode();
	}

}
