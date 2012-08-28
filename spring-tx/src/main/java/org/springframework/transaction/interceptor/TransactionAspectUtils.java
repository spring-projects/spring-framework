/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.transaction.interceptor;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Utility methods for obtaining a PlatformTransactionManager by
 * {@link TransactionAttribute#getQualifier() qualifier value}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0.2
 * @deprecated as of Spring 3.1.2 in favor of {@link BeanFactoryUtils}
 */
@Deprecated
public abstract class TransactionAspectUtils {

	/**
	 * Obtain a PlatformTransactionManager from the given BeanFactory, matching the given qualifier.
	 * @param beanFactory the BeanFactory to get the {@code PlatformTransactionManager} bean from
	 * @param qualifier the qualifier for selecting between multiple {@code PlatformTransactionManager} matches
	 * @return the chosen {@code PlatformTransactionManager} (never {@code null})
	 * @throws IllegalStateException if no matching {@code PlatformTransactionManager} bean found
	 * @deprecated as of Spring 3.1.2 in favor of
	 * {@link BeanFactoryAnnotationUtils#qualifiedBeanOfType(BeanFactory, Class, String)}
	 */
	public static PlatformTransactionManager getTransactionManager(BeanFactory beanFactory, String qualifier) {
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, PlatformTransactionManager.class, qualifier);
	}

	/**
	 * Obtain a PlatformTransactionManager from the given BeanFactory, matching the given qualifier.
	 * @param bf the BeanFactory to get the {@code PlatformTransactionManager} bean from
	 * @param qualifier the qualifier for selecting between multiple {@code PlatformTransactionManager} matches
	 * @return the chosen {@code PlatformTransactionManager} (never {@code null})
	 * @throws IllegalStateException if no matching {@code PlatformTransactionManager} bean found
	 * @deprecated as of Spring 3.1.2 in favor of
	 * {@link BeanFactoryAnnotationUtils#qualifiedBeanOfType(BeanFactory, Class, String)}
	 */
	public static PlatformTransactionManager getTransactionManager(ConfigurableListableBeanFactory bf, String qualifier) {
		return BeanFactoryAnnotationUtils.qualifiedBeanOfType(bf, PlatformTransactionManager.class, qualifier);
	}

}
