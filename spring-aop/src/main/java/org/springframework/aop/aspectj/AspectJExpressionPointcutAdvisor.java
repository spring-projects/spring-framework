/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractGenericPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;

/**
 * Spring AOP Advisor that can be used for any AspectJ pointcut expression.
 *
 * @author Rob Harrop
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcutAdvisor extends AbstractGenericPointcutAdvisor implements BeanFactoryAware {

	private final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();


	public void setExpression(@Nullable String expression) {
		this.pointcut.setExpression(expression);
	}

	@Nullable
	public String getExpression() {
		return this.pointcut.getExpression();
	}

	public void setLocation(@Nullable String location) {
		this.pointcut.setLocation(location);
	}

	@Nullable
	public String getLocation() {
		return this.pointcut.getLocation();
	}

	public void setParameterNames(String... names) {
		this.pointcut.setParameterNames(names);
	}

	public void setParameterTypes(Class<?>... types) {
		this.pointcut.setParameterTypes(types);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.pointcut.setBeanFactory(beanFactory);
	}

	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
