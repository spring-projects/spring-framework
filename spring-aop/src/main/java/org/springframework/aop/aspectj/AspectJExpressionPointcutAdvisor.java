/*
 * Copyright 2002-2006 the original author or authors.
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

/**
 * Spring AOP Advisor that can be used for any AspectJ pointcut expression.
 *
 * @author Rob Harrop
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();


	public Pointcut getPointcut() {
		return this.pointcut;
	}

	public void setExpression(String expression) {
		this.pointcut.setExpression(expression);
	}

	public void setLocation(String location) {
		this.pointcut.setLocation(location);
	}

	public void setParameterTypes(Class[] types) {
		this.pointcut.setParameterTypes(types);
	}

	public void setParameterNames(String[] names) {
		this.pointcut.setParameterNames(names);
	}

	public String getLocation() {
		return this.pointcut.getLocation();
	}

	public String getExpression() {
		return this.pointcut.getExpression();
	}

}
