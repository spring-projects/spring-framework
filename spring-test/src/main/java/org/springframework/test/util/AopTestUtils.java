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

package org.springframework.test.util;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

/**
 * {@code AopTestUtils} is a collection of AOP-related utility methods for
 * use in unit and integration testing scenarios.
 *
 * <p>For Spring's core AOP utilities, see
 * {@link org.springframework.aop.support.AopUtils AopUtils} and
 * {@link org.springframework.aop.framework.AopProxyUtils AopProxyUtils}.
 *
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @since 4.2
 * @see org.springframework.aop.support.AopUtils
 * @see org.springframework.aop.framework.AopProxyUtils
 * @see ReflectionTestUtils
 * @deprecated in favor of {@link AopUtils}
 */
@Deprecated(since = "6.1", forRemoval = true)
public abstract class AopTestUtils {

	/**
	 * Get the <em>target</em> object of the supplied {@code candidate} object.
	 * <p>If the supplied {@code candidate} is a Spring
	 * {@linkplain AopUtils#isAopProxy proxy}, the target of the proxy will
	 * be returned; otherwise, the {@code candidate} will be returned
	 * <em>as is</em>.
	 * @param <T> the type of the target object
	 * @param candidate the instance to check (potentially a Spring AOP proxy;
	 * never {@code null})
	 * @return the target object or the {@code candidate} (never {@code null})
	 * @throws IllegalStateException if an error occurs while unwrapping a proxy
	 * @see AopUtils#getTargetObject
	 * @see Advised#getTargetSource()
	 * @see #getUltimateTargetObject
	 */
	public static <T> T getTargetObject(Object candidate) {
		return AopUtils.getTargetObject(candidate);
	}

	/**
	 * Get the ultimate <em>target</em> object of the supplied {@code candidate}
	 * object, unwrapping not only a top-level proxy but also any number of
	 * nested proxies.
	 * <p>If the supplied {@code candidate} is a Spring
	 * {@linkplain AopUtils#isAopProxy proxy}, the ultimate target of all
	 * nested proxies will be returned; otherwise, the {@code candidate}
	 * will be returned <em>as is</em>.
	 * <p>NOTE: If the top-level proxy or a nested proxy is not backed by a
	 * {@linkplain org.springframework.aop.TargetSource#isStatic() static}
	 * {@link org.springframework.aop.TargetSource TargetSource}, invocation of
	 * this utility method may result in undesired behavior such as infinite
	 * recursion leading to a {@link StackOverflowError}.
	 * @param <T> the type of the target object
	 * @param candidate the instance to check (potentially a Spring AOP proxy;
	 * never {@code null})
	 * @return the target object or the {@code candidate} (never {@code null})
	 * @throws IllegalStateException if an error occurs while unwrapping a proxy
	 * @see AopUtils#getUltimateTargetObject
	 * @see Advised#getTargetSource()
	 * @see org.springframework.aop.TargetSource#isStatic()
	 * @see org.springframework.aop.framework.AopProxyUtils#ultimateTargetClass
	 */
	public static <T> T getUltimateTargetObject(Object candidate) {
		return AopUtils.getUltimateTargetObject(candidate);
	}

}
