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

package org.springframework.aop.aspectj.autoproxy.benchmark;

import java.lang.reflect.Method;

import org.springframework.aop.Advisor;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

/**
 * 
 * 
 * @author Rod Johnson
 *
 */
public class TraceAfterReturningAdvice implements AfterReturningAdvice {
	
	public int afterTakesInt;
	
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		++afterTakesInt;
	}
	
	public static Advisor advisor() {
		return new DefaultPointcutAdvisor(
			new StaticMethodMatcherPointcut() {
				public boolean matches(Method method, Class targetClass) {
					return method.getParameterTypes().length == 1 &&
						method.getParameterTypes()[0].equals(Integer.class);
				}
			},
			new TraceAfterReturningAdvice());
	}

}
