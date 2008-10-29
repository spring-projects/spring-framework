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

package org.springframework.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author Dmitriy Kopylenko
 */
final class SimpleBeforeAdviceInterceptor implements MethodInterceptor {
	
	private SimpleBeforeAdvice advice;
	
	public SimpleBeforeAdviceInterceptor(SimpleBeforeAdvice advice) {
		this.advice = advice;
	}

	public Object invoke(MethodInvocation mi) throws Throwable {
		advice.before();
		return mi.proceed();
	}
	
}
