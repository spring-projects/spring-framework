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

import org.aspectj.lang.ProceedingJoinPoint;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.core.Ordered;

/**
 * Used in advice precedence tests (surprise!)
 *
 * @author Adrian Colyer
 */
public class PrecedenceTestAspect implements BeanNameAware, Ordered {

	private String name;

	private int order = Ordered.LOWEST_PRECEDENCE;

	private Collaborator collaborator;


	public void setBeanName(String name) {
		this.name = name;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	public void setCollaborator(Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	public void beforeAdviceOne() {
		this.collaborator.beforeAdviceOne(this.name);
	}

	public void beforeAdviceTwo() {
		this.collaborator.beforeAdviceTwo(this.name);
	}

	public int aroundAdviceOne(ProceedingJoinPoint pjp) {
		int ret = -1;
		this.collaborator.aroundAdviceOne(this.name);
		try {
			ret = ((Integer)pjp.proceed()).intValue();
		} 
		catch(Throwable t) { throw new RuntimeException(t); }
		this.collaborator.aroundAdviceOne(this.name);
		return ret;
	}

	public int aroundAdviceTwo(ProceedingJoinPoint pjp) {
		int ret = -1;
		this.collaborator.aroundAdviceTwo(this.name);
		try {
			ret = ((Integer)pjp.proceed()).intValue();
		} 
		catch(Throwable t) {throw new RuntimeException(t);}
		this.collaborator.aroundAdviceTwo(this.name);
		return ret;
	}

	public void afterAdviceOne() {
		this.collaborator.afterAdviceOne(this.name);
	}

	public void afterAdviceTwo() {
		this.collaborator.afterAdviceTwo(this.name);
	}


	public interface Collaborator {

		void beforeAdviceOne(String beanName);
		void beforeAdviceTwo(String beanName);
		void aroundAdviceOne(String beanName);
		void aroundAdviceTwo(String beanName);
		void afterAdviceOne(String beanName);
		void afterAdviceTwo(String beanName);
	}

}
