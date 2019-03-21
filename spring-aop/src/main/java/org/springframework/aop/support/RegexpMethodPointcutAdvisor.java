/*
 * Copyright 2002-2014 the original author or authors.
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

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.util.ObjectUtils;

/**
 * Convenient class for regexp method pointcuts that hold an Advice,
 * making them an {@link org.springframework.aop.Advisor}.
 *
 * <p>Configure this class using the "pattern" and "patterns"
 * pass-through properties. These are analogous to the pattern
 * and patterns properties of {@link AbstractRegexpMethodPointcut}.
 *
 * <p>Can delegate to any {@link AbstractRegexpMethodPointcut} subclass.
 * By default, {@link JdkRegexpMethodPointcut} will be used. To choose
 * a specific one, override the {@link #createPointcut} method.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPattern
 * @see #setPatterns
 * @see JdkRegexpMethodPointcut
 */
@SuppressWarnings("serial")
public class RegexpMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private String[] patterns;

	private AbstractRegexpMethodPointcut pointcut;

	private final Object pointcutMonitor = new SerializableMonitor();


	/**
	 * Create an empty RegexpMethodPointcutAdvisor.
	 * @see #setPattern
	 * @see #setPatterns
	 * @see #setAdvice
	 */
	public RegexpMethodPointcutAdvisor() {
	}

	/**
	 * Create a RegexpMethodPointcutAdvisor for the given advice.
	 * The pattern still needs to be specified afterwards.
	 * @param advice the advice to use
	 * @see #setPattern
	 * @see #setPatterns
	 */
	public RegexpMethodPointcutAdvisor(Advice advice) {
		setAdvice(advice);
	}

	/**
	 * Create a RegexpMethodPointcutAdvisor for the given advice.
	 * @param pattern the pattern to use
	 * @param advice the advice to use
	 */
	public RegexpMethodPointcutAdvisor(String pattern, Advice advice) {
		setPattern(pattern);
		setAdvice(advice);
	}

	/**
	 * Create a RegexpMethodPointcutAdvisor for the given advice.
	 * @param patterns the patterns to use
	 * @param advice the advice to use
	 */
	public RegexpMethodPointcutAdvisor(String[] patterns, Advice advice) {
		setPatterns(patterns);
		setAdvice(advice);
	}


	/**
	 * Set the regular expression defining methods to match.
	 * <p>Use either this method or {@link #setPatterns}, not both.
	 * @see #setPatterns
	 */
	public void setPattern(String pattern) {
		setPatterns(pattern);
	}

	/**
	 * Set the regular expressions defining methods to match.
	 * To be passed through to the pointcut implementation.
	 * <p>Matching will be the union of all these; if any of the
	 * patterns matches, the pointcut matches.
	 * @see AbstractRegexpMethodPointcut#setPatterns
	 */
	public void setPatterns(String... patterns) {
		this.patterns = patterns;
	}


	/**
	 * Initialize the singleton Pointcut held within this Advisor.
	 */
	@Override
	public Pointcut getPointcut() {
		synchronized (this.pointcutMonitor) {
			if (this.pointcut == null) {
				this.pointcut = createPointcut();
				this.pointcut.setPatterns(this.patterns);
			}
			return pointcut;
		}
	}

	/**
	 * Create the actual pointcut: By default, a {@link JdkRegexpMethodPointcut}
	 * will be used.
	 * @return the Pointcut instance (never {@code null})
	 */
	protected AbstractRegexpMethodPointcut createPointcut() {
		return new JdkRegexpMethodPointcut();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": advice [" + getAdvice() +
				"], pointcut patterns " + ObjectUtils.nullSafeToString(this.patterns);
	}


	/**
	 * Empty class used for a serializable monitor object.
	 */
	private static class SerializableMonitor implements Serializable {
	}

}
