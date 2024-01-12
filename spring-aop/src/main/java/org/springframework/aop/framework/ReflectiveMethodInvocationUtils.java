/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.aop.framework;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;

/**
 * Utility methods for {@link ReflectiveMethodInvocation}.
 *
 * @author Injae Kim
 * @since 6.2
 */
public abstract class ReflectiveMethodInvocationUtils {

	/**
	 * Invoke default method in {@link ReflectiveMethodInvocation}.
	 * @return {@code null} if method is not default method or invocation is not {@link ReflectiveMethodInvocation}
	 */
	@Nullable
	public static Object invokeDefaultMethod(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (method.isDefault() && invocation instanceof ReflectiveMethodInvocation reflectiveMethodInvocation) {
			Object proxy = reflectiveMethodInvocation.getProxy();
			return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
		}
		return null;
	}

}
