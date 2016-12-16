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

package org.springframework.web.testfixture.method;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Supplier;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.NamingStrategy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.FieldValue;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.objenesis.ObjenesisException;
import org.springframework.objenesis.SpringObjenesis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import static net.bytebuddy.matcher.ElementMatchers.any;

public class ByteBuddyMethodProxy {

	private static final String METHOD_INTERCEPTOR_FIELD = "$$methodInterceptor";

	private static final Log logger = LogFactory.getLog(CglibMethodProxy.class);

	private static final SpringObjenesis objenesis = new SpringObjenesis();

	private static final TypeCache<String> cache = new TypeCache<>(TypeCache.Sort.SOFT);

	private static final ClassLoadingStrategy<ClassLoader> STRATEGY = ClassFileVersion.ofThisVm().isAtLeast(ClassFileVersion.JAVA_V9)
			? ClassLoadingStrategy.UsingLookup.of(MethodHandles.lookup())
			: ClassLoadingStrategy.Default.INJECTION;

	public Object createProxy(Class<?> type, MethodInterceptor interceptor) {
		Class<?> proxyClass = cache.findOrInsert(type.getClassLoader(), type.getName(), () -> new ByteBuddy()
				.with(TypeValidation.DISABLED)
				.with(new NamingStrategy.SuffixingRandom(ClassUtils.BYTE_BUDDY_CLASS_INFIX))
				.subclass(type)
				.implement(Supplier.class)
				.defineField(METHOD_INTERCEPTOR_FIELD, MethodInterceptor.class, Visibility.PUBLIC)
				.method(any())
				.intercept(MethodDelegation.to(ByteBuddyMethodProxy.class))
				.make()
				.load(type.getClassLoader(), STRATEGY)
				.getLoaded());

		Object proxy = null;

		if (objenesis.isWorthTrying()) {
			try {
				proxy = objenesis.newInstance(proxyClass, true);
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

		try {
			Field field = ReflectionUtils.findField(proxy.getClass(), METHOD_INTERCEPTOR_FIELD);
			Assert.state(field != null, "Unable to find generated MethodInterceptor field");
			field.set(proxy, interceptor);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException("Could bot set MethodInterceptor field", ex);
		}

		return proxy;
	}

	@Nullable
	@RuntimeType
	public static Object intercept(
			@This Object object,
			@Origin Method method,
			@AllArguments Object[] args,
			@FieldValue(METHOD_INTERCEPTOR_FIELD) MethodInterceptor interceptor) throws Throwable {
		return interceptor.invoke(new ReflectiveMethodInvocation(
				object, null, method, args, null, Collections.emptyList()));
	}
}
