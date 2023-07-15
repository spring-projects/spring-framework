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

package org.springframework.beans.factory.aot;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Resolver used to support the autowiring of fields. Typically used in
 * AOT-processed applications as a targeted alternative to the
 * {@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor
 * AutowiredAnnotationBeanPostProcessor}.
 *
 * <p>When resolving arguments in a native image, the {@link Field} being used must
 * be marked with an {@link ExecutableMode#INTROSPECT introspection} hint so
 * that field annotations can be read. Full {@link ExecutableMode#INVOKE
 * invocation} hints are only required if the
 * {@link #resolveAndSet(RegisteredBean, Object)} method of this class is being
 * used (typically to support private fields).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 */
public final class AutowiredFieldValueResolver extends AutowiredElementResolver {

	private final String fieldName;

	private final boolean required;

	@Nullable
	private final String shortcut;


	private AutowiredFieldValueResolver(String fieldName, boolean required,
			@Nullable String shortcut) {

		Assert.hasText(fieldName, "'fieldName' must not be empty");
		this.fieldName = fieldName;
		this.required = required;
		this.shortcut = shortcut;
	}


	/**
	 * Create a new {@link AutowiredFieldValueResolver} for the specified field
	 * where injection is optional.
	 * @param fieldName the field name
	 * @return a new {@link AutowiredFieldValueResolver} instance
	 */
	public static AutowiredFieldValueResolver forField(String fieldName) {
		return new AutowiredFieldValueResolver(fieldName, false, null);
	}

	/**
	 * Create a new {@link AutowiredFieldValueResolver} for the specified field
	 * where injection is required.
	 * @param fieldName the field name
	 * @return a new {@link AutowiredFieldValueResolver} instance
	 */
	public static AutowiredFieldValueResolver forRequiredField(String fieldName) {
		return new AutowiredFieldValueResolver(fieldName, true, null);
	}


	/**
	 * Return a new {@link AutowiredFieldValueResolver} instance that uses a
	 * direct bean name injection shortcut.
	 * @param beanName the bean name to use as a shortcut
	 * @return a new {@link AutowiredFieldValueResolver} instance that uses the
	 * shortcuts
	 */
	public AutowiredFieldValueResolver withShortcut(String beanName) {
		return new AutowiredFieldValueResolver(this.fieldName, this.required, beanName);
	}

	/**
	 * Resolve the field for the specified registered bean and provide it to the
	 * given action.
	 * @param registeredBean the registered bean
	 * @param action the action to execute with the resolved field value
	 */
	public <T> void resolve(RegisteredBean registeredBean, ThrowingConsumer<T> action) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		Assert.notNull(action, "'action' must not be null");
		T resolved = resolve(registeredBean);
		if (resolved != null) {
			action.accept(resolved);
		}
	}

	/**
	 * Resolve the field value for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @param requiredType the required type
	 * @return the resolved field value
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T resolve(RegisteredBean registeredBean, Class<T> requiredType) {
		Object value = resolveObject(registeredBean);
		Assert.isInstanceOf(requiredType, value);
		return (T) value;
	}

	/**
	 * Resolve the field value for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved field value
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public <T> T resolve(RegisteredBean registeredBean) {
		return (T) resolveObject(registeredBean);
	}

	/**
	 * Resolve the field value for the specified registered bean.
	 * @param registeredBean the registered bean
	 * @return the resolved field value
	 */
	@Nullable
	public Object resolveObject(RegisteredBean registeredBean) {
		Assert.notNull(registeredBean, "'registeredBean' must not be null");
		return resolveValue(registeredBean, getField(registeredBean));
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
		Object resolved = resolveValue(registeredBean, field);
		if (resolved != null) {
			ReflectionUtils.makeAccessible(field);
			ReflectionUtils.setField(field, instance, resolved);
		}
	}

	@Nullable
	private Object resolveValue(RegisteredBean registeredBean, Field field) {
		String beanName = registeredBean.getBeanName();
		Class<?> beanClass = registeredBean.getBeanClass();
		ConfigurableBeanFactory beanFactory = registeredBean.getBeanFactory();
		DependencyDescriptor descriptor = new DependencyDescriptor(field, this.required);
		descriptor.setContainingClass(beanClass);
		if (this.shortcut != null) {
			descriptor = new ShortcutDependencyDescriptor(descriptor, this.shortcut);
		}
		Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
		TypeConverter typeConverter = beanFactory.getTypeConverter();
		try {
			Assert.isInstanceOf(AutowireCapableBeanFactory.class, beanFactory);
			Object value = ((AutowireCapableBeanFactory) beanFactory).resolveDependency(
					descriptor, beanName, autowiredBeanNames, typeConverter);
			registerDependentBeans(beanFactory, beanName, autowiredBeanNames);
			return value;
		}
		catch (BeansException ex) {
			throw new UnsatisfiedDependencyException(null, beanName,
					new InjectionPoint(field), ex);
		}
	}

	private Field getField(RegisteredBean registeredBean) {
		Field field = ReflectionUtils.findField(registeredBean.getBeanClass(),
				this.fieldName);
		Assert.notNull(field, () -> "No field '" + this.fieldName + "' found on "
				+ registeredBean.getBeanClass().getName());
		return field;
	}

}
