/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.orm.hibernate3.support;

import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.scope.ScopedObject;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ScopedBeanInterceptor}.
 *
 * @author Costin Leau
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class ScopedBeanInterceptorTests {

	private final ScopedBeanInterceptor interceptor = new ScopedBeanInterceptor();

	@Test
	public void interceptorWithPlainObject() throws Exception {
		final Object realObject = new Object();

		ScopedObject scoped = new ScopedObject() {
			@Override
			public Object getTargetObject() {
				return realObject;
			}
			@Override
			public void removeFromScope() {
				// do nothing
			}
		};

		// default contract is to return null for default behavior
		assertNull(interceptor.getEntityName(realObject));
		assertEquals(realObject.getClass().getName(), interceptor.getEntityName(scoped));
	}

	@Test
	public void interceptorWithCglibProxy() throws Exception {
		final Object realObject = new Object();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(realObject);
		proxyFactory.setProxyTargetClass(true);
		final Object proxy = proxyFactory.getProxy();

		ScopedObject scoped = new ScopedObject() {
			@Override
			public Object getTargetObject() {
				return proxy;
			}
			@Override
			public void removeFromScope() {
				// do nothing
			}
		};

		assertEquals(realObject.getClass().getName(), interceptor.getEntityName(scoped));
	}

}
