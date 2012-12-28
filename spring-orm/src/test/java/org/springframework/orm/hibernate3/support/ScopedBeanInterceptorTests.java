/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.scope.ScopedObject;

/**
 * @author Costin Leau
 */
public class ScopedBeanInterceptorTests extends TestCase {

	public void testInterceptorWithPlainObject() throws Exception {
		ScopedBeanInterceptor interceptor = new ScopedBeanInterceptor();
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
		assertEquals(null, interceptor.getEntityName(realObject));
		assertEquals(realObject.getClass().getName(), interceptor.getEntityName(scoped));
	}

	public void testInterceptorWithCglibProxy() throws Exception {
		ScopedBeanInterceptor interceptor = new ScopedBeanInterceptor();
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
