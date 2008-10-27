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

import org.aopalliance.aop.Advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

/**
 * Abstract BeanFactory-based PointcutAdvisor that allows for any Advice
 * to be configured as reference to an Advice bean in a BeanFactory.
 *
 * <p>Specifying the name of an advice bean instead of the advice object itself
 * (if running within a BeanFactory) increases loose coupling at initialization time,
 * in order to not initialize the advice object until the pointcut actually matches.
 *
 * @author Juergen Hoeller
 * @since 2.0.2
 * @see #setAdviceBeanName
 * @see DefaultBeanFactoryPointcutAdvisor
 */
public abstract class AbstractBeanFactoryPointcutAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

	private String adviceBeanName;

	private BeanFactory beanFactory;

	private Advice advice;

	private final Object adviceMonitor = new Object();


	/**
	 * Specify the name of the advice bean that this advisor should refer to.
	 * <p>An instance of the specified bean will be obtained on first access
	 * of this advisor's advice. This advisor will only ever obtain at most one
	 * single instance of the advice bean, caching the instance for the lifetime
	 * of the advisor.
	 * @see #getAdvice()
	 */
	public void setAdviceBeanName(String adviceBeanName) {
		this.adviceBeanName = adviceBeanName;
	}

	/**
	 * Return the name of the advice bean that this advisor refers to, if any.
	 */
	public String getAdviceBeanName() {
		return this.adviceBeanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	public Advice getAdvice() {
		synchronized (this.adviceMonitor) {
			if (this.advice == null && this.adviceBeanName != null) {
				Assert.state(this.beanFactory != null, "BeanFactory must be set to resolve 'adviceBeanName'");
				this.advice = (Advice) this.beanFactory.getBean(this.adviceBeanName, Advice.class);
			}
			return this.advice;
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + ": advice bean '" + getAdviceBeanName() + "'";
	}

}
