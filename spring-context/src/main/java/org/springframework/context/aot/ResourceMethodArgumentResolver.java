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

package org.springframework.context.aot;

import java.lang.reflect.Method;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Resolver used to support injection of named beans to methods. Typically used in
 * AOT-processed applications as a targeted alternative to the
 * {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}.
 *
 * <p>When resolving arguments in a native image, the {@link Method} being used
 * must be marked with an {@link ExecutableMode#INTROSPECT introspection} hint
 * so that field annotations can be read. Full {@link ExecutableMode#INVOKE
 * invocation} hints are only required if the
 * {@link #resolveAndInvoke(RegisteredBean, Object)} method of this class is
 * being used (typically to support private methods).
 * @author Stephane Nicoll
 * @since 6.1
 */
public final class ResourceMethodArgumentResolver extends ResourceElementResolver {

	private final String methodName;

	private final Class<?> lookupType;

	private ResourceMethodArgumentResolver(String name, boolean defaultName,
			String methodName, Class<?> lookupType) {
		super(name, defaultName);
		this.methodName = methodName;
		this.lookupType = lookupType;
	}


	/**
	 * Create a new {@link ResourceMethodArgumentResolver} for the specified method
	 * using a resource name that infers from the method name.
	 * @param methodName the method name
	 * @param parameterType the parameter type.
	 * @return a new {@link ResourceMethodArgumentResolver} instance
	 */
	public static ResourceMethodArgumentResolver forMethod(String methodName, Class<?> parameterType) {
		return new ResourceMethodArgumentResolver(defaultResourceName(methodName), true,
				methodName, parameterType);
	}

	/**
	 * Create a new {@link ResourceMethodArgumentResolver} for the specified method
	 * and resource name.
	 * @param methodName the method name
	 * @param parameterType the parameter type
	 * @param resourceName the resource name
	 * @return a new {@link ResourceMethodArgumentResolver} instance
	 */
	public static ResourceMethodArgumentResolver forMethod(String methodName, Class<?> parameterType, String resourceName) {
		return new ResourceMethodArgumentResolver(resourceName, false, methodName, parameterType);
	}

	@Override
	protected DependencyDescriptor createDependencyDescriptor(RegisteredBean bean) {
		return new LookupDependencyDescriptor(getMethod(bean), this.lookupType);
	}

	/**
	 * Resolve the method argument for the specified registered bean and invoke
	 * the method using reflection.
	 * @param registeredBean the registered bean
	 * @param instance the bean instance
	 */
	public void resolveAndInvoke(RegisteredBean registeredBean, Object instance) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(instance, "'instance' must not be null");
		Method method = getMethod(registeredBean);
		Object resolved = resolveValue(registeredBean);
		ReflectionUtils.makeAccessible(method);
		ReflectionUtils.invokeMethod(method, instance, resolved);
	}

	private Method getMethod(RegisteredBean registeredBean) {
		Method method = ReflectionUtils.findMethod(registeredBean.getBeanClass(),
				this.methodName, this.lookupType);
		Assert.notNull(method, () ->
				"Method '%s' with parameter type '%s' declared on %s could not be found.".formatted(
						this.methodName, this.lookupType.getName(),
						registeredBean.getBeanClass().getName()));
		return method;
	}

	private static String defaultResourceName(String methodName) {
		if (methodName.startsWith("set") && methodName.length() > 3) {
			return StringUtils.uncapitalizeAsProperty(methodName.substring(3));
		}
		return methodName;
	}

}
