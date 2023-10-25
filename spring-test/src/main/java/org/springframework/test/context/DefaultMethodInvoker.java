/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Default implementation of the {@link MethodInvoker} API.
 *
 * <p>This implementation never provides arguments to a {@link Method}.
 *
 * @author Sam Brannen
 * @since 6.1
 */
final class DefaultMethodInvoker implements MethodInvoker {

	private static final Log logger = LogFactory.getLog(DefaultMethodInvoker.class);


	@Override
	@Nullable
	public Object invoke(Method method, @Nullable Object target) throws Exception {
		Assert.notNull(method, "Method must not be null");

		try {
			ReflectionUtils.makeAccessible(method);
			return method.invoke(target);
		}
		catch (InvocationTargetException ex) {
			if (logger.isErrorEnabled()) {
				logger.error("Exception encountered while invoking method [%s] on target [%s]"
						.formatted(method, target), ex.getTargetException());
			}
			ReflectionUtils.rethrowException(ex.getTargetException());
			// appease the compiler
			return null;
		}
	}

}
