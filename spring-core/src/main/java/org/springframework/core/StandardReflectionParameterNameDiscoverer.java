/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jspecify.annotations.Nullable;

/**
 * {@link ParameterNameDiscoverer} implementation which uses JDK 8's reflection facilities
 * for introspecting parameter names (based on the "-parameters" compiler flag).
 *
 * <p>This is a key element of {@link DefaultParameterNameDiscoverer} where it is being
 * combined with {@link KotlinReflectionParameterNameDiscoverer} if Kotlin is present.
 *
 * @author Juergen Hoeller
 * @since 4.0
 * @see java.lang.reflect.Method#getParameters()
 * @see java.lang.reflect.Parameter#getName()
 * @see KotlinReflectionParameterNameDiscoverer
 * @see DefaultParameterNameDiscoverer
 */
public class StandardReflectionParameterNameDiscoverer implements ParameterNameDiscoverer {

	@Override
	public @Nullable String @Nullable [] getParameterNames(Method method) {
		return getParameterNames(method.getParameters());
	}

	@Override
	public @Nullable String @Nullable [] getParameterNames(Constructor<?> ctor) {
		return getParameterNames(ctor.getParameters());
	}

	private String @Nullable [] getParameterNames(Parameter[] parameters) {
		String[] parameterNames = new String[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			Parameter param = parameters[i];
			if (!param.isNamePresent()) {
				return null;
			}
			parameterNames[i] = param.getName();
		}
		return parameterNames;
	}

}
