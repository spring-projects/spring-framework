/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

/**
 * Interface to discover parameter names for methods and constructors.
 *
 * <p>Parameter name discovery is not always possible, but various strategies exist
 * &mdash; for example, using the JDK's reflection facilities for introspecting
 * parameter names (based on the "-parameters" compiler flag), looking for
 * {@code argNames} annotation attributes optionally configured for AspectJ
 * annotated methods, etc.
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
public interface ParameterNameDiscoverer {

	/**
	 * Return parameter names for a method, or {@code null} if they cannot be determined.
	 * <p>Individual entries in the array may be {@code null} if parameter names are only
	 * available for some parameters of the given method but not for others. However,
	 * it is recommended to use stub parameter names instead wherever feasible.
	 * @param method the method to find parameter names for
	 * @return an array of parameter names if the names can be resolved,
	 * or {@code null} if they cannot
	 */
	@Nullable String @Nullable [] getParameterNames(Method method);

	/**
	 * Return parameter names for a constructor, or {@code null} if they cannot be determined.
	 * <p>Individual entries in the array may be {@code null} if parameter names are only
	 * available for some parameters of the given constructor but not for others. However,
	 * it is recommended to use stub parameter names instead wherever feasible.
	 * @param ctor the constructor to find parameter names for
	 * @return an array of parameter names if the names can be resolved,
	 * or {@code null} if they cannot
	 */
	@Nullable String @Nullable [] getParameterNames(Constructor<?> ctor);

}
