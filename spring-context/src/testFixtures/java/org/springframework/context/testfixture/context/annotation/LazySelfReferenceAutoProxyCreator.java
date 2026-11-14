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

package org.springframework.context.testfixture.context.annotation;

import org.aopalliance.intercept.MethodInterceptor;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;

/**
 * Auto-proxy creator that applies a class-based proxy to
 * {@link LazySelfReferenceComponent}.
 *
 * @author Pankraz76
 */
@SuppressWarnings("serial")
public class LazySelfReferenceAutoProxyCreator extends AbstractAutoProxyCreator {

	public LazySelfReferenceAutoProxyCreator() {
		setProxyTargetClass(true);
	}

	@Override
	protected Object @Nullable [] getAdvicesAndAdvisorsForBean(
			Class<?> beanClass, String beanName, @Nullable TargetSource customTargetSource) {

		if (LazySelfReferenceComponent.class.isAssignableFrom(beanClass)) {
			MethodInterceptor interceptor = invocation -> ("greeting".equals(invocation.getMethod().getName()) ?
					invocation.proceed() + " World" : invocation.proceed());
			return new Object[] {interceptor};
		}
		return DO_NOT_PROXY;
	}

}
