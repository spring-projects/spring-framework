/*
 * Copyright 2002-2011 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ObjectUtils;

/**
 * Utility methods for obtaining a PlatformTransactionManager by
 * {@link TransactionAttribute#getQualifier() qualifier value}.
 *
 * @author Juergen Hoeller
 * @since 3.0.2
 */
public abstract class TransactionAspectUtils {

	/**
	 * Obtain a PlatformTransactionManager from the given BeanFactory,
	 * matching the given qualifier.
	 * @param beanFactory the BeanFactory to get the PlatformTransactionManager bean from
	 * @param qualifier the qualifier for selecting between multiple PlatformTransactionManager matches
	 * @return the chosen PlatformTransactionManager (never <code>null</code>)
	 * @throws IllegalStateException if no matching PlatformTransactionManager bean found
	 */
	public static PlatformTransactionManager getTransactionManager(BeanFactory beanFactory, String qualifier) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			// Full qualifier matching supported.
			return getTransactionManager((ConfigurableListableBeanFactory) beanFactory, qualifier);
		}
		else if (beanFactory.containsBean(qualifier)) {
			// Fallback: PlatformTransactionManager at least found by bean name.
			return beanFactory.getBean(qualifier, PlatformTransactionManager.class);
		}
		else {
			throw new IllegalStateException("No matching PlatformTransactionManager bean found for bean name '" +
					qualifier + "'! (Note: Qualifier matching not supported because given BeanFactory does not " +
					"implement ConfigurableListableBeanFactory.)");
		}
	}

	/**
	 * Obtain a PlatformTransactionManager from the given BeanFactory,
	 * matching the given qualifier.
	 * @param bf the BeanFactory to get the PlatformTransactionManager bean from
	 * @param qualifier the qualifier for selecting between multiple PlatformTransactionManager matches
	 * @return the chosen PlatformTransactionManager (never <code>null</code>)
	 * @throws IllegalStateException if no matching PlatformTransactionManager bean found
	 */
	public static PlatformTransactionManager getTransactionManager(ConfigurableListableBeanFactory bf, String qualifier) {
		Map<String, PlatformTransactionManager> tms =
				BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, PlatformTransactionManager.class);
		PlatformTransactionManager chosen = null;
		for (String beanName : tms.keySet()) {
			if (isQualifierMatch(qualifier, beanName, bf)) {
				if (chosen != null) {
					throw new IllegalStateException("No unique PlatformTransactionManager bean found " +
							"for qualifier '" + qualifier + "'");
				}
				chosen = tms.get(beanName);
			}
		}
		if (chosen != null) {
			return chosen;
		}
		else {
			throw new IllegalStateException("No matching PlatformTransactionManager bean found for qualifier '" +
					qualifier + "' - neither qualifier match nor bean name match!");
		}
	}

	/**
	 * Check whether we have a qualifier match for the given candidate bean.
	 * @param qualifier the qualifier that we are looking for
	 * @param beanName the name of the candidate bean
	 * @param bf the BeanFactory to get the bean definition from
	 * @return <code>true</code> if either the bean definition (in the XML case)
	 * or the bean's factory method (in the @Bean case) defines a matching qualifier
	 * value (through &lt;qualifier<&gt; or @Qualifier)
	 */
	private static boolean isQualifierMatch(String qualifier, String beanName, ConfigurableListableBeanFactory bf) {
		if (bf.containsBean(beanName)) {
			try {
				BeanDefinition bd = bf.getMergedBeanDefinition(beanName);
				if (bd instanceof AbstractBeanDefinition) {
					AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
					AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
					if ((candidate != null && qualifier.equals(candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY))) ||
							qualifier.equals(beanName) || ObjectUtils.containsElement(bf.getAliases(beanName), qualifier)) {
						return true;
					}
				}
				if (bd instanceof RootBeanDefinition) {
					Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
					if (factoryMethod != null) {
						Qualifier targetAnnotation = factoryMethod.getAnnotation(Qualifier.class);
						if (targetAnnotation != null && qualifier.equals(targetAnnotation.value())) {
							return true;
						}
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// ignore - can't compare qualifiers for a manually registered singleton object
			}
		}
		return false;
	}

}
