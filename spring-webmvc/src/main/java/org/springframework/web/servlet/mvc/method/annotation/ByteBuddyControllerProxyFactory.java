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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.EmptyTargetSource;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.ControllerMethodInvocationInterceptor;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.MethodInvocationInfo;

public class ByteBuddyControllerProxyFactory implements ControllerMethodInvocationInterceptor {

	private static final Log logger = LogFactory.getLog(MvcUriComponentsBuilder.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final TypeCache<String> cache = new TypeCache<>(TypeCache.Sort.SOFT);

	private static final ClassLoadingStrategy<ClassLoader> STRATEGY = ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
			? ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())
			: ClassLoadingStrategy.Default.INJECTION;

	private static final String FIELD_NAME = "$$factory";

	private final Class<?> controllerType;

	@Nullable
	private Method controllerMethod;

	@Nullable
	private Object[] argumentValues;

	ByteBuddyControllerProxyFactory(Class<?> controllerType) {
		this.controllerType = controllerType;
	}

	@Nullable
	@RuntimeType
	public static Object intercept(@This Object obj,
			@Origin Method method,
			@AllArguments Object[] args,
			@FieldValue(FIELD_NAME) ByteBuddyControllerProxyFactory factory) {
		switch (method.getName()) {
			case "getControllerType":
				return factory.controllerType;
			case "getControllerMethod":
				return factory.controllerMethod;
			case "getArgumentValues":
				return factory.argumentValues;
		}
		if (ReflectionUtils.isObjectMethod(method)) {
			return ReflectionUtils.invokeMethod(method, obj, args);
		}
		else {
			factory.controllerMethod = method;
			factory.argumentValues = args;
			Class<?> returnType = method.getReturnType();
			try {
				return (returnType == void.class ? null : returnType.cast(initProxy(returnType, factory)));
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
		return intercept(inv.getThis(), inv.getMethod(), inv.getArguments(), this);
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
	@IgnoreForBinding
	static <T> T initProxy(Class<?> controllerType, @Nullable ByteBuddyControllerProxyFactory interceptor) {

		if (interceptor == null) {
			interceptor = new ByteBuddyControllerProxyFactory(controllerType);
		}

		if (controllerType == Object.class) {
			return (T) interceptor;
		}

		else if (controllerType.isInterface()) {
			ProxyFactory factory = new ProxyFactory(EmptyTargetSource.INSTANCE);
			factory.addInterface(controllerType);
			factory.addInterface(MethodInvocationInfo.class);
			factory.addAdvice(interceptor);
			return (T) factory.getProxy();
		}

		else {
			Class<?> proxyClass = cache.findOrInsert(controllerType.getClassLoader(), controllerType.getName(), () -> new ByteBuddy()
					.with(TypeValidation.DISABLED)
					.with(new AuxiliaryType.NamingStrategy.SuffixingRandom(ClassUtils.BYTE_BUDDY_CLASS_INFIX))
					.subclass(controllerType)
					.defineField(FIELD_NAME, ByteBuddyControllerProxyFactory.class, Visibility.PUBLIC)
					.implement(MethodInvocationInfo.class)
					.method(any()).intercept(MethodDelegation.to(ByteBuddyControllerProxyFactory.class))
					.make()
					.load(controllerType.getClassLoader(), STRATEGY)
					.getLoaded());

			Object proxy = null;

			if (objenesis.isWorthTrying()) {
				try {
					proxy = objenesis.newInstance(proxyClass, true);
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

			try {
				proxyClass.getField(FIELD_NAME).set(proxy, interceptor);
			}
			catch (Throwable ex) {
				throw new IllegalStateException(
						"Failed to set proxy dispatcher", ex);
			}

			return (T) proxy;
		}
	}
}
