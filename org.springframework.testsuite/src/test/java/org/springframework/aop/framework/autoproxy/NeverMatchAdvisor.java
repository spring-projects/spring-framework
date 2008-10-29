/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.aop.interceptor.NopInterceptor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

/**
 * 
 * @author Rod Johnson
 */
public class NeverMatchAdvisor extends StaticMethodMatcherPointcutAdvisor {
	
	public NeverMatchAdvisor() {
		super(new NopInterceptor());
	}
	
	/**
	 * This method is solely to allow us to create a mixture of dependencies in
	 * the bean definitions. The dependencies don't have any meaning, and don't
	 * <b>do</b> anything.
	 */
	public void setDependencies(List l) {
		
	}

	/**
	 * @see org.springframework.aop.MethodMatcher#matches(java.lang.reflect.Method, java.lang.Class)
	 */
	public boolean matches(Method m, Class targetClass) {
		//System.err.println("NeverMAtch test");
		return false;
	}

}
