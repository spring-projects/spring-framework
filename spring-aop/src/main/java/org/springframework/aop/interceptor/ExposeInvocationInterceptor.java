/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.PriorityOrdered;

/**
 * Interceptor that exposes the current {@link org.aopalliance.intercept.MethodInvocation}
 * as a thread-local object. We occasionally need to do this; for example, when a pointcut
 * (e.g. an AspectJ expression pointcut) needs to know the full invocation context.
 *
 * <p>Don't use this interceptor unless this is really necessary. Target objects should
 * not normally know about Spring AOP, as this creates a dependency on Spring API.
 * Target objects should be plain POJOs as far as possible.
 *
 * <p>If used, this interceptor will normally be the first in the interceptor chain.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class ExposeInvocationInterceptor implements MethodInterceptor, PriorityOrdered, Serializable {

	/** Singleton instance of this class */
	public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

	/**
	 * Singleton advisor for this class. Use in preference to INSTANCE when using
	 * Spring AOP, as it prevents the need to create a new Advisor to wrap the instance.
	 */
	public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

	private static final ThreadLocal<MethodInvocation> invocation =
			new NamedThreadLocal<MethodInvocation>("Current AOP method invocation");


	/**
	 * Return the AOP Alliance MethodInvocation object associated with the current invocation.
	 * @return the invocation object associated with the current invocation
	 * @throws IllegalStateException if there is no AOP invocation in progress,
	 * or if the ExposeInvocationInterceptor was not added to this interceptor chain
	 */
	public static MethodInvocation currentInvocation() throws IllegalStateException {
		MethodInvocation mi = invocation.get();
		if (mi == null)
			throw new IllegalStateException(
					"No MethodInvocation found: Check that an AOP invocation is in progress, and that the " +
					"ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
					"advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor!");
		return mi;
	}


	/**
	 * Ensures that only the canonical instance can be created.
	 */
	private ExposeInvocationInterceptor() {
	}

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		MethodInvocation oldInvocation = invocation.get();
		invocation.set(mi);
		try {
			return mi.proceed();
		}
		finally {
			invocation.set(oldInvocation);
		}
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * Required to support serialization. Replaces with canonical instance
	 * on deserialization, protecting Singleton pattern.
	 * <p>Alternative to overriding the {@code equals} method.
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
