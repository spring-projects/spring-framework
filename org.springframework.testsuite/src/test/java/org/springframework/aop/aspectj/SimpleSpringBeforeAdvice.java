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
 *
 * Created on 14-Feb-2006 by Adrian Colyer
 */
package org.springframework.aop.aspectj;

import java.lang.reflect.Method;

import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.beans.factory.BeanNameAware;

/**
 * Used as part of aspect precedence tests
 * 
 * @author Adrian Colyer
 * @since 2.0
 */
public class SimpleSpringBeforeAdvice implements MethodBeforeAdvice, BeanNameAware {

	private PrecedenceTestAspect.Collaborator collaborator;
	private String name;
	
	/* (non-Javadoc)
	 * @see org.springframework.aop.MethodBeforeAdvice#before(java.lang.reflect.Method, java.lang.Object[], java.lang.Object)
	 */
	public void before(Method method, Object[] args, Object target)
			throws Throwable {
		this.collaborator.beforeAdviceOne(this.name);
	}

	public void setCollaborator(PrecedenceTestAspect.Collaborator collaborator) {
		this.collaborator = collaborator;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		this.name = name;
	}

}
