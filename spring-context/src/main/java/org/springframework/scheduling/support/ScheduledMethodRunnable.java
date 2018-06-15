/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.scheduling.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;

import org.springframework.util.ReflectionUtils;

/**
 * Variant of {@link MethodInvokingRunnable} meant to be used for processing
 * of no-arg scheduled methods. Propagates user exceptions to the caller,
 * assuming that an error strategy for Runnables is in place.
 *
 * @author Juergen Hoeller
 * @since 3.0.6
 * @see org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 */
public class ScheduledMethodRunnable implements Runnable {

	private final Object target;

	private final Method method;


	public ScheduledMethodRunnable(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	public ScheduledMethodRunnable(Object target, String methodName) throws NoSuchMethodException {
		this.target = target;
		this.method = target.getClass().getMethod(methodName);
	}


	public Object getTarget() {
		return this.target;
	}

	public Method getMethod() {
		return this.method;
	}


	@Override
	public void run() {
		try {
			ReflectionUtils.makeAccessible(this.method);
			this.method.invoke(this.target);
		}
		catch (InvocationTargetException ex) {
			ReflectionUtils.rethrowRuntimeException(ex.getTargetException());
		}
		catch (IllegalAccessException ex) {
			throw new UndeclaredThrowableException(ex);
		}
	}

	@Override
	public String toString() {
		return this.method.getDeclaringClass().getName() + "." + this.method.getName();
	}

}
