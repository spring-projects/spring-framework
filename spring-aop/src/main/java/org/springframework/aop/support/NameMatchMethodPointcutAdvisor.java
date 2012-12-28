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

package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;

/**
 * Convenient class for name-match method pointcuts that hold an Advice,
 * making them an Advisor.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see NameMatchMethodPointcut
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private final NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();


	public NameMatchMethodPointcutAdvisor() {
	}

	public NameMatchMethodPointcutAdvisor(Advice advice) {
		setAdvice(advice);
	}


	/**
	 * Set the {@link ClassFilter} to use for this pointcut.
	 * Default is {@link ClassFilter#TRUE}.
	 * @see NameMatchMethodPointcut#setClassFilter
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	/**
	 * Convenience method when we have only a single method name to match.
	 * Use either this method or {@code setMappedNames}, not both.
	 * @see #setMappedNames
	 * @see NameMatchMethodPointcut#setMappedName
	 */
	public void setMappedName(String mappedName) {
		this.pointcut.setMappedName(mappedName);
	}

	/**
	 * Set the method names defining methods to match.
	 * Matching will be the union of all these; if any match,
	 * the pointcut matches.
	 * @see NameMatchMethodPointcut#setMappedNames
	 */
	public void setMappedNames(String[] mappedNames) {
		this.pointcut.setMappedNames(mappedNames);
	}

	/**
	 * Add another eligible method name, in addition to those already named.
	 * Like the set methods, this method is for use when configuring proxies,
	 * before a proxy is used.
	 * @param name name of the additional method that will match
	 * @return this pointcut to allow for multiple additions in one line
	 * @see NameMatchMethodPointcut#addMethodName
	 */
	public NameMatchMethodPointcut addMethodName(String name) {
		return this.pointcut.addMethodName(name);
	}


	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
