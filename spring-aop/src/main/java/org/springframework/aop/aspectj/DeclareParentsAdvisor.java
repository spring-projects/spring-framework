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

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.support.ClassFilters;
import org.springframework.aop.support.DelegatePerTargetObjectIntroductionInterceptor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;

/**
 * Introduction advisor delegating to the given object.
 * Implements AspectJ annotation-style behavior for the DeclareParents annotation.
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @since 2.0
 */
public class DeclareParentsAdvisor implements IntroductionAdvisor {

	private final Class<?> introducedInterface;

	private final ClassFilter typePatternClassFilter;

	private final Advice advice;


	/**
	 * Create a new advisor for this DeclareParents field.
	 * @param interfaceType static field defining the introduction
	 * @param typePattern type pattern the introduction is restricted to
	 * @param defaultImpl the default implementation class
	 */
	public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Class<?> defaultImpl) {
		this(interfaceType, typePattern, defaultImpl,
			 new DelegatePerTargetObjectIntroductionInterceptor(defaultImpl, interfaceType));
	}

	/**
	 * Create a new advisor for this DeclareParents field.
	 * @param interfaceType static field defining the introduction
	 * @param typePattern type pattern the introduction is restricted to
	 * @param delegateRef the delegate implementation object
	 */
	public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Object delegateRef) {
		this(interfaceType, typePattern, delegateRef.getClass(),
			 new DelegatingIntroductionInterceptor(delegateRef));
	}

	/**
	 * Private constructor to share common code between impl-based delegate and reference-based delegate
	 * (cannot use method such as init() to share common code, due the the use of final fields)
	 * @param interfaceType static field defining the introduction
	 * @param typePattern type pattern the introduction is restricted to
	 * @param implementationClass implementation class
	 * @param advice delegation advice
	 */
	private DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Class<?> implementationClass, Advice advice) {
		this.introducedInterface = interfaceType;
		ClassFilter typePatternFilter = new TypePatternClassFilter(typePattern);

		// Excludes methods implemented.
		ClassFilter exclusion = new ClassFilter() {
			public boolean matches(Class<?> clazz) {
				return !(introducedInterface.isAssignableFrom(clazz));
			}
		};

		this.typePatternClassFilter = ClassFilters.intersection(typePatternFilter, exclusion);
		this.advice = advice;
	}


	public ClassFilter getClassFilter() {
		return this.typePatternClassFilter;
	}

	public void validateInterfaces() throws IllegalArgumentException {
		// Do nothing
	}

	public boolean isPerInstance() {
		return true;
	}

	public Advice getAdvice() {
		return this.advice;
	}

	public Class<?>[] getInterfaces() {
		return new Class[] {this.introducedInterface};
	}

}
