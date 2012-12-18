/*
 * Copyright 2002-2007 the original author or authors.
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

/**
 * Convenient Pointcut-driven Advisor implementation.
 *
 * <p>This is the most commonly used Advisor implementation. It can be used
 * with any pointcut and advice type, except for introductions. There is
 * normally no need to subclass this class, or to implement custom Advisors.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPointcut
 * @see #setAdvice
 */
public class DefaultPointcutAdvisor extends AbstractGenericPointcutAdvisor implements Serializable {

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * Create an empty DefaultPointcutAdvisor.
	 * <p>Advice must be set before use using setter methods.
	 * Pointcut will normally be set also, but defaults to {@code Pointcut.TRUE}.
	 */
	public DefaultPointcutAdvisor() {
	}

	/**
	 * Create a DefaultPointcutAdvisor that matches all methods.
	 * <p>{@code Pointcut.TRUE} will be used as Pointcut.
	 * @param advice the Advice to use
	 */
	public DefaultPointcutAdvisor(Advice advice) {
		this(Pointcut.TRUE, advice);
	}

	/**
	 * Create a DefaultPointcutAdvisor, specifying Pointcut and Advice.
	 * @param pointcut the Pointcut targeting the Advice
	 * @param advice the Advice to run when Pointcut matches
	 */
	public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
		this.pointcut = pointcut;
		setAdvice(advice);
	}


	/**
	 * Specify the pointcut targeting the advice.
	 * <p>Default is {@code Pointcut.TRUE}.
	 * @see #setAdvice
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = (pointcut != null ? pointcut : Pointcut.TRUE);
	}

	public Pointcut getPointcut() {
		return this.pointcut;
	}


	@Override
	public String toString() {
		return getClass().getName() + ": pointcut [" + getPointcut() + "]; advice [" + getAdvice() + "]";
	}

}
