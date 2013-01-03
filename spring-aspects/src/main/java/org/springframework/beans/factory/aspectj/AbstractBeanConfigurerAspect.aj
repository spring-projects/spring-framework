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

package org.springframework.beans.factory.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.springframework.beans.factory.wiring.BeanConfigurerSupport;

/**
 * Abstract superaspect for AspectJ aspects that can perform Dependency
 * Injection on objects, however they may be created. Define the beanCreation()
 * pointcut in subaspects.
 *
 * <p>Subaspects may also need a metadata resolution strategy, in the
 * {@code BeanWiringInfoResolver} interface. The default implementation
 * looks for a bean with the same name as the FQN. This is the default name
 * of a bean in a Spring container if the id value is not supplied explicitly.
 *
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 2.0
 * @deprecated as of Spring 2.5.2.
 * Use AbstractDependencyInjectionAspect or its subaspects instead.
 */
public abstract aspect AbstractBeanConfigurerAspect extends BeanConfigurerSupport {

	/**
	 * Configured bean before initialization.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	before(Object beanInstance) : beanInitialization(beanInstance) {
		if (preConstructionConfiguration(beanInstance)) {
			configureBean(beanInstance);
		}
	}

	/**
	 * Configured bean after construction.
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object beanInstance) returning : beanCreation(beanInstance) {
		if (!preConstructionConfiguration(beanInstance)) {
			configureBean(beanInstance);
		}
	}


	/**
	 * The initialization of a new object.
	 *
	 * <p>WARNING: Although this pointcut is non-abstract for backwards
	 * compatibility reasons, it is meant to be overridden to select
	 * initialization of any configurable bean.
	 */
	protected pointcut beanInitialization(Object beanInstance);

	/**
	 * The creation of a new object.
	 */
	protected abstract pointcut beanCreation(Object beanInstance);


	/**
	 * Are dependencies to be injected prior to the construction of an object?
	 */
	protected boolean preConstructionConfiguration(Object beanInstance) {
		return false; // matches the default in the @Configurable annotation
	}

}
