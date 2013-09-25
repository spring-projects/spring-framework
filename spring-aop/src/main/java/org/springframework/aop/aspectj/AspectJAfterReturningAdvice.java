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

package org.springframework.aop.aspectj;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.springframework.aop.AfterAdvice;
import org.springframework.aop.AfterReturningAdvice;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;

/**
 * Spring AOP advice wrapping an AspectJ after-returning advice method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @since 2.0
 */
public class AspectJAfterReturningAdvice extends AbstractAspectJAdvice implements AfterReturningAdvice, AfterAdvice {

	public AspectJAfterReturningAdvice(
			Method aspectJBeforeAdviceMethod, AspectJExpressionPointcut pointcut, AspectInstanceFactory aif) {

		super(aspectJBeforeAdviceMethod, pointcut, aif);
	}

	@Override
	public boolean isBeforeAdvice() {
		return false;
	}

	@Override
	public boolean isAfterAdvice() {
		return true;
	}

	@Override
	public void setReturningName(String name) {
		setReturningNameNoCheck(name);
	}

	@Override
	public void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
		if (shouldInvokeOnReturnValueOf(method, returnValue)) {
			invokeAdviceMethod(getJoinPointMatch(), returnValue, null);
		}
	}

	/**
	 * Following AspectJ semantics, if a returning clause was specified, then the
	 * advice is only invoked if the returned value is an instance of the given
	 * returning type and generic type parameters, if any, match the assignment
	 * rules. If the returning type is Object, the advice is *always* invoked.
	 * @param returnValue the return value of the target method
	 * @return whether to invoke the advice method for the given return value
	 */
	private boolean shouldInvokeOnReturnValueOf(Method method, Object returnValue) {
		Class<?> type = getDiscoveredReturningType();
		Type genericType = getDiscoveredReturningGenericType();
		// If we aren't dealing with a raw type, check if generic parameters are assignable.
		return (matchesReturnValue(type, method, returnValue) &&
				(genericType == null || genericType == type ||
						TypeUtils.isAssignable(genericType, method.getGenericReturnType())));
	}

	/**
	 * Following AspectJ semantics, if a return value is null (or return type is void),
	 * then the return type of target method should be used to determine whether advice
	 * is invoked or not. Also, even if the return type is void, if the type of argument
	 * declared in the advice method is Object, then the advice must still get invoked.
	 * @param type the type of argument declared in advice method
	 * @param method the advice method
	 * @param returnValue the return value of the target method
	 * @return whether to invoke the advice method for the given return value and type
	 */
	private boolean matchesReturnValue(Class<?> type, Method method, Object returnValue) {
		if (returnValue != null) {
			return ClassUtils.isAssignableValue(type, returnValue);
		}
		else if (type.equals(Object.class) && method.getReturnType().equals(void.class)) {
			return true;
		}
		else{
			return ClassUtils.isAssignable(type, method.getReturnType());
		}
	}

}
