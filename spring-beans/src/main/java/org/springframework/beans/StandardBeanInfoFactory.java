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

package org.springframework.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;

import org.springframework.core.Ordered;
import org.springframework.core.SpringProperties;
import org.springframework.lang.NonNull;

/**
 * {@link BeanInfoFactory} implementation that performs standard
 * {@link java.beans.Introspector} inspection.
 *
 * <p>To be configured via a {@code META-INF/spring.factories} file with the following content:
 * {@code org.springframework.beans.BeanInfoFactory=org.springframework.beans.StandardBeanInfoFactory}
 *
 * <p>Ordered at {@link Ordered#LOWEST_PRECEDENCE} to allow other user-defined
 * {@link BeanInfoFactory} types to take precedence.
 *
 * @author Juergen Hoeller
 * @since 6.0
 * @see ExtendedBeanInfoFactory
 * @see CachedIntrospectionResults
 * @see Introspector#getBeanInfo(Class)
 */
public class StandardBeanInfoFactory implements BeanInfoFactory, Ordered {

	/**
	 * System property that instructs Spring to use the {@link Introspector#IGNORE_ALL_BEANINFO}
	 * mode when calling the JavaBeans {@link Introspector}: "spring.beaninfo.ignore", with a
	 * value of "true" skipping the search for {@code BeanInfo} classes (typically for scenarios
	 * where no such classes are being defined for beans in the application in the first place).
	 * <p>The default is "false", considering all {@code BeanInfo} metadata classes, like for
	 * standard {@link Introspector#getBeanInfo(Class)} calls. Consider switching this flag to
	 * "true" if you experience repeated ClassLoader access for non-existing {@code BeanInfo}
	 * classes, in case such access is expensive on startup or on lazy loading.
	 * <p>Note that such an effect may also indicate a scenario where caching doesn't work
	 * effectively: Prefer an arrangement where the Spring jars live in the same ClassLoader
	 * as the application classes, which allows for clean caching along with the application's
	 * lifecycle in any case. For a web application, consider declaring a local
	 * {@link org.springframework.web.util.IntrospectorCleanupListener} in {@code web.xml}
	 * in case of a multi-ClassLoader layout, which will allow for effective caching as well.
	 * @see Introspector#getBeanInfo(Class, int)
	 */
	public static final String IGNORE_BEANINFO_PROPERTY_NAME = "spring.beaninfo.ignore";

	private static final boolean shouldIntrospectorIgnoreBeaninfoClasses =
			SpringProperties.getFlag(IGNORE_BEANINFO_PROPERTY_NAME);


	@Override
	@NonNull
	public BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
		BeanInfo beanInfo = (shouldIntrospectorIgnoreBeaninfoClasses ?
				Introspector.getBeanInfo(beanClass, Introspector.IGNORE_ALL_BEANINFO) :
				Introspector.getBeanInfo(beanClass));

		// Immediately remove class from Introspector cache to allow for proper garbage
		// collection on class loader shutdown; we cache it in CachedIntrospectionResults
		// in a GC-friendly manner. This is necessary (again) for the JDK ClassInfo cache.
		Class<?> classToFlush = beanClass;
		do {
			Introspector.flushFromCaches(classToFlush);
			classToFlush = classToFlush.getSuperclass();
		}
		while (classToFlush != null && classToFlush != Object.class);

		return beanInfo;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
