/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Convenience methods performing bean lookups related to annotations, for example
 * Spring's {@link Qualifier @Qualifier} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1.2
 * @see BeanFactoryUtils
 */
public abstract class BeanFactoryAnnotationUtils {

	/**
	 * Obtain a bean of type {@code T} from the given {@code BeanFactory} declaring a
	 * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
	 * qualifier, or having a bean name matching the given qualifier.
	 * @param beanFactory the BeanFactory to get the target bean from
	 * @param beanType the type of bean to retrieve
	 * @param qualifier the qualifier for selecting between multiple bean matches
	 * @return the matching bean of type {@code T} (never {@code null})
	 * @throws NoUniqueBeanDefinitionException if multiple matching beans of type {@code T} found
	 * @throws NoSuchBeanDefinitionException if no matching bean of type {@code T} found
	 * @throws BeansException if the bean could not be created
	 * @see BeanFactory#getBean(Class)
	 */
	public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier)
			throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			// Full qualifier matching supported.
			return qualifiedBeanOfType((ConfigurableListableBeanFactory) beanFactory, beanType, qualifier);
		}
		else if (beanFactory.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name.
			return beanFactory.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for bean name '" + qualifier +
					"'! (Note: Qualifier matching not supported because given " +
					"BeanFactory does not implement ConfigurableListableBeanFactory.)");
		}
	}

	/**
	 * Obtain a bean of type {@code T} from the given {@code BeanFactory} declaring a qualifier
	 * (e.g. {@code <qualifier>} or {@code @Qualifier}) matching the given qualifier).
	 * @param bf the BeanFactory to get the target bean from
	 * @param beanType the type of bean to retrieve
	 * @param qualifier the qualifier for selecting between multiple bean matches
	 * @return the matching bean of type {@code T} (never {@code null})
	 */
	private static <T> T qualifiedBeanOfType(ConfigurableListableBeanFactory bf, Class<T> beanType, String qualifier) {
		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(bf, beanType);
		String matchingBean = null;
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, bf)) {
				if (matchingBean != null) {
					throw new NoUniqueBeanDefinitionException(beanType, matchingBean, beanName);
				}
				matchingBean = beanName;
			}
		}
		if (matchingBean != null) {
			return bf.getBean(matchingBean, beanType);
		}
		else if (bf.containsBean(qualifier)) {
			// Fallback: target bean at least found by bean name - probably a manually registered singleton.
			return bf.getBean(qualifier, beanType);
		}
		else {
			throw new NoSuchBeanDefinitionException(qualifier, "No matching " + beanType.getSimpleName() +
					" bean found for qualifier '" + qualifier + "' - neither qualifier match nor bean name match!");
		}
	}

	/**
	 * Check whether the named bean declares a qualifier of the given name.
	 * @param qualifier the qualifier to match
	 * @param beanName the name of the candidate bean
	 * @param beanFactory the {@code BeanFactory} from which to retrieve the named bean
	 * @return {@code true} if either the bean definition (in the XML case)
	 * or the bean's factory method (in the {@code @Bean} case) defines a matching
	 * qualifier value (through {@code <qualifier>} or {@code @Qualifier})
	 * @since 5.0
	 */
	public static boolean isQualifierMatch(Predicate<String> qualifier, String beanName, BeanFactory beanFactory) {
		// Try quick bean name or alias match first...
		if (qualifier.test(beanName)) {
			return true;
		}
		if (beanFactory != null) {
			for (String alias : beanFactory.getAliases(beanName)) {
				if (qualifier.test(alias)) {
					return true;
				}
			}
			try {
				if (beanFactory instanceof ConfigurableBeanFactory) {
					BeanDefinition bd = ((ConfigurableBeanFactory) beanFactory).getMergedBeanDefinition(beanName);
					// Explicit qualifier metadata on bean definition? (typically in XML definition)
					if (bd instanceof AbstractBeanDefinition) {
						AbstractBeanDefinition abd = (AbstractBeanDefinition) bd;
						AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
						if (candidate != null) {
							Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
							if (value != null && qualifier.test(value.toString())) {
								return true;
							}
						}
					}
					// Corresponding qualifier on factory method? (typically in configuration class)
					if (bd instanceof RootBeanDefinition) {
						Method factoryMethod = ((RootBeanDefinition) bd).getResolvedFactoryMethod();
						if (factoryMethod != null) {
							Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
							if (targetAnnotation != null) {
								return qualifier.test(targetAnnotation.value());
							}
						}
					}
				}
				// Corresponding qualifier on bean implementation class? (for custom user types)
				Class<?> beanType = beanFactory.getType(beanName);
				if (beanType != null) {
					Qualifier targetAnnotation = AnnotationUtils.getAnnotation(beanType, Qualifier.class);
					if (targetAnnotation != null) {
						return qualifier.test(targetAnnotation.value());
					}
				}
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore - can't compare qualifiers for a manually registered singleton object
			}
		}
		return false;
	}

}
