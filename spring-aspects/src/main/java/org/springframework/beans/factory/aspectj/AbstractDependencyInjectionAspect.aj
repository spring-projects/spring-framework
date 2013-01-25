/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;

/**
 * Abstract base aspect that can perform Dependency
 * Injection on objects, however they may be created.
 * 
 * @author Ramnivas Laddad
 * @since 2.5.2
 */
public abstract aspect AbstractDependencyInjectionAspect {
	/**
	 * Select construction join points for objects to inject dependencies
	 */
	public abstract pointcut beanConstruction(Object bean); 

	/**
	 * Select deserialization join points for objects to inject dependencies
	 */
	public abstract pointcut beanDeserialization(Object bean);
	
	/**
	 * Select join points in a configurable bean
	 */
	public abstract pointcut inConfigurableBean();
	
	/**
	 * Select join points in beans to be configured prior to construction?
	 * By default, use post-construction injection matching the default in the Configurable annotation.
	 */
	public pointcut preConstructionConfiguration() : if(false);
	
	/**
	 * Select the most-specific initialization join point 
	 * (most concrete class) for the initialization of an instance.
	 */
	public pointcut mostSpecificSubTypeConstruction() :
		if(thisJoinPoint.getSignature().getDeclaringType() == thisJoinPoint.getThis().getClass());

	/**
	 * Select least specific super type that is marked for DI (so that injection occurs only once with pre-construction inejection
	 */
	public abstract pointcut leastSpecificSuperTypeConstruction();
		
	/**
	 * Configure the bean
	 */
	public abstract void configureBean(Object bean);

	
	private pointcut preConstructionCondition() : 
		leastSpecificSuperTypeConstruction() && preConstructionConfiguration();
	
	private pointcut postConstructionCondition() :
		mostSpecificSubTypeConstruction() && !preConstructionConfiguration();
	
	/**
	 * Pre-construction configuration.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	before(Object bean) : 
		beanConstruction(bean) && preConstructionCondition() && inConfigurableBean()  { 
		configureBean(bean);
	}

	/**
	 * Post-construction configuration.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object bean) returning : 
		beanConstruction(bean) && postConstructionCondition() && inConfigurableBean() {
		configureBean(bean);
	}
	
	/**
	 * Post-deserialization configuration.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object bean) returning : 
		beanDeserialization(bean) && inConfigurableBean() {
		configureBean(bean);
	}
	
}
