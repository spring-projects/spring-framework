/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.method.support;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * {@link MethodParameter} utility methods.
 *
 * @author Matt Benson
 * @since 4.3.0
 */
public abstract class MethodParameterUtils {

	private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER =
			new DefaultParameterNameDiscoverer();


	private MethodParameterUtils() {
	}

	/**
	 * Return an {@link Iterable} representing the sequence of {@link MethodParameter}
	 * instances from the original argument up the method override hierarchy.
	 *
	 * @param parameter to expand
	 * @return {@link Iterable} of {@link MethodParameter}
	 */
	public static Iterable<MethodParameter> parameterHierarchy(
			final MethodParameter parameter) {
		final Iterator<Method> wrapped = overriddenMethods(
				parameter.getMethod()).iterator();

		return new Iterable<MethodParameter>() {

			@Override
			public Iterator<MethodParameter> iterator() {
				return new Iterator<MethodParameter>() {

					@Override
					public MethodParameter next() {
						MethodParameter result = new SynthesizingMethodParameter(
								wrapped.next(), parameter.getParameterIndex());
						result.initParameterNameDiscovery(PARAMETER_NAME_DISCOVERER);
						return result;
					}

					@Override
					public boolean hasNext() {
						return wrapped.hasNext();
					}
				};
			}
		};
	}

	private static Iterable<Method> overriddenMethods(final Method m) {
		final Set<Method> result = new LinkedHashSet<Method>();

		ReflectionUtils.doWithMethods(m.getDeclaringClass(), new MethodCallback() {

			@Override
			public void doWith(Method method)
					throws IllegalArgumentException, IllegalAccessException {
				result.add(method);
			}
		}, new MethodFilter() {

			@Override
			public boolean matches(Method method) {
				return method == m
						|| method.getName().equals(m.getName()) && Arrays.equals(
								method.getParameterTypes(), m.getParameterTypes());
			}
		});
		if (!m.getDeclaringClass().isInterface()) {
			for (Iterator<Class<?>> intrfaces = ClassUtils.getAllInterfacesForClassAsSet(
					m.getDeclaringClass()).iterator(); intrfaces.hasNext();) {
				result.add(ReflectionUtils.findMethod(intrfaces.next(), m.getName(),
						m.getParameterTypes()));
			}
			result.remove(null);
		}
		return result;
	}

}
