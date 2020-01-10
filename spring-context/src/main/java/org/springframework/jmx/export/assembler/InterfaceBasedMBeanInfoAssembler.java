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

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Subclass of {@code AbstractReflectiveMBeanInfoAssembler} that allows for
 * the management interface of a bean to be defined using arbitrary interfaces.
 * Any methods or properties that are defined in those interfaces are exposed
 * as MBean operations and attributes.
 *
 * <p>By default, this class votes on the inclusion of each operation or attribute
 * based on the interfaces implemented by the bean class. However, you can supply an
 * array of interfaces via the {@code managedInterfaces} property that will be
 * used instead. If you have multiple beans and you wish each bean to use a different
 * set of interfaces, then you can map bean keys (that is the name used to pass the
 * bean to the {@code MBeanExporter}) to a list of interface names using the
 * {@code interfaceMappings} property.
 *
 * <p>If you specify values for both {@code interfaceMappings} and
 * {@code managedInterfaces}, Spring will attempt to find interfaces in the
 * mappings first. If no interfaces for the bean are found, it will use the
 * interfaces defined by {@code managedInterfaces}.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setManagedInterfaces
 * @see #setInterfaceMappings
 * @see MethodNameBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class InterfaceBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler
		implements BeanClassLoaderAware, InitializingBean {

	@Nullable
	private Class<?>[] managedInterfaces;

	/** Mappings of bean keys to an array of classes */
	@Nullable
	private Properties interfaceMappings;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** Mappings of bean keys to an array of classes */
	@Nullable
	private Map<String, Class<?>[]> resolvedInterfaceMappings;


	/**
	 * Set the array of interfaces to use for creating the management info.
	 * These interfaces will be used for a bean if no entry corresponding to
	 * that bean is found in the {@code interfaceMappings} property.
	 * @param managedInterfaces an array of classes indicating the interfaces to use.
	 * Each entry <strong>MUST</strong> be an interface.
	 * @see #setInterfaceMappings
	 */
	public void setManagedInterfaces(@Nullable Class<?>... managedInterfaces) {
		if (managedInterfaces != null) {
			for (Class<?> ifc : managedInterfaces) {
				if (!ifc.isInterface()) {
					throw new IllegalArgumentException(
							"Management interface [" + ifc.getName() + "] is not an interface");
				}
			}
		}
		this.managedInterfaces = managedInterfaces;
	}

	/**
	 * Set the mappings of bean keys to a comma-separated list of interface names.
	 * <p>The property key should match the bean key and the property value should match
	 * the list of interface names. When searching for interfaces for a bean, Spring
	 * will check these mappings first.
	 * @param mappings the mappings of bean keys to interface names
	 */
	public void setInterfaceMappings(@Nullable Properties mappings) {
		this.interfaceMappings = mappings;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.interfaceMappings != null) {
			this.resolvedInterfaceMappings = resolveInterfaceMappings(this.interfaceMappings);
		}
	}

	/**
	 * Resolve the given interface mappings, turning class names into Class objects.
	 * @param mappings the specified interface mappings
	 * @return the resolved interface mappings (with Class objects as values)
	 */
	private Map<String, Class<?>[]> resolveInterfaceMappings(Properties mappings) {
		Map<String, Class<?>[]> resolvedMappings = new HashMap<>(mappings.size());
		for (Enumeration<?> en = mappings.propertyNames(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] classNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			Class<?>[] classes = resolveClassNames(classNames, beanKey);
			resolvedMappings.put(beanKey, classes);
		}
		return resolvedMappings;
	}

	/**
	 * Resolve the given class names into Class objects.
	 * @param classNames the class names to resolve
	 * @param beanKey the bean key that the class names are associated with
	 * @return the resolved Class
	 */
	private Class<?>[] resolveClassNames(String[] classNames, String beanKey) {
		Class<?>[] classes = new Class<?>[classNames.length];
		for (int x = 0; x < classes.length; x++) {
			Class<?> cls = ClassUtils.resolveClassName(classNames[x].trim(), this.beanClassLoader);
			if (!cls.isInterface()) {
				throw new IllegalArgumentException(
						"Class [" + classNames[x] + "] mapped to bean key [" + beanKey + "] is no interface");
			}
			classes[x] = cls;
		}
		return classes;
	}


	/**
	 * Check to see if the {@code Method} is declared in
	 * one of the configured interfaces and that it is public.
	 * @param method the accessor {@code Method}.
	 * @param beanKey the key associated with the MBean in the
	 * {@code beans} {@code Map}.
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces, otherwise {@code false}.
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * Check to see if the {@code Method} is declared in
	 * one of the configured interfaces and that it is public.
	 * @param method the mutator {@code Method}.
	 * @param beanKey the key associated with the MBean in the
	 * {@code beans} {@code Map}.
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces, otherwise {@code false}.
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * Check to see if the {@code Method} is declared in
	 * one of the configured interfaces and that it is public.
	 * @param method the operation {@code Method}.
	 * @param beanKey the key associated with the MBean in the
	 * {@code beans} {@code Map}.
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces, otherwise {@code false}.
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * Check to see if the {@code Method} is both public and declared in
	 * one of the configured interfaces.
	 * @param method the {@code Method} to check.
	 * @param beanKey the key associated with the MBean in the beans map
	 * @return {@code true} if the {@code Method} is declared in one of the
	 * configured interfaces and is public, otherwise {@code false}.
	 */
	private boolean isPublicInInterface(Method method, String beanKey) {
		return Modifier.isPublic(method.getModifiers()) && isDeclaredInInterface(method, beanKey);
	}

	/**
	 * Checks to see if the given method is declared in a managed
	 * interface for the given bean.
	 */
	private boolean isDeclaredInInterface(Method method, String beanKey) {
		Class<?>[] ifaces = null;

		if (this.resolvedInterfaceMappings != null) {
			ifaces = this.resolvedInterfaceMappings.get(beanKey);
		}

		if (ifaces == null) {
			ifaces = this.managedInterfaces;
			if (ifaces == null) {
				ifaces = ClassUtils.getAllInterfacesForClass(method.getDeclaringClass());
			}
		}

		for (Class<?> ifc : ifaces) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (ifcMethod.getName().equals(method.getName()) &&
						ifcMethod.getParameterCount() == method.getParameterCount() &&
						Arrays.equals(ifcMethod.getParameterTypes(), method.getParameterTypes())) {
					return true;
				}
			}
		}

		return false;
	}

}
