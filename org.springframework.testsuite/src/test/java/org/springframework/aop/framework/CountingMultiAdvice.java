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

package org.springframework.aop.framework;

import java.lang.reflect.Method;

import javax.servlet.ServletException;

import org.springframework.aop.AfterReturningAdvice;
import org.springframework.aop.MethodBeforeAdvice;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.dao.DataAccessException;

/**
 * Advice object that implements <i>multiple</i> Advice interfaces.
 *
 * @author Juergen Hoeller
 * @since 19.05.2005
 */
public class CountingMultiAdvice extends MethodCounter
		implements MethodBeforeAdvice, AfterReturningAdvice, ThrowsAdvice {

	public void before(Method m, Object[] args, Object target) throws Throwable {
		count(m);
	}

	public void afterReturning(Object o, Method m, Object[] args, Object target) throws Throwable {
		count(m);
	}

	public void afterThrowing(ServletException sex) throws Throwable {
		count(ServletException.class.getName());
	}

	public void afterThrowing(DataAccessException ex) throws Throwable {
		count(DataAccessException.class.getName());
	}

}
