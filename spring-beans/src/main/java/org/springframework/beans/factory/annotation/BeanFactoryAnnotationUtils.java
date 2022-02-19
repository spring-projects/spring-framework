/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Convenience methods performing bean lookups related to Spring-specific annotations,
 * for example Spring's {@link Qualifier @Qualifier} annotation.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.1.2
 * @see BeanFactoryUtils
 */
public abstract class BeanFactoryAnnotationUtils {

	/**
	 * Retrieve all beans of type {@code T} from the given {@code BeanFactory} declaring a
	 * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
	 * qualifier, or having a bean name matching the given qualifier.
	 * @param beanFactory the factory to get the target beans from (also searching ancestors)
	 * @param beanType the type of beans to retrieve
	 * @param qualifier the qualifier for selecting among all type matches
	 * @return the matching beans of type {@code T}
	 * @throws BeansException if any of the matching beans could not be created
	 * @since 5.1.1
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> Map<String, T> qualifiedBeansOfType(
			ListableBeanFactory beanFactory, Class<T> beanType, String qualifier) throws BeansException {

		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, beanType);
		Map<String, T> result = new LinkedHashMap<>(4);
		for (String beanName : candidateBeans) {
			if (isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
				result.put(beanName, beanFactory.getBean(beanName, beanType));
			}
		}
		return result;
	}

	/**
	 * Obtain a bean of type {@code T} from the given {@code BeanFactory} declaring a
	 * qualifier (e.g. via {@code <qualifier>} or {@code @Qualifier}) matching the given
	 * qualifier, or having a bean name matching the given qualifier.
	 * @param beanFactory the factory to get the target bean from (also searching ancestors)
	 * @param beanType the type of bean to retrieve
	 * @param qualifier the qualifier for selecting between multiple bean matches
	 * @return the matching bean of type {@code T} (never {@code null})
	 * @throws NoUniqueBeanDefinitionException if multiple matching beans of type {@code T} found
	 * @throws NoSuchBeanDefinitionException if no matching bean of type {@code T} found
	 * @throws BeansException if the bean could not be created
	 * @see BeanFactoryUtils#beanOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 */
	public static <T> T qualifiedBeanOfType(BeanFactory beanFactory, Class<T> beanType, String qualifier)
			throws BeansException {

		Assert.notNull(beanFactory, "BeanFactory must not be null");

		if (beanFactory instanceof ListableBeanFactory lbf) {
			// Full qualifier matching supported.
			return qualifiedBeanOfType(lbf, beanType, qualifier);
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
	 * @param bf the factory to get the target bean from
	 * @param beanType the type of bean to retrieve
	 * @param qualifier the qualifier for selecting between multiple bean matches
	 * @return the matching bean of type {@code T} (never {@code null})
	 */
	private static <T> T qualifiedBeanOfType(ListableBeanFactory bf, Class<T> beanType, String qualifier) {
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
	 * @param beanFactory the factory from which to retrieve the named bean
	 * @return {@code true} if either the bean definition (in the XML case)
	 * or the bean's factory method (in the {@code @Bean} case) defines a matching
	 * qualifier value (through {@code <qualifier>} or {@code @Qualifier})
	 * @since 5.0
	 */
	public static boolean isQualifierMatch(
			Predicate<String> qualifier, String beanName, @Nullable BeanFactory beanFactory) {

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
				Class<?> beanType = beanFactory.getType(beanName);
				if (beanFactory instanceof ConfigurableBeanFactory cbf) {
					BeanDefinition bd = cbf.getMergedBeanDefinition(beanName);
					// Explicit qualifier metadata on bean definition? (typically in XML definition)
					if (bd instanceof AbstractBeanDefinition abd) {
						AutowireCandidateQualifier candidate = abd.getQualifier(Qualifier.class.getName());
						if (candidate != null) {
							Object value = candidate.getAttribute(AutowireCandidateQualifier.VALUE_KEY);
							if (value != null && qualifier.test(value.toString())) {
								return true;
							}
						}
					}
					// Corresponding qualifier on factory method? (typically in configuration class)
					if (bd instanceof RootBeanDefinition rbd) {
						Method factoryMethod = rbd.getResolvedFactoryMethod();
						if (factoryMethod != null) {
							Qualifier targetAnnotation = AnnotationUtils.getAnnotation(factoryMethod, Qualifier.class);
							if (targetAnnotation != null) {
								return qualifier.test(targetAnnotation.value());
							}
						}
					}
				}
				// Corresponding qualifier on bean implementation class? (for custom user types)
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
