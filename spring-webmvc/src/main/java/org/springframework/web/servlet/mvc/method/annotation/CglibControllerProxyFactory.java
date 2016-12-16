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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.cglib.core.SpringNamingPolicy;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.ControllerMethodInvocationInterceptor;

public class CglibControllerProxyFactory implements ControllerMethodInvocationInterceptor, MethodInterceptor {

	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private final Class<?> controllerType;

	@Nullable
	private Method controllerMethod;

	@Nullable
	private Object[] argumentValues;

	CglibControllerProxyFactory(Class<?> controllerType) {
		this.controllerType = controllerType;
	}

	@Override
	@Nullable
	public Object intercept(Object obj, Method method, Object[] args, @Nullable MethodProxy proxy) {
		switch (method.getName()) {
			case "getControllerType":
				return this.controllerType;
			case "getControllerMethod":
				return this.controllerMethod;
			case "getArgumentValues":
				return this.argumentValues;
		}
		if (ReflectionUtils.isObjectMethod(method)) {
			return ReflectionUtils.invokeMethod(method, obj, args);
		}
		else {
			this.controllerMethod = method;
			this.argumentValues = args;
			Class<?> returnType = method.getReturnType();
			try {
				return (returnType == void.class ? null : returnType.cast(initProxy(returnType, this)));
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"Failed to create proxy for controller method return type: " + method, ex);
			}
		}
	}

	@Override
	@Nullable
	public Object invoke(org.aopalliance.intercept.MethodInvocation inv) {
		return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), null);
	}

	@Override
	public Class<?> getControllerType() {
		return this.controllerType;
	}

	@Override
	public Method getControllerMethod() {
		Assert.state(this.controllerMethod != null, "Not initialized yet");
		return this.controllerMethod;
	}

	@Override
	public Object[] getArgumentValues() {
		Assert.state(this.argumentValues != null, "Not initialized yet");
		return this.argumentValues;
	}

	@SuppressWarnings("unchecked")
	static <T> T initProxy(Class<?> controllerType, @Nullable CglibControllerProxyFactory interceptor) {

		if (interceptor == null) {
			interceptor = new CglibControllerProxyFactory(controllerType);
		}

		if (controllerType == Object.class) {
			return (T) interceptor;
		}

		else if (controllerType.isInterface()) {
			ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
			factory.addInterface(controllerType);
			factory.addInterface(MvcUriComponentsBuilder.MethodInvocationInfo.class);
			factory.addAdvice(interceptor);
			return (T) factory.getProxy();
		}

		else {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(controllerType);
			enhancer.setInterfaces(new Class<?>[] {MvcUriComponentsBuilder.MethodInvocationInfo.class});
			enhancer.setNamingPolicy(SpringNamingPolicy.INSTANCE);
			enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

			Class<?> proxyClass = enhancer.createClass();
			Object proxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					proxy = objenesis.newInstance(proxyClass, enhancer.getUseCache());
				}
				catch (ObjenesisException ex) {
					logger.debug("Failed to create controller proxy, falling back on default constructor", ex);
				}
			}

			if (proxy == null) {
				try {
					proxy = ReflectionUtils.accessibleConstructor(proxyClass).newInstance();
				}
				catch (Throwable ex) {
					throw new IllegalStateException(
							"Failed to create controller proxy or use default constructor", ex);
				}
			}

			((Factory) proxy).setCallbacks(new Callback[] {interceptor});
			return (T) proxy;
		}
	}
}
