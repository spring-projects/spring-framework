/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.reflect.Method;

import org.reactivestreams.Publisher;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Package-visible class designed to avoid a hard dependency on ResponseEntity dependency at runtime.
 *
 * @author Ding Hao
 * @since 6.2
 */
abstract class ResponseEntityUtils {

	private static final boolean reactivePresent;

	private static final String RESPONSE_ENTITY_CLASS_NAME = "org.springframework.http.ResponseEntity";

	private static final int RETURN_TYPE_METHOD_PARAMETER_INDEX = -1;

	@Nullable
	private static final Method okMethod;

	static {
		ClassLoader classLoader = ResponseEntityUtils.class.getClassLoader();
		reactivePresent = ClassUtils.isPresent("org.reactivestreams.Publisher", classLoader);
		Method method = null;
		try {
			Class<?> responseEntityClass = ClassUtils.forName(RESPONSE_ENTITY_CLASS_NAME, classLoader);
			method = ReflectionUtils.findMethod(responseEntityClass, "ok", Object.class);
		}
		catch (ClassNotFoundException ignored) {

		}
		okMethod = method;
	}

	private static boolean hasResponseEntityReturnType(Method method) {
		return RESPONSE_ENTITY_CLASS_NAME
				.equals(new MethodParameter(method, RETURN_TYPE_METHOD_PARAMETER_INDEX).getParameterType().getName());
	}

	private static boolean isReactiveValue(@Nullable Object returnValue) {
		return reactivePresent && returnValue instanceof Publisher<?>;
	}

	@Nullable
	static Object adaptReturnValue(Method method, @Nullable Object returnValue) {
		if (isReactiveValue(returnValue) && hasResponseEntityReturnType(method)) {
			return ReflectionUtils.invokeMethod(okMethod, null, returnValue);
		}
		return returnValue;
	}

}
