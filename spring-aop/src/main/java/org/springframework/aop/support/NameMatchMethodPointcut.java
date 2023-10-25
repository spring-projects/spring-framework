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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;

/**
 * Pointcut bean for simple method name matches, as an alternative to regular
 * expression patterns.
 *
 * <p>Each configured method name can be an exact method name or a method name
 * pattern (see {@link #isMatch(String, String)} for details on the supported
 * pattern styles).
 *
 * <p>Does not handle overloaded methods: all methods with a given name will be eligible.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 11.02.2004
 * @see #isMatch
 * @see JdkRegexpMethodPointcut
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcut extends StaticMethodMatcherPointcut implements Serializable {

	private List<String> mappedNamePatterns = new ArrayList<>();


	/**
	 * Convenience method for configuring a single method name pattern.
	 * <p>Use either this method or {@link #setMappedNames(String...)}, but not both.
	 * @see #setMappedNames
	 */
	public void setMappedName(String mappedNamePattern) {
		setMappedNames(mappedNamePattern);
	}

	/**
	 * Set the method name patterns defining methods to match.
	 * <p>Matching will be the union of all these; if any match, the pointcut matches.
	 * @see #setMappedName(String)
	 */
	public void setMappedNames(String... mappedNamePatterns) {
		this.mappedNamePatterns = new ArrayList<>(Arrays.asList(mappedNamePatterns));
	}

	/**
	 * Add another method name pattern, in addition to those already configured.
	 * <p>Like the "set" methods, this method is for use when configuring proxies,
	 * before a proxy is used.
	 * <p><b>NOTE:</b> This method does not work after the proxy is in use, since
	 * advice chains will be cached.
	 * @param mappedNamePattern the additional method name pattern
	 * @return this pointcut to allow for method chaining
	 * @see #setMappedNames(String...)
	 * @see #setMappedName(String)
	 */
	public NameMatchMethodPointcut addMethodName(String mappedNamePattern) {
		this.mappedNamePatterns.add(mappedNamePattern);
		return this;
	}


	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		for (String mappedNamePattern : this.mappedNamePatterns) {
			if (mappedNamePattern.equals(method.getName()) || isMatch(method.getName(), mappedNamePattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine if the given method name matches the mapped name pattern.
	 * <p>The default implementation checks for {@code xxx*}, {@code *xxx},
	 * {@code *xxx*}, and {@code xxx*yyy} matches, as well as direct equality.
	 * <p>Can be overridden in subclasses.
	 * @param methodName the method name to check
	 * @param mappedNamePattern the method name pattern
	 * @return {@code true} if the method name matches the pattern
	 * @see PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedNamePattern) {
		return PatternMatchUtils.simpleMatch(mappedNamePattern, methodName);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof NameMatchMethodPointcut that &&
				this.mappedNamePatterns.equals(that.mappedNamePatterns)));
	}

	@Override
	public int hashCode() {
		return this.mappedNamePatterns.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.mappedNamePatterns;
	}

}
