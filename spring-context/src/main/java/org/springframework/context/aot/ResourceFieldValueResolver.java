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

import java.lang.reflect.Field;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Resolver used to support injection of named beans on fields. Typically used in
 * AOT-processed applications as a targeted alternative to the
 * {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor}.
 *
 * <p>When resolving arguments in a native image, the {@link Field} being used must
 * be marked with an {@link ExecutableMode#INTROSPECT introspection} hint so
 * that field annotations can be read. Full {@link ExecutableMode#INVOKE
 * invocation} hints are only required if the
 * {@link #resolveAndSet(RegisteredBean, Object)} method of this class is being
 * used (typically to support private fields).
 *
 * @author Stephane Nicoll
 * @since 6.1
 */
public final class ResourceFieldValueResolver extends ResourceElementResolver {

	private final String fieldName;

	public ResourceFieldValueResolver(String name, boolean defaultName, String fieldName) {
		super(name, defaultName);
		this.fieldName = fieldName;
	}


	/**
	 * Create a new {@link ResourceFieldValueResolver} for the specified field.
	 * @param fieldName the field name
	 * @return a new {@link ResourceFieldValueResolver} instance
	 */
	public static ResourceFieldValueResolver forField(String fieldName) {
		return new ResourceFieldValueResolver(fieldName, true, fieldName);
	}

	/**
	 * Create a new {@link ResourceFieldValueResolver} for the specified field and
	 * resource name.
	 * @param fieldName the field name
	 * @param resourceName the resource name
	 * @return a new {@link ResourceFieldValueResolver} instance
	 */
	public static ResourceFieldValueResolver forField(String fieldName, String resourceName) {
		return new ResourceFieldValueResolver(resourceName, false, fieldName);
	}

	@Override
	protected DependencyDescriptor createDependencyDescriptor(RegisteredBean bean) {
		Field field = getField(bean);
		return new LookupDependencyDescriptor(field, field.getType());
	}

	/**
	 * Resolve the field value for the specified registered bean and set it
	 * using reflection.
	 * @param registeredBean the registered bean
	 * @param instance the bean instance
	 */
	public void resolveAndSet(RegisteredBean registeredBean, Object instance) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(instance, "'instance' must not be null");
		Field field = getField(registeredBean);
		Object resolved = resolveValue(registeredBean);
		ReflectionUtils.makeAccessible(field);
		ReflectionUtils.setField(field, instance, resolved);
	}

	private Field getField(RegisteredBean registeredBean) {
		Field field = ReflectionUtils.findField(registeredBean.getBeanClass(),
				this.fieldName);
		Assert.notNull(field, () -> "No field '" + this.fieldName + "' found on "
				+ registeredBean.getBeanClass().getName());
		return field;
	}

}
