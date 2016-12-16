/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.handler.invocation;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.ReflectionUtils;

public class CglibMethodProxy {

	private static final Log logger = LogFactory.getLog(CglibMethodProxy.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	public Object createProxy(Class<?> type, MethodInterceptor interceptor) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setInterfaces(new Class<?>[] {Supplier.class});
		enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
		enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

		Class<?> proxyClass = enhancer.createClass();
		Object proxy = null;

		if (objenesis.isWorthTrying()) {
			try {
				proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
			}
			catch (ObjenesisException ex) {
				logger.debug("Objenesis failed, falling back to default constructor", ex);
			}
		}

		if (proxy == null) {
			try {
				proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalStateException("Unable to instantiate proxy " +
						"via both Objenesis and default constructor fails as well", ex);
			}
		}

		((Factory) proxy).setCallbacks(new Callback[] {new MethodInvocationInterceptor(interceptor)});
		return proxy;
	}

	private static final class MethodInvocationInterceptor implements org.springframework.cglib.proxy.MethodInterceptor {

		private final MethodInterceptor interceptor;

		private MethodInvocationInterceptor(MethodInterceptor interceptor) {
			this.interceptor = interceptor;
		}

		@Override
		@Nullable
		public Object intercept(Object object, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			return interceptor.invoke(new ReflectiveMethodInvocation(
					object, null, method, args, null, Collections.emptyList()));
		}
	}

}
