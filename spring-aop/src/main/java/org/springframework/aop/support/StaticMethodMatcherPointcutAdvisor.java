/*
 * Copyright 2002-2012 the original author or authors.
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

	private int order = Integer.MAX_VALUE;

	private Advice advice;


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
	public boolean isPerInstance() {
		return true;
	}

	@Override
	public Pointcut getPointcut() {
		return this;
	}

}
