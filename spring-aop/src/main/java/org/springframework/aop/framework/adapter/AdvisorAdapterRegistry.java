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

package org.springframework.aop.framework.adapter;

import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;

/**
 * Interface for registries of Advisor adapters.
 *
 * <p><i>This is an SPI interface, not to be implemented by any Spring user.</i>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 */
public interface AdvisorAdapterRegistry {

	/**
	 * Return an Advisor wrapping the given advice.
	 * <p>Should by default at least support
	 * {@link org.aopalliance.intercept.MethodInterceptor},
	 * {@link org.springframework.aop.MethodBeforeAdvice},
	 * {@link org.springframework.aop.AfterReturningAdvice},
	 * {@link org.springframework.aop.ThrowsAdvice}.
	 * @param advice object that should be an advice
	 * @return an Advisor wrapping the given advice. Never returns {@code null}.
	 * If the advice parameter is an Advisor, return it.
	 * @throws UnknownAdviceTypeException if no registered advisor adapter
	 * can wrap the supposed advice
	 */
	Advisor wrap(Object advice) throws UnknownAdviceTypeException;

	/**
	 * Return an array of AOP Alliance MethodInterceptors to allow use of the
	 * given Advisor in an interception-based framework.
	 * <p>Don't worry about the pointcut associated with the Advisor,
	 * if it's a PointcutAdvisor: just return an interceptor.
	 * @param advisor Advisor to find an interceptor for
	 * @return an array of MethodInterceptors to expose this Advisor's behavior
	 * @throws UnknownAdviceTypeException if the Advisor type is
	 * not understood by any registered AdvisorAdapter.
	 */
	MethodInterceptor[] getInterceptors(Advisor advisor) throws UnknownAdviceTypeException;

	/**
	 * Register the given AdvisorAdapter. Note that it is not necessary to register
	 * adapters for an AOP Alliance Interceptors or Spring Advices: these must be
	 * automatically recognized by an AdvisorAdapterRegistry implementation.
	 * @param adapter AdvisorAdapter that understands a particular Advisor
	 * or Advice types
	 */
	void registerAdvisorAdapter(AdvisorAdapter adapter);

}
