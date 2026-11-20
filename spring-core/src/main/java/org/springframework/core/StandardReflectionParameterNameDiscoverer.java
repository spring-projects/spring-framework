/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jspecify.annotations.Nullable;

/**
 * {@link ParameterNameDiscoverer} implementation which uses Java's reflection facilities
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
