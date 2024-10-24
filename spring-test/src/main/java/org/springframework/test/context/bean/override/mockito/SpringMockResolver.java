/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.test.context.bean.override.mockito;

import org.mockito.plugins.MockResolver;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * A {@link MockResolver} for testing Spring applications with Mockito.
 *
 * <p>Resolves mocks by walking the Spring AOP proxy chain until the target or a
 * non-static proxy is found.
 *
 * @author Sam Brannen
 * @author Andy Wilkinson
 * @author Juergen Hoeller
 * @since 6.2
 */
public class SpringMockResolver implements MockResolver {

	static final boolean springAopPresent = ClassUtils.isPresent(
			"org.springframework.aop.framework.Advised", SpringMockResolver.class.getClassLoader());


	@Override
	public Object resolve(Object instance) {
		if (springAopPresent) {
			return getUltimateTargetObject(instance);
		}
		return instance;
	}

	/**
	 * This is a modified version of
	 * {@link org.springframework.test.util.AopTestUtils#getUltimateTargetObject(Object)
	 * AopTestUtils#getUltimateTargetObject()} which only checks static target sources.
	 * @param candidate the instance to check (potentially a Spring AOP proxy;
	 * never {@code null})
	 * @return the target object or the {@code candidate} (never {@code null})
	 * @throws IllegalStateException if an error occurs while unwrapping a proxy
	 * @see Advised#getTargetSource()
	 * @see TargetSource#isStatic()
	 */
	static Object getUltimateTargetObject(Object candidate) {
		Assert.notNull(candidate, "Candidate must not be null");
		try {
			if (AopUtils.isAopProxy(candidate) && candidate instanceof Advised advised) {
				TargetSource targetSource = advised.getTargetSource();
				if (targetSource.isStatic()) {
					Object target = targetSource.getTarget();
					if (target != null) {
						return getUltimateTargetObject(target);
					}
				}
			}
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Failed to unwrap proxied object", ex);
		}
		return candidate;
	}

}
